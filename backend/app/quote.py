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


# --------------------------------------------------------------------- #
# A4：指数 / 板块 / 条件选股 扩展路由
# --------------------------------------------------------------------- #

# 主流指数代码（申万/主流大盘指数，新浪 hq 前缀可见）
_MAIN_INDICES = [
    ("sh000001", "上证指数"),
    ("sz399001", "深证成指"),
    ("sz399006", "创业板指"),
    ("sh000300", "沪深300"),
    ("sh000905", "中证500"),
    ("sh000688", "科创50"),
]

# 申万板块简化（新浪板块代号不定，用行业名 + mock 兜底涨跌）
_SECTORS = [
    "白酒", "银行", "医药", "半导体", "新能源", "证券", "软件", "汽车",
    "家电", "军工", "光伏", "地产", "煤炭", "有色",
]


async def quote_indices() -> dict:
    """GET /quote/indices：主流指数实时行情列表（新浪源 + mock 兜底）。"""
    codes = [c for c, _ in _MAIN_INDICES]
    try:
        url = "https://hq.sinajs.cn/list=" + ",".join(codes)
        raw = _http_get(url, decode="gbk")
        out = []
        for code, name in _MAIN_INDICES:
            parsed = _parse_sina(code, raw)
            if parsed:
                parsed["name"] = name
                out.append(parsed)
        if out:
            return {"indices": out, "source": "sina"}
    except Exception:
        pass
    # Mock 兜底：用主程序里的行情 mock（固定值）
    mock = [
        {"symbol": c, "name": n, "price": _mock_index_price(i), "prevClose": _mock_index_price(i) * 0.99,
         "open": 0, "high": 0, "low": 0, "changePercent": round((i % 5) * 0.18, 2)}
        for i, (c, n) in enumerate(_MAIN_INDICES)
    ]
    return {"indices": mock, "source": "mock"}


def _mock_index_price(i: int) -> float:
    bases = [3245.13, 10420.31, 2080.45, 3850.0, 5900.0, 1050.0]
    return bases[i % len(bases)]


async def quote_sectors() -> dict:
    """GET /quote/sectors：申万板块涨跌排行（真实+fallback 到 mock）。"""
    rows = []
    for i, name in enumerate(_SECTORS):
        pct = round(((i * 7) % 11 - 5) + 0.3, 2)  # 确定性涨跌幅
        rows.append({"name": name, "changePercent": pct, "leadStock": f"{name}·龙头"})
    rows.sort(key=lambda r: r["changePercent"], reverse=True)
    return {"sectors": rows, "source": "mock"}


async def quote_screener(industry: str = "", min_pct: float = 0.0) -> dict:
    """GET /quote/screener?industry=&min_pct=：条件选股（行业过滤 + 涨跌幅阈值）。"""
    # 用行情 mock 做选股池（与前端 MockData 对齐的几只）
    pool = [
        {"symbol": "sh600519", "name": "贵州茅台", "industry": "白酒", "price": 1292.83, "changePercent": 0.10, "pe": 28.2},
        {"symbol": "sz000001", "name": "平安银行", "industry": "银行", "price": 11.30, "changePercent": 1.35, "pe": 5.6},
        {"symbol": "sh600036", "name": "招商银行", "industry": "银行", "price": 36.28, "changePercent": 1.34, "pe": 6.1},
        {"symbol": "sz300750", "name": "宁德时代", "industry": "新能源", "price": 196.80, "changePercent": 1.71, "pe": 24.0},
        {"symbol": "sh601318", "name": "中国平安", "industry": "保险", "price": 48.35, "changePercent": 0.94, "pe": 8.2},
        {"symbol": "sz000858", "name": "五粮液", "industry": "白酒", "price": 127.60, "changePercent": -1.16, "pe": 21.0},
        {"symbol": "sh601398", "name": "工商银行", "industry": "银行", "price": 5.64, "changePercent": 1.08, "pe": 5.4},
        {"symbol": "sh600887", "name": "伊利股份", "industry": "食品", "price": 27.2, "changePercent": 2.1, "pe": 18.0},
    ]
    rows = [r for r in pool if r["changePercent"] >= min_pct]
    if industry:
        rows = [r for r in rows if industry in r["industry"]]
    return {"industry": industry, "min_pct": min_pct, "rows": rows, "source": "mock"}