"""知牛 网关 - Function Calling 7 工具（A3）

工具 schema 定义 + 本地执行器。7 个工具：
get_realtime_quote / get_kline / get_financials / search_news / screen_stocks / compare_stocks / create_alert

执行器纯本地（复用行情/news/mock），不依赖上游厂商，保证无 Key / 离线也可演示工具编排。
"""
from __future__ import annotations

import json
from typing import Any, Dict, List

TOOLS_SCHEMA: List[dict] = [
    {
        "type": "function",
        "function": {
            "name": "get_realtime_quote",
            "description": "获取 A 股/指数实时行情（价格/涨跌幅/五档）。",
            "parameters": {
                "type": "object",
                "properties": {
                    "symbol": {"type": "string", "description": "如 sh600519 / sz000001 / sh000001"}
                },
                "required": ["symbol"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_kline",
            "description": "获取个股日 K 线（近 N 日 OHLCV）。",
            "parameters": {
                "type": "object",
                "properties": {
                    "symbol": {"type": "string"},
                    "datalen": {"type": "integer", "description": "返回条数，默认 20"},
                },
                "required": ["symbol"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_financials",
            "description": "获取个股财务摘要（市盈率/市净率/市值/营收/净利）。",
            "parameters": {
                "type": "object",
                "properties": {"symbol": {"type": "string"}},
                "required": ["symbol"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_news",
            "description": "搜索个股相关新闻/快讯。",
            "parameters": {
                "type": "object",
                "properties": {"keyword": {"type": "string", "description": "股票名/关键词"}},
                "required": ["keyword"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "screen_stocks",
            "description": "按行业/市值/涨跌幅/条件选股。",
            "parameters": {
                "type": "object",
                "properties": {
                    "industry": {"type": "string"},
                    "min_pct": {"type": "number", "description": "最小涨跌幅(%), 如 2"},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "compare_stocks",
            "description": "对比多只股票的行情/涨跌。",
            "parameters": {
                "type": "object",
                "properties": {
                    "symbols": {"type": "array", "items": {"type": "string"}}
                },
                "required": ["symbols"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "create_alert",
            "description": "创建价格预警（条件+阈值）。",
            "parameters": {
                "type": "object",
                "properties": {
                    "symbol": {"type": "string"},
                    "condition": {"type": "string", "enum": ["above", "below"]},
                    "price": {"type": "number"},
                },
                "required": ["symbol", "condition", "price"],
            },
        },
    },
]

# 演示兜底行情数据（与前端 Mock 对齐，无网络可演示）
_MOCK_QUOTES: Dict[str, dict] = {
    "sh600519": {"name": "贵州茅台", "price": 1292.83, "pct": 0.10, "open": 1272.0, "high": 1295.0, "low": 1270.01, "prev": 1291.5},
    "sz000001": {"name": "平安银行", "price": 11.30, "pct": 1.35, "open": 11.2, "high": 11.38, "low": 11.12, "prev": 11.15},
    "sh600036": {"name": "招商银行", "price": 36.28, "pct": 1.34, "open": 36.1, "high": 36.45, "low": 35.9, "prev": 35.8},
    "sz300750": {"name": "宁德时代", "price": 196.80, "pct": 1.71, "open": 195.0, "high": 198.0, "low": 193.1, "prev": 193.5},
    "sh601318": {"name": "中国平安", "price": 48.35, "pct": 0.94, "open": 48.2, "high": 48.66, "low": 47.8, "prev": 47.9},
    "sh000001": {"name": "上证指数", "price": 3245.13, "pct": 0.39, "open": 3232.0, "high": 3250.0, "low": 3228.0, "prev": 3232.68},
}


def _quote(symbol: str) -> dict:
    q = _MOCK_QUOTES.get(symbol)
    if not q:
        return {"error": f"未找到 {symbol} 的行情"}
    return {"symbol": symbol, **q}


def _kline(symbol: str) -> dict:
    base = 1200.0
    bars = []
    for i in range(20):
        close = base + i * 3.5 + (i % 5) * 2
        bars.append(
            {"day": f"2026-07-{(i % 28) + 1:02d}", "open": close - 6, "high": close + 8,
             "low": close - 10, "close": close, "volume": 26000 + i * 700}
        )
    return {"symbol": symbol, "data": bars}


def _financials(symbol: str) -> dict:
    q = _MOCK_QUOTES.get(symbol, {})
    return {
        "symbol": symbol,
        "name": q.get("name", symbol),
        "pe": 28.4, "pb": 6.8, "market_cap_wan": 162000,
        "revenue_yoy": 0.082, "net_profit_yoy": 0.151,
    }


def _news(keyword: str) -> dict:
    return {
        "keyword": keyword,
        "items": [
            {"title": f"{keyword} 放量突破 20 日线，主力资金净流入", "date": "2026-08-24"},
            {"title": f"{keyword} 公告：拟回购股份用于股权激励", "date": "2026-08-23"},
            {"title": f"机构：{keyword} 中报业绩符合预期，维持增持", "date": "2026-08-22"},
        ],
    }


def _screen(industry: str, min_pct: float) -> dict:
    rows = []
    for sym, q in _MOCK_QUOTES.items():
        if sym.startswith("sh0000") or not industry or industry in q.get("name", ""):
            if q.get("pct", 0) >= (min_pct or 0):
                rows.append({"symbol": sym, "name": q.get("name"), "pct": q.get("pct")})
    return {"industry": industry, "min_pct": min_pct, "rows": rows}


def _compare(symbols: List[str]) -> dict:
    return {"rows": [{"symbol": s, **_quote(s)} for s in symbols]}


def _alert(symbol: str, condition: str, price: float) -> dict:
    return {"symbol": symbol, "condition": condition, "price": price, "status": "created"}


_EXECUTORS: Dict[str, Any] = {
    "get_realtime_quote": lambda a: _quote(a.get("symbol", "")),
    "get_kline": lambda a: _kline(a.get("symbol", "")),
    "get_financials": lambda a: _financials(a.get("symbol", "")),
    "search_news": lambda a: _news(a.get("keyword", "")),
    "screen_stocks": lambda a: _screen(a.get("industry", ""), a.get("min_pct", 0)),
    "compare_stocks": lambda a: _compare(a.get("symbols", [])),
    "create_alert": lambda a: _alert(a.get("symbol", ""), a.get("condition", ""), a.get("price", 0)),
}


def has_tools() -> bool:
    return len(TOOLS_SCHEMA) > 0


def get_tools_schema() -> List[dict]:
    return TOOLS_SCHEMA


def execute_tool(name: str, args: dict) -> dict:
    """执行单个工具，返回结构化 result（可回灌 role:tool）。"""
    handler = _EXECUTORS.get(name)
    if handler is None:
        return {"error": f"未知工具: {name}"}
    try:
        if not isinstance(args, dict):
            args = json.loads(args) if isinstance(args, str) else {}
        return handler(args)
    except Exception as e:  # noqa: BLE001
        return {"error": f"工具 {name} 执行失败: {e}"}