"""知牛 行情代理层（多端统一入口，规避新浪 Referer/GBK/CORS）

真实端点（2026-08-22 核实，见 docs/api-matrix.md §1）：
- 实时：https://hq.sinajs.cn/list=<code1>,<code2>，必须 Referer: https://finance.sina.com.cn，GBK，字段索引 0~31
- K 线：https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=&scale=&ma=no&datalen=

降级链：新浪 → （失败/过期）stale 缓存 → （再失败）前端 Mock JSON 兜底（与 data/mock 语义一致）。
返回 header 带 X-Gateway-Stale: true 表示命中了 stale/兜底数据。
"""
from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Dict, Optional
from urllib import request

REFERER = {"Referer": "https://finance.sina.com.cn"}  # 新浪必需的 Referer
_REQ_HEADERS = {**REFERER, "User-Agent": "Mozilla/5.0 zhiniu-gateway"}
_TIMEOUT = 4.0  # 新浪 4s 超时即视为失败，避免卡住演示

_REALTIME_TTL = 3.0  # 实时缓存 3s

# 目录（相对本文件定位到 shared/data/mock，避免硬编码绝对路径依赖 jpy/外挂）
_MOCK_DIR = (
    Path(__file__).resolve().parent.parent.parent
    / "shared/src/commonMain/kotlin/com/zhiniu/data/mock"
)

_rt_cache: Dict[str, dict] = {}          # codes key → (ts, data)
_rt_order: list = []                     # 简单 LRU 顺序，防无限增长
_kline_cache: Dict[str, dict] = {}       # symbol key → (ts, data)
_rt_last_ok: Dict[str, dict] = {}        # 最近一次成功抓取（stale 兜底）


def _load_quotes() -> Dict[str, dict]:
    """读取前端 Mock 实时数据，返回 code→quote 映射（离线兜底）。"""
    try:
        path = _MOCK_DIR / "mock_quotes.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        return {q["symbol"]: q for q in data.get("quotes", [])}
    except Exception:
        return {}


_MOCK_QUOTES = _load_quotes()
_MOCK_KLINE = None


def _load_kline():
    global _MOCK_KLINE
    if _MOCK_KLINE is None:
        try:
            path = _MOCK_DIR / "mock_kline_sh600519.json"
            _MOCK_KLINE = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            _MOCK_KLINE = {}
    return _MOCK_KLINE


def _http_get(url: str, decode: Optional[str] = None) -> str:
    req = request.Request(url, headers=_REQ_HEADERS)
    with request.urlopen(req, timeout=_TIMEOUT) as resp:
        raw = resp.read()
        return raw.decode(decode or "utf-8", errors="replace")


def _num(v) -> float:
    try:
        return float(v)
    except Exception:
        return 0.0


def _parse_sina(code: str, raw: str) -> Optional[dict]:
    """解析新浪 hq_str_<code> 的 32 字段为结构化 quote。"""
    key = f'hq_str_{code}="'
    if key not in raw:
        return None
    body = raw.split(key, 1)[1].split('"', 1)[0]
    f = body.split(",")
    if len(f) < 32:
        return None
    # 10-19 买一~买五（量,价 交替）、20-29 卖一~卖五（量,价 交替）→ 统一为 [价,量]
    bids = [[_num(f[11 + i * 2]), _num(f[10 + i * 2])] for i in range(5)]
    asks = [[_num(f[21 + i * 2]), _num(f[20 + i * 2])] for i in range(5)]

    volume_share = _num(f[8])  # 股
    amount_yuan = _num(f[9])   # 元
    return {
        "symbol": code,
        "name": f[0],
        "open": _num(f[1]),
        "prevClose": _num(f[2]),
        "price": _num(f[3]),
        "high": _num(f[4]),
        "low": _num(f[5]),
        "buy1": _num(f[6]),
        "sell1": _num(f[7]),
        "volume": volume_share / 100.0,     # 股 → 手
        "amount": amount_yuan / 10000.0,    # 元 → 万元
        "bids": bids,
        "asks": asks,
        "date": f[30],
        "time": f[31],
    }


def _mock_realtime(codes: list) -> dict:
    """离线 Mock 兜底：返回前端 mock_quotes.json 对齐结构。"""
    out: dict = {}
    for c in codes:
        q = _MOCK_QUOTES.get(c)
        if q:
            out[c] = q
    return out


async def quote_realtime(codes: list) -> dict:
    """GET /quote/realtime?codes=...  返回 code→quote（UTF-8 JSON）。"""
    codes = codes or []
    if not codes:
        return {}
    cache_hit = True
    now = time.time()
    latest = {c: v for c, v in _rt_cache.items() if now - v[0] <= _REALTIME_TTL}
    result: Dict[str, dict] = {c: v[1] for c, v in latest.items() if c in codes}

    need = [c for c in codes if c not in result]
    if need:
        cache_hit = False
        try:
            url = "https://hq.sinajs.cn/list=" + ",".join(need)
            raw = _http_get(url, decode="gbk")
            fresh: Dict[str, dict] = {}
            for c in need:
                parsed = _parse_sina(c, raw)
                if parsed:
                    fresh[c] = parsed
            if fresh:
                _rt_last_ok.update(fresh)
            # 更新缓存
            for c, v in fresh.items():
                _rt_cache[c] = (now, v)
                if c not in _rt_order:
                    _rt_order.append(c)
            while len(_rt_order) > 100:
                old = _rt_order.pop(0)
                _rt_cache.pop(old, None)
            result.update(fresh)
        except Exception:
            pass  # 网络/超时 → 走 stale 或 mock

    # stale 兜底：有最近成功数据优先
    was_stale = False
    for c in need:
        if c not in result and c in _rt_last_ok:
            result[c] = _rt_last_ok[c]
            was_stale = True
    # Mock 兜底
    mock = _mock_realtime(need)
    for c, v in mock.items():
        result.setdefault(c, v)

    return result, was_stale


def _kline_ttl(scale: int) -> float:
    """K 线 TTL：日线(scale>=240)隔夜外，分钟线 short TTL。"""
    if scale >= 240:
        return 6 * 3600.0   # 6h
    return scale * 1.0      # 分钟线：scale 秒量级


async def quote_kline(symbol: str, scale: int = 240, datalen: int = 120) -> dict:
    """GET /quote/kline?symbol=&scale=&datalen=  返回 {symbol,name,data:[{day...}]}"""
    now = time.time()
    cached = _kline_cache.get(symbol)
    if cached and now - cached[0] <= _kline_ttl(scale):
        return cached[1]

    result: Optional[dict] = None
    is_daily = scale >= 240
    if is_daily and _kline_cache.get(symbol):
        # 日线数据不过期兜底（stale），header 由外层标 stale
        result = _kline_cache[symbol][1]
    try:
        url = (
            "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData"
            f"?symbol={symbol}&scale={scale}&ma=no&datalen={min(datalen, 1023)}"
        )
        raw = _http_get(url)
        data = json.loads(raw)
        if isinstance(data, list) and data:
            result = {"symbol": symbol, "name": data[-1].get("day", ""), "data": data, "scale": scale}
    except Exception:
        pass

    if result is None:
        mock = _load_kline()
        if mock and (mock.get("symbol") == symbol or symbol == "sh600519"):
            result = {
                "symbol": symbol,
                "name": mock.get("name", ""),
                "data": mock.get("data", []),
                "scale": scale,
            }

    if result is not None:
        _kline_cache[symbol] = (now, result)
    return result or {"symbol": symbol, "name": "", "data": [], "scale": scale}