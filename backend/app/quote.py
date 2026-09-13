"""知牛 行情代理层（多端统一入口，规避新浪 Referer/GBK/CORS）

真实端点（2026-08-22 核实，见 docs/api-matrix.md §1）：
- 实时：https://hq.sinajs.cn/list=<code1>,<code2>，必须 Referer: https://finance.sina.com.cn，GBK，字段索引 0~31
- K 线：https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=&scale=&ma=no&datalen=

降级链：新浪 → （失败/过期）stale 缓存 → （再失败）前端 Mock JSON 兜底（与 data/mock 语义一致）。
返回 header 带 X-Gateway-Stale: true 表示命中了 stale/兜底数据。
"""
from __future__ import annotations

import asyncio
import json
import re
import time
from pathlib import Path
from typing import Dict, Optional
from urllib import request
from urllib.parse import urlencode

REFERER = {"Referer": "https://finance.sina.com.cn"}  # 新浪必需的 Referer
_REQ_HEADERS = {**REFERER, "User-Agent": "Mozilla/5.0 zhiniu-gateway"}
_EASTMONEY_HEADERS = {
    "Referer": "https://quote.eastmoney.com/",
    "User-Agent": "Mozilla/5.0 zhiniu-gateway",
}
_TIMEOUT = 4.0  # 新浪 4s 超时即视为失败，避免卡住演示

_REALTIME_TTL = 3.0  # 实时缓存 3s
_ULIST_TTL = 30.0       # 东财批量市值/换手补充 30s
_SECTORS_TTL = 60.0     # 行业板块排行 60s
_SCREENER_TTL = 60.0    # 选股池快照 60s
_POPULARITY_TTL = 120.0  # 人气榜排名 120s
_POPULARITY_URL = "https://emappdata.eastmoney.com/stockrank/getAllCurrentList"

# 目录（相对本文件定位到 shared/data/mock，避免硬编码绝对路径依赖 jpy/外挂）
_MOCK_DIR = (
    Path(__file__).resolve().parent.parent.parent
    / "shared/src/commonMain/kotlin/com/zhiniu/data/mock"
)

_rt_cache: Dict[str, dict] = {}          # codes key → (ts, data)
_rt_order: list = []                     # 简单 LRU 顺序，防无限增长
_kline_cache: Dict[str, dict] = {}       # symbol key → (ts, data)
_rt_last_ok: Dict[str, dict] = {}        # 最近一次成功抓取（stale 兜底）
_fundamentals_cache: Dict[str, tuple[float, dict]] = {}
_fundamentals_last_ok: Dict[str, dict] = {}
_ulist_cache: Dict[str, tuple[float, dict]] = {}        # code → (ts, {marketCap, turnoverRate})
_sectors_cache: Optional[tuple[float, dict]] = None
_screener_cache: Optional[tuple[float, dict]] = None
_popularity_cache: Optional[tuple[float, dict]] = None


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


def _http_get(url: str, decode: Optional[str] = None, headers: Optional[dict] = None) -> str:
    req = request.Request(url, headers=headers or _REQ_HEADERS)
    with request.urlopen(req, timeout=_TIMEOUT) as resp:
        raw = resp.read()
        return raw.decode(decode or "utf-8", errors="replace")


def _num(v) -> float:
    try:
        return float(v)
    except Exception:
        return 0.0


def _optional_num(v) -> Optional[float]:
    if v in (None, "", "-"):
        return None
    try:
        return float(v)
    except Exception:
        return None


def _scaled_field(data: dict, key: str) -> Optional[float]:
    value = _optional_num(data.get(key))
    if value is None:
        return None
    digits = int(_num(data.get("f152")))
    return round(value / (10 ** max(0, digits)), max(0, digits))


def _parse_eastmoney_snapshot(symbol: str, data: dict) -> dict:
    """Parse valuation and classification fields from Eastmoney's quote snapshot."""
    concepts = [part.strip() for part in str(data.get("f129") or "").split(",") if part.strip()]
    return {
        "symbol": symbol,
        "name": str(data.get("f58") or ""),
        "industry": str(data.get("f127") or ""),
        "region": str(data.get("f128") or ""),
        "concepts": concepts,
        "pe": _scaled_field(data, "f162"),
        "pb": _scaled_field(data, "f167"),
        "turnoverRate": _scaled_field(data, "f168"),
        "amplitude": _scaled_field(data, "f171"),
        "marketCap": _optional_num(data.get("f116")),
        "floatMarketCap": _optional_num(data.get("f117")),
    }


def _parse_eastmoney_report(payload: dict) -> dict:
    rows = ((payload.get("result") or {}).get("data") or [])
    if not rows:
        return {}
    row = rows[0]
    return {
        "reportDate": str(row.get("REPORTDATE") or "")[:10],
        "reportType": str(row.get("DATATYPE") or ""),
        "revenue": _optional_num(row.get("TOTAL_OPERATE_INCOME")),
        "netProfit": _optional_num(row.get("PARENT_NETPROFIT")),
        "roe": _optional_num(row.get("WEIGHTAVG_ROE")),
        "grossMargin": _optional_num(row.get("XSMLL")),
        "revenueYoY": _optional_num(row.get("YSTZ")),
        "netProfitYoY": _optional_num(row.get("SJLTZ")),
    }


def _parse_eastmoney_flow(payload: dict) -> dict:
    rows = ((payload.get("data") or {}).get("klines") or [])
    if not rows:
        return {}
    values = str(rows[-1]).split(",")
    if len(values) < 6:
        return {}
    return {
        "asOf": values[0],
        "mainNetInflow": _num(values[1]),
        "smallNetInflow": _num(values[2]),
        "mediumNetInflow": _num(values[3]),
        "largeNetInflow": _num(values[4]),
        "superLargeNetInflow": _num(values[5]),
    }


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
        "amount": amount_yuan,              # 元（前端统一按元格式化为万/亿）
        "bids": bids,
        "asks": asks,
        "date": f[30],
        "time": f[31],
        "source": "新浪财经",
        "provider": "sina-realtime",
        "isStale": False,
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
            result[c] = {**_rt_last_ok[c], "isStale": True}
            was_stale = True
    # Mock 兜底
    mock = _mock_realtime(need)
    for c, v in mock.items():
        if c not in result:
            result[c] = {
                **v,
                "source": "知牛离线快照",
                "provider": "offline-snapshot",
                "isStale": True,
            }
            was_stale = True

    # 东财批量补充总市值/换手率（课程要求首页展示总市值、成交量；失败静默）
    try:
        extras = await _quote_extras(list(result.keys()))
        for c, extra in extras.items():
            if c in result:
                result[c]["marketCap"] = extra.get("marketCap")
                result[c]["turnoverRate"] = extra.get("turnoverRate")
                result[c]["volumeRatio"] = extra.get("volumeRatio")
    except Exception:
        pass

    return result, was_stale


def _kline_ttl(scale: int) -> float:
    """K 线 TTL：日线(scale>=240)隔夜外，分钟线 short TTL。"""
    if scale >= 240:
        return 6 * 3600.0   # 6h
    return scale * 1.0      # 分钟线：scale 秒量级


async def quote_kline(symbol: str, scale: int = 240, datalen: int = 120) -> dict:
    """GET /quote/kline?symbol=&scale=&datalen=  返回 {symbol,name,data:[{day...}]}"""
    now = time.time()
    cache_key = f"{symbol}|{scale}|{min(datalen, 1023)}"
    cached = _kline_cache.get(cache_key)
    if cached and now - cached[0] <= _kline_ttl(scale):
        return cached[1]

    result: Optional[dict] = None
    is_daily = scale >= 240
    if is_daily and _kline_cache.get(cache_key):
        # 日线数据不过期兜底（stale），header 由外层标 stale
        result = {**_kline_cache[cache_key][1], "isStale": True}
    try:
        url = (
            "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData"
            f"?symbol={symbol}&scale={scale}&ma=no&datalen={min(datalen, 1023)}"
        )
        raw = _http_get(url)
        data = json.loads(raw)
        if isinstance(data, list) and data:
            result = {
                "symbol": symbol,
                "name": data[-1].get("day", ""),
                "data": data,
                "scale": scale,
                "source": "新浪财经",
                "provider": "sina-kline",
                "isStale": False,
            }
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
                "source": "知牛离线快照",
                "provider": "offline-snapshot",
                "isStale": True,
            }

    if result is not None:
        _kline_cache[cache_key] = (now, result)
    return result or {"symbol": symbol, "name": "", "data": [], "scale": scale}


def _eastmoney_secid(symbol: str) -> Optional[str]:
    if not re.fullmatch(r"(?:sh|sz)\d{6}", symbol):
        return None
    return ("1." if symbol.startswith("sh") else "0.") + symbol[2:]


async def _eastmoney_json(url: str) -> dict:
    raw = await asyncio.to_thread(_http_get, _guard_external_url(url), None, _EASTMONEY_HEADERS)
    parsed = json.loads(raw)
    return parsed if isinstance(parsed, dict) else {}


_ALLOWED_API_HOSTS = {
    "push2.eastmoney.com",
    "datacenter-web.eastmoney.com",       # 东财行情快照/板块/选股
    "emappdata.eastmoney.com",   # 东财人气榜
    "searchapi.eastmoney.com",   # 东财全市场搜索建议
    "hq.sinajs.cn",              # 新浪实时
    "quotes.sina.cn",            # 新浪 K 线
    "qt.gtimg.cn",               # 腾讯行情（市值/换手/量比第二真实来源）
}
_TENCENT_HEADERS = {"Referer": "https://gu.qq.com/", "User-Agent": _EASTMONEY_HEADERS["User-Agent"]}


def _guard_external_url(url: str) -> str:
    """出站请求白名单：仅允许 HTTPS + 已知行情域名（防 SSRF / 内网探测）。"""
    from urllib.parse import urlparse

    parsed = urlparse(url)
    if parsed.scheme != "https" or parsed.hostname not in _ALLOWED_API_HOSTS:
        raise ValueError(f"outbound url not allowed: {parsed.hostname}")
    return url


def _post_popularity_rank(page_size: int) -> dict:
    """仅请求固定的东财人气榜端点（白名单校验 + 不接受外部 URL）。"""
    body = {"appId": "appId01", "globalId": "786e4c21", "marketType": "", "pageNo": 1, "pageSize": int(page_size)}
    req = request.Request(
        _guard_external_url(_POPULARITY_URL),
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "User-Agent": _EASTMONEY_HEADERS["User-Agent"]},
        method="POST",
    )
    with request.urlopen(req, timeout=_TIMEOUT) as resp:
        parsed = json.loads(resp.read().decode("utf-8", errors="replace"))
    return parsed if isinstance(parsed, dict) else {}


def _parse_eastmoney_ulist(payload: dict) -> Dict[str, dict]:
    """解析 ulist.np 批量快照：code → {marketCap, turnoverRate, volumeRatio}（缺字段保持 null）。"""
    rows = ((payload.get("data") or {}).get("diff") or [])
    out: Dict[str, dict] = {}
    for row in rows:
        code = str(row.get("f12") or "")
        if len(code) == 6:
            out[code] = {
                "marketCap": _optional_num(row.get("f20")),
                "turnoverRate": _optional_num(row.get("f8")),
                "volumeRatio": _optional_num(row.get("f10")),
            }
    return out


def _parse_tencent_extras(raw: str) -> Dict[str, dict]:
    """解析腾讯 qt.gtimg.cn `v_<code>="..."` 88 字段：38 换手率 %、39 市盈率、45 总市值（亿）→ 元、46 市净率、49 量比。"""
    out: Dict[str, dict] = {}
    for line in raw.split(";"):
        line = line.strip()
        if not line.startswith("v_") or '="' not in line:
            continue
        key, _, rest = line.partition('="')
        symbol = key[2:].lower()
        fields = rest.rstrip('"').split("~")
        if len(fields) < 50 or not re.fullmatch(r"(?:sh|sz)\d{6}", symbol):
            continue
        cap_yi = _optional_num(fields[45])
        out[symbol] = {
            "name": fields[1],
            "marketCap": round(cap_yi * 1e8) if cap_yi is not None else None,
            "turnoverRate": _optional_num(fields[38]),
            "volumeRatio": _optional_num(fields[49]),
            "pe": _optional_num(fields[39]),
            "pb": _optional_num(fields[46]),
        }
    return out


async def _quote_extras(symbols: list) -> Dict[str, dict]:
    """批量补充总市值/换手率/量比：东财 ulist → 腾讯 gtimg 双真实来源（30s TTL；全部失败静默为 null）。"""
    now = time.time()
    out: Dict[str, dict] = {}
    secids: list = []
    need_codes: list = []
    for symbol in symbols:
        cached = _ulist_cache.get(symbol)
        if cached and now - cached[0] <= _ULIST_TTL:
            out[symbol] = cached[1]
            continue
        sid = _eastmoney_secid(symbol)
        if sid:
            secids.append(sid)
            need_codes.append(symbol)
    if secids:
        url = (
            "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&invt=2"
            "&fields=f12,f8,f10,f20&secids=" + ",".join(secids)
        )
        try:
            parsed = _parse_eastmoney_ulist(await _eastmoney_json(url))
            for symbol in need_codes:
                extra = parsed.get(symbol[2:])
                if extra:
                    _ulist_cache[symbol] = (now, extra)
                    out[symbol] = extra
        except Exception:
            pass  # 东财限流/断连 → 走腾讯补充
    missing = [symbol for symbol in need_codes if symbol not in out]
    if missing:
        url = _guard_external_url("https://qt.gtimg.cn/q=" + ",".join(missing))
        try:
            raw = await asyncio.to_thread(_http_get, url, "gbk", _TENCENT_HEADERS)
            for symbol, extra in _parse_tencent_extras(raw).items():
                if symbol in missing:
                    _ulist_cache[symbol] = (now, extra)
                    out[symbol] = extra
        except Exception:
            pass  # 市值/换手缺失时前端显示 —，不阻塞行情
    return out


def _parse_eastmoney_sector(row: dict) -> Optional[dict]:
    name = str(row.get("f14") or "").strip()
    if not name:
        return None
    lead_code = str(row.get("f140") or "")
    lead_symbol = ""
    if len(lead_code) == 6:
        lead_symbol = ("sh" if lead_code.startswith(("5", "6", "9")) else "sz") + lead_code
    return {
        "code": str(row.get("f12") or ""),
        "name": name,
        "changePercent": _optional_num(row.get("f3")),
        "upCount": int(_num(row.get("f104"))),
        "downCount": int(_num(row.get("f105"))),
        "leadStock": str(row.get("f128") or ""),
        "leadSymbol": lead_symbol,
        "leadChangePercent": _optional_num(row.get("f136")),
    }


def _parse_eastmoney_stock_row(row: dict) -> Optional[dict]:
    code = str(row.get("f12") or "")
    name = str(row.get("f14") or "").strip()
    if len(code) != 6 or not name:
        return None
    symbol = ("sh" if code.startswith(("5", "6", "9")) else "sz") + code
    return {
        "symbol": symbol,
        "name": name,
        "price": _optional_num(row.get("f2")),
        "changePercent": _optional_num(row.get("f3")),
        "volume": _optional_num(row.get("f5")),       # 手
        "amount": _optional_num(row.get("f6")),       # 元
        "turnoverRate": _optional_num(row.get("f8")),
        "pe": _optional_num(row.get("f9")),
        "marketCap": _optional_num(row.get("f20")),   # 元
        "industry": str(row.get("f100") or ""),
    }


async def quote_fundamentals(symbol: str) -> dict:
    """Return real valuation, latest published report, and intraday money flow.

    Missing upstream fields stay null/absent; the gateway never fabricates
    financial values. A previously successful response may be returned with
    ``isStale=true`` when Eastmoney is temporarily unavailable.
    """
    secid = _eastmoney_secid(symbol)
    if secid is None:
        return {
            "symbol": symbol, "available": False, "isStale": False,
            "source": "", "provider": "invalid-symbol",
        }
    now = time.time()
    cached = _fundamentals_cache.get(symbol)
    if cached and now - cached[0] <= 300.0:
        return cached[1]

    code = symbol[2:]
    snapshot_url = (
        "https://push2.eastmoney.com/api/qt/stock/get?"
        + urlencode({
            "secid": secid,
            "fields": "f58,f116,f117,f127,f128,f129,f152,f162,f167,f168,f171",
        })
    )
    report_url = (
        "https://datacenter-web.eastmoney.com/api/data/v1/get?"
        + urlencode({
            "reportName": "RPT_LICO_FN_CPD",
            "columns": "ALL",
            "filter": f'(SECURITY_CODE="{code}")',
            "pageNumber": 1,
            "pageSize": 1,
            "sortColumns": "REPORTDATE",
            "sortTypes": -1,
        })
    )
    flow_url = (
        "https://push2.eastmoney.com/api/qt/stock/fflow/kline/get?"
        + urlencode({
            "lmt": 1, "klt": 1, "secid": secid,
            "fields1": "f1,f2,f3,f7",
            "fields2": "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63",
        })
    )
    # 三源独立降级：push2 被墙/限流时，估值走腾讯 gtimg，财报走 datacenter-web，互不拖垮。
    snapshot: dict = {}
    report: dict = {}
    flow: dict = {}
    try:
        snapshot_payload = await _eastmoney_json(snapshot_url)
        snapshot = _parse_eastmoney_snapshot(symbol, snapshot_payload.get("data") or {})
    except Exception:
        snapshot = {}
    try:
        report = _parse_eastmoney_report(await _eastmoney_json(report_url))
    except Exception:
        report = {}
    try:
        flow = _parse_eastmoney_flow(await _eastmoney_json(flow_url))
    except Exception:
        flow = {}

    if not snapshot.get("name"):
        # push2 快照不可达 → 腾讯 gtimg 兜底：名称/PE/PB/总市值（同一批真实行情源）
        try:
            raw = await asyncio.to_thread(
                _http_get,
                _guard_external_url("https://qt.gtimg.cn/q=" + symbol),
                "gbk",
                _TENCENT_HEADERS,
            )
            extra = _parse_tencent_extras(raw).get(symbol) or {}
            if extra:
                snapshot = {
                    "symbol": symbol,
                    "name": extra.get("name") or "",
                    "industry": "", "region": "", "concepts": [],
                    "pe": extra.get("pe"),
                    "pb": extra.get("pb"),
                    "turnoverRate": extra.get("turnoverRate"),
                    "amplitude": None,
                    "marketCap": extra.get("marketCap"),
                    "floatMarketCap": None,
                }
        except Exception:
            pass

    has_any = bool(snapshot.get("pe") or snapshot.get("name") or report or flow)
    if not has_any:
        previous = _fundamentals_last_ok.get(symbol)
        if previous:
            return {**previous, "isStale": True}
        return {
            "symbol": symbol, "available": False, "isStale": False,
            "source": "", "provider": "upstream-unavailable",
        }

    providers = [p for p, ok in (
        ("eastmoney-snapshot", bool(snapshot.get("name"))),
        ("tencent-valuation", bool(snapshot.get("pe")) and not snapshot.get("industry")),
        ("eastmoney-report", bool(report)),
        ("eastmoney-flow", bool(flow)),
    ) if ok]
    result = {
        **snapshot,
        **report,
        "moneyFlow": flow,
        "available": True,
        "source": "东方财富/腾讯" if "tencent-valuation" in providers else "东方财富",
        "provider": "+".join(providers) or "upstream-unavailable",
        "isStale": False,
    }
    _fundamentals_cache[symbol] = (now, result)
    if len(providers) >= 2:
        _fundamentals_last_ok[symbol] = result
    return result


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
    """GET /quote/sectors：东财行业板块涨跌排行（真实 + stale/mock 兜底）。"""
    global _sectors_cache
    now = time.time()
    cached = _sectors_cache
    if cached and now - cached[0] <= _SECTORS_TTL:
        return cached[1]
    try:
        url = (
            "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=100&po=1&np=1"
            "&fltt=2&invt=2&fid=f3&fs=m:90+t:2"
            "&fields=f3,f12,f14,f104,f105,f128,f136,f140"
        )
        payload = await _eastmoney_json(url)
        rows = [r for r in (_parse_eastmoney_sector(row) for row in ((payload.get("data") or {}).get("diff") or [])) if r]
        if rows:
            result = {"sectors": rows, "source": "eastmoney", "isStale": False}
            _sectors_cache = (now, result)
            return result
    except Exception:
        pass  # 网络/超时 → stale → mock
    if cached:
        return {**cached[1], "isStale": True}
    rows = []
    for i, name in enumerate(_SECTORS):
        pct = round(((i * 7) % 11 - 5) + 0.3, 2)  # 确定性涨跌幅
        rows.append({"name": name, "changePercent": pct, "leadStock": f"{name}·龙头"})
    rows.sort(key=lambda r: r["changePercent"], reverse=True)
    return {"sectors": rows, "source": "mock", "isStale": True}


async def quote_screener(industry: str = "", min_pct: float = 0.0, limit: int = 30) -> dict:
    """GET /quote/screener?industry=&min_pct=：条件选股（东财成交额榜前 100 池 + 过滤）。"""
    global _screener_cache
    now = time.time()
    cached = _screener_cache
    pool: list = []
    source = "mock"
    stale = True
    if cached and now - cached[0] <= _SCREENER_TTL:
        pool, source, stale = cached[1]["rows"], cached[1]["source"], False
    else:
        try:
            url = (
                "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=100&po=1&np=1"
                "&fltt=2&invt=2&fid=f6&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23"
                "&fields=f12,f14,f2,f3,f5,f6,f8,f9,f20,f100"
            )
            payload = await _eastmoney_json(url)
            rows = [r for r in (_parse_eastmoney_stock_row(row) for row in ((payload.get("data") or {}).get("diff") or [])) if r]
            if rows:
                pool, source, stale = rows, "eastmoney", False
                _screener_cache = (now, {"rows": rows, "source": source})
        except Exception:
            pass  # 网络/超时 → stale → mock
        if not pool:
            if cached:
                pool, source, stale = cached[1]["rows"], cached[1]["source"], True
            else:
                pool, source, stale = _mock_screener_pool(), "mock", True
    rows = [r for r in pool if (_num(r.get("changePercent"))) >= min_pct]
    if industry:
        rows = [r for r in rows if industry in str(r.get("industry") or "")]
    return {"industry": industry, "min_pct": min_pct, "rows": rows[: max(1, limit)], "source": source, "isStale": stale}


def _mock_screener_pool() -> list:
    """离线兜底选股池（与前端 MockData 对齐的几只）。"""
    return [
        {"symbol": "sh600519", "name": "贵州茅台", "industry": "白酒", "price": 1292.83, "changePercent": 0.10, "pe": 28.2},
        {"symbol": "sz000001", "name": "平安银行", "industry": "银行", "price": 11.30, "changePercent": 1.35, "pe": 5.6},
        {"symbol": "sh600036", "name": "招商银行", "industry": "银行", "price": 36.28, "changePercent": 1.34, "pe": 6.1},
        {"symbol": "sz300750", "name": "宁德时代", "industry": "新能源", "price": 196.80, "changePercent": 1.71, "pe": 24.0},
        {"symbol": "sh601318", "name": "中国平安", "industry": "保险", "price": 48.35, "changePercent": 0.94, "pe": 8.2},
        {"symbol": "sz000858", "name": "五粮液", "industry": "白酒", "price": 127.60, "changePercent": -1.16, "pe": 21.0},
        {"symbol": "sh601398", "name": "工商银行", "industry": "银行", "price": 5.64, "changePercent": 1.08, "pe": 5.4},
        {"symbol": "sh600887", "name": "伊利股份", "industry": "食品", "price": 27.2, "changePercent": 2.1, "pe": 18.0},
    ]


async def quote_popularity(count: int = 20) -> dict:
    """GET /quote/popularity：东财人气榜真实排名 + 新浪实时行情增强（名称/最新价/涨跌幅）。"""
    global _popularity_cache
    now = time.time()
    if _popularity_cache and now - _popularity_cache[0] <= _POPULARITY_TTL:
        return _popularity_cache[1]
    try:
        payload = await asyncio.to_thread(_post_popularity_rank, max(1, min(count, 50)))
        entries = payload.get("data") or []
        ranked: list = []
        for entry in entries:
            symbol = str(entry.get("sc") or "").strip().lower()
            if re.fullmatch(r"(?:sh|sz)\d{6}", symbol):
                ranked.append((symbol, int(_num(entry.get("rk")))))
        if ranked:
            symbols = [s for s, _ in ranked]
            url = _guard_external_url("https://hq.sinajs.cn/list=" + ",".join(symbols))
            raw = await asyncio.to_thread(_http_get, url, "gbk", None)
            quotes = {s: parsed for s in symbols if (parsed := _parse_sina(s, raw))}
            stocks = []
            for symbol, rank in ranked:
                row: dict = {"symbol": symbol, "rank": rank}
                q = quotes.get(symbol)
                if q and q.get("price", 0) > 0:
                    prev = _num(q.get("prevClose"))
                    row.update(
                        name=q.get("name", ""),
                        price=q.get("price"),
                        changePercent=round((q["price"] - prev) / prev * 100, 2) if prev else 0.0,
                    )
                stocks.append(row)
            result = {"stocks": stocks, "source": "eastmoney-popularity", "isStale": False}
            _popularity_cache = (now, result)
            return result
    except Exception:
        pass  # 网络/超时 → stale → mock 池按成交额排序
    if _popularity_cache:
        return {**_popularity_cache[1], "isStale": True}
    rows = sorted(_MOCK_QUOTES.values(), key=lambda q: -_num(q.get("amount")))[: max(1, min(count, 50))]
    stocks = []
    for i, q in enumerate(rows):
        price, prev = _num(q.get("price")), _num(q.get("prevClose"))
        stocks.append({
            "symbol": q.get("symbol", ""),
            "rank": i + 1,
            "name": q.get("name", ""),
            "price": price,
            "changePercent": round((price - prev) / prev * 100, 2) if prev else 0.0,
        })
    return {"stocks": stocks, "source": "mock", "isStale": True}


# ---------- 全市场搜索（东财 suggest，名称/代码/拼音模糊匹配） ----------

_SEARCH_URL = "https://searchapi.eastmoney.com/api/suggest/get"
_SEARCH_TTL = 300.0
_search_cache: tuple = (0.0, "", {"items": [], "source": "", "isStale": False})


def _parse_eastmoney_suggest(payload: dict, count: int) -> list:
    """解析东财 suggest：仅保留沪深 A 股（含科创板独立分类 23），映射为 sh/sz + 6 位代码。"""
    table = payload.get("QuotationCodeTable") or {}
    rows = table.get("Data") or []
    if not isinstance(rows, list):
        return []
    items: list = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        market = str(row.get("MktNum", ""))
        classify = str(row.get("Classify", ""))
        is_star = market == "1" and classify == "23" and str(row.get("SecurityType")) == "25"
        if market not in ("0", "1") or not (classify == "AStock" or is_star):
            continue
        code = str(row.get("Code", ""))
        name = str(row.get("Name", "")).strip()
        if len(code) != 6 or not name:
            continue
        items.append({
            "symbol": ("sh" if market == "1" else "sz") + code,
            "code": code,
            "name": name,
            "market": "SH" if market == "1" else "SZ",
            "securityTypeName": str(row.get("SecurityTypeName", "")).strip(),
        })
        if len(items) >= count:
            break
    return items


async def quote_search(keyword: str, count: int = 10) -> dict:
    """GET /quote/search?keyword=&count=  全市场 A 股搜索；离线回退本地快照匹配。"""
    kw = keyword.strip()
    if not kw or len(kw) > 40 or count < 1:
        return {"items": [], "source": "invalid", "isStale": False}
    count = max(1, min(count, 20))
    now = time.time()
    if _search_cache[0] and now - _search_cache[0] <= _SEARCH_TTL and _search_cache[1] == kw:
        return {**_search_cache[2], "cached": True}
    try:
        url = _SEARCH_URL + "?" + urlencode({"input": kw, "type": "14", "count": str(count)})
        raw = await asyncio.to_thread(_http_get, _guard_external_url(url), None, _EASTMONEY_HEADERS)
        items = _parse_eastmoney_suggest(json.loads(raw), count)
        if items:
            result = {"items": items, "source": "eastmoney-suggest", "isStale": False}
            globals()["_search_cache"] = (now, kw, result)
            return result
    except Exception:
        pass  # 网络/超时 → 本地快照匹配兜底
    items = []
    for q in _MOCK_QUOTES.values():
        name = str(q.get("name", ""))
        symbol = str(q.get("symbol", ""))
        if kw in name or kw in symbol or kw in symbol[-6:]:
            items.append({
                "symbol": symbol,
                "code": symbol[-6:],
                "name": name,
                "market": symbol[:2].upper(),
                "securityTypeName": "A股",
            })
            if len(items) >= count:
                break
    return {"items": items, "source": "local-snapshot" if items else "none", "isStale": True}
