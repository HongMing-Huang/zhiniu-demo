"""知牛 网关 - 用量统计（A6）

进程内聚合每次 chat_completions 的 模型/请求次数/耗时/降级/错误分布，
提供 GET /analytics/usage（按模型 + 时段）。in-memory，重启清零，够演示与设置页数据源。
"""
from __future__ import annotations

import threading
import time
from collections import defaultdict
from typing import Dict, List

# record: {ts, model, latency_ms, degraded:bool, error:Optional[str]}
_records: List[dict] = []
_lock = threading.Lock()

# 分桶窗口：最近 1h 内按分钟
_MAX_RECORDS = 10000


def record_usage(model: str, latency_ms: float, degraded: bool = False, error: Optional[str] = None) -> None:
    with _lock:
        _records.append(
            {
                "ts": time.time(),
                "model": model,
                "latency_ms": latency_ms,
                "degraded": degraded,
                "error": error,
            }
        )
        if len(_records) > _MAX_RECORDS:
            del _records[: len(_records) - _MAX_RECORDS]


def _pct(values: List[float], q: float) -> float:
    if not values:
        return 0.0
    s = sorted(values)
    idx = min(len(s) - 1, int(len(s) * q))
    return round(s[idx], 1)


def usage_summary(hours: int = 1) -> dict:
    """按模型聚合：request_count / tokens(占位) / P50·P95 延迟 / 降级率 / 错误分布。"""
    cutoff = time.time() - hours * 3600
    with _lock:
        recent = [r for r in _records if r["ts"] >= cutoff]

    by_model: Dict[str, List[dict]] = defaultdict(list)
    for r in recent:
        by_model[r["model"]].append(r)

    models: List[dict] = []
    for model, items in by_model.items():
        lat = [i["latency_ms"] for i in items]
        errors = [i["error"] for i in items if i["error"]]
        degraded = [i for i in items if i["degraded"]]
        total = len(items)
        error_dist: Dict[str, int] = defaultdict(int)
        for e in errors:
            error_dist[e] += 1
        models.append(
            {
                "model": model,
                "request_count": total,
                "p50_latency_ms": _pct(lat, 0.50),
                "p95_latency_ms": _pct(lat, 0.95),
                "degraded_rate": round(len(degraded) / total, 3) if total else 0.0,
                "error_distribution": dict(error_dist),
            }
        )
    models.sort(key=lambda m: m["request_count"], reverse=True)
    return {
        "window_hours": hours,
        "total_requests": len(recent),
        "models": models,
    }