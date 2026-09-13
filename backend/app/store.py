"""知牛 · 研究缓存 / 并发去重 / 可选 Postgres（Neon）持久化。

三层策略：
1. 进程内缓存 + 进行中任务去重（默认启用，零依赖）。
2. 可选 Postgres 持久化：设置 DATABASE_URL（如 Neon 连接串）即启用，
   分析结果跨进程共享；未设置时全部优雅降级为纯内存。
3. 聊天幂等：request_id 去重（内存 LRU，数据库可用时落唯一约束表）。

实现：SQLAlchemy Core——查询语句为模块级常量，参数经 bindparam 名称绑定；
执行端只传参数字典，源码不含 SQL 文本、不含拼接。连接串来自 DATABASE_URL，
未显式指定 sslmode 时追加 require（Neon 等托管库 TLS）。
"""

from __future__ import annotations

import asyncio
import os
import time
from collections import OrderedDict
from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import (
    Column,
    DateTime,
    MetaData,
    String,
    Table,
    bindparam,
    insert,
    select,
    text,
    update,
)

DATABASE_URL = os.getenv("DATABASE_URL", "").strip()
RESEARCH_TTL_SECONDS = int(os.getenv("RESEARCH_TTL_SECONDS", "3600"))
_REQUEST_MEMORY_MAX = 512

metadata = MetaData()
_research_cache_table = Table(
    "research_cache",
    metadata,
    Column("symbol", String, primary_key=True),
    Column("keyword", String, primary_key=True, default=""),
    Column("payload_json", String, nullable=False),
    Column("created_at", DateTime(timezone=True), server_default=text("now()")),
)
_request_dedup_table = Table(
    "request_dedup",
    metadata,
    Column("request_id", String, primary_key=True),
    Column("payload_json", String),
    Column("created_at", DateTime(timezone=True), server_default=text("now()")),
)

# 模块级语句：参数全部经 bindparam 名称绑定。
_GET_RESEARCH = select(_research_cache_table.c.payload_json).where(
    _research_cache_table.c.symbol == bindparam("symbol"),
    _research_cache_table.c.keyword == bindparam("keyword"),
    _research_cache_table.c.created_at > bindparam("cutoff"),
)
_PUT_RESEARCH = insert(_research_cache_table)
_UPDATE_RESEARCH = (
    update(_research_cache_table)
    .where(_research_cache_table.c.symbol == bindparam("symbol"))
    .where(_research_cache_table.c.keyword == bindparam("keyword"))
    .values(payload_json=bindparam("payload_json"), created_at=bindparam("created_at"))
)
_GET_DEDUP = select(_request_dedup_table.c.payload_json).where(
    _request_dedup_table.c.request_id == bindparam("request_id")
)
_PUT_DEDUP = insert(_request_dedup_table)

_status: dict[str, Any] = {"enabled": bool(DATABASE_URL), "ready": False, "error": ""}
_engine: Any = None
_engine_lock = asyncio.Lock()

# 进程内研究缓存：key -> (ts, payload)
_research_cache: OrderedDict[tuple[str, str], tuple[float, dict]] = OrderedDict()
# 进行中研究：key -> Future（并发去重）
_inflight: dict[tuple[str, str], asyncio.Future] = {}
# 聊天幂等：request_id -> payload（内存 LRU）
_request_seen: OrderedDict[str, dict] = OrderedDict()


def _cache_key(symbol: str, keyword: str) -> tuple[str, str]:
    return (symbol.lower().strip(), (keyword or "").strip())


def _ts_to_dt(ts: float) -> datetime:
    return datetime.fromtimestamp(ts, tz=timezone.utc)


def status() -> dict[str, Any]:
    return dict(_status)


async def _get_engine() -> Any:
    global _engine
    if not _status["enabled"]:
        return None
    if _engine is not None:
        return _engine
    async with _engine_lock:
        if _engine is not None:
            return _engine
        try:
            from sqlalchemy.ext.asyncio import create_async_engine

            url = DATABASE_URL
            if url.startswith("postgres://"):
                url = url.replace("postgres://", "postgresql://", 1)
            if url.startswith("postgresql://"):
                url = url.replace("postgresql://", "postgresql+psycopg://", 1)
            if "sslmode=" not in url:
                url += ("&" if "?" in url else "?") + "sslmode=require"  # Neon 等托管库 TLS
            _engine = create_async_engine(url, pool_size=2, max_overflow=1, pool_timeout=10)
            async with _engine.begin() as conn:
                await conn.run_sync(metadata.create_all)
            _status["ready"] = True
            return _engine
        except Exception as exc:  # 数据库不可用 → 降级纯内存，不阻塞研究链路
            _status["error"] = str(exc)[:200]
            _engine = None
            return None


async def init() -> None:
    if _status["enabled"]:
        await _get_engine()


# ---------- 研究缓存 ----------

def memory_get_research(symbol: str, keyword: str) -> Optional[dict]:
    key = _cache_key(symbol, keyword)
    hit = _research_cache.get(key)
    if not hit:
        return None
    ts, payload = hit
    if time.time() - ts > RESEARCH_TTL_SECONDS:
        _research_cache.pop(key, None)
        return None
    _research_cache.move_to_end(key)
    return {**payload, "cached": True, "cache": "memory"}


async def get_research(symbol: str, keyword: str) -> Optional[dict]:
    import json

    key = _cache_key(symbol, keyword)
    hit = memory_get_research(symbol, keyword)
    if hit is not None:
        return hit
    engine = await _get_engine()
    if engine is None:
        return None
    try:
        async with engine.connect() as conn:
            row = (
                await conn.execute(
                    _GET_RESEARCH,
                    {
                        "symbol": key[0],
                        "keyword": key[1],
                        "cutoff": _ts_to_dt(time.time() - RESEARCH_TTL_SECONDS),
                    },
                )
            ).fetchone()
        if row and row[0]:
            payload = json.loads(row[0])
            _research_cache[key] = (time.time(), payload)
            return {**payload, "cached": True, "cache": "postgres"}
    except Exception as exc:
        _status["error"] = str(exc)[:200]
    return None


async def put_research(symbol: str, keyword: str, payload: dict) -> None:
    import json

    key = _cache_key(symbol, keyword)
    now_dt = _ts_to_dt(time.time())
    payload_text = json.dumps(payload, ensure_ascii=False)
    _research_cache[key] = (time.time(), payload)
    while len(_research_cache) > 64:
        _research_cache.popitem(last=False)
    engine = await _get_engine()
    if engine is None:
        return
    try:
        async with engine.begin() as conn:
            existing = (
                await conn.execute(
                    select(_research_cache_table.c.symbol).where(
                        _research_cache_table.c.symbol == bindparam("symbol")
                    ),
                    {"symbol": key[0]},
                )
            ).fetchone()
            if existing:
                await conn.execute(
                    _UPDATE_RESEARCH,
                    {"symbol": key[0], "keyword": key[1], "payload_json": payload_text, "created_at": now_dt},
                )
            else:
                await conn.execute(
                    _PUT_RESEARCH,
                    [
                        {
                            "symbol": key[0],
                            "keyword": key[1],
                            "payload_json": payload_text,
                            "created_at": now_dt,
                        }
                    ],
                )
    except Exception as exc:
        _status["error"] = str(exc)[:200]


# ---------- 进行中任务去重 ----------

def begin_inflight(symbol: str, keyword: str) -> Optional[asyncio.Future]:
    """已有同 key 任务进行中 → 返回其 Future；否则登记新 Future 并返回 None。"""
    key = _cache_key(symbol, keyword)
    existing = _inflight.get(key)
    if existing is not None:
        return existing
    _inflight[key] = asyncio.get_event_loop().create_future()
    return None


def finish_inflight(symbol: str, keyword: str, payload: dict) -> None:
    fut = _inflight.pop(_cache_key(symbol, keyword), None)
    if fut is not None and not fut.done():
        fut.set_result(payload)


def fail_inflight(symbol: str, keyword: str, exc: Exception) -> None:
    fut = _inflight.pop(_cache_key(symbol, keyword), None)
    if fut is not None and not fut.done():
        fut.set_exception(exc)


async def await_inflight(fut: asyncio.Future, timeout: float = 90.0) -> Optional[dict]:
    try:
        return await asyncio.wait_for(fut, timeout)
    except Exception:
        return None


# ---------- 聊天幂等（request_id） ----------

async def request_seen(request_id: str) -> Optional[dict]:
    import json

    if not request_id:
        return None
    hit = _request_seen.get(request_id)
    if hit is not None:
        return hit
    engine = await _get_engine()
    if engine is None:
        return None
    try:
        async with engine.connect() as conn:
            row = (
                await conn.execute(_GET_DEDUP, {"request_id": request_id})
            ).fetchone()
        return json.loads(row[0]) if row and row[0] else None
    except Exception as exc:
        _status["error"] = str(exc)[:200]
        return None


async def remember_request(request_id: str, payload: dict) -> None:
    import json

    if not request_id:
        return
    _request_seen[request_id] = payload
    while len(_request_seen) > _REQUEST_MEMORY_MAX:
        _request_seen.popitem(last=False)
    engine = await _get_engine()
    if engine is None:
        return
    try:
        async with engine.begin() as conn:
            await conn.execute(
                _PUT_DEDUP,
                [
                    {
                        "request_id": request_id,
                        "payload_json": json.dumps(payload, ensure_ascii=False),
                    }
                ],
            )
    except Exception as exc:
        _status["error"] = str(exc)[:200]
