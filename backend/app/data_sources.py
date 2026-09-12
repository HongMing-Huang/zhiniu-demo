"""External market-data providers shared by REST endpoints and Agent tools.

The provider boundary is intentionally small: normalize upstream responses here,
attach provenance, and let callers decide how to present stale/fallback data.
"""
from __future__ import annotations

import asyncio
import hashlib
import html
import json
import re
import time
from dataclasses import dataclass
from typing import Dict, Iterable, Optional
from urllib import parse, request


_EASTMONEY_SEARCH = "https://search-api-web.eastmoney.com/search/jsonp"

# 出站白名单：仅 HTTPS + 已知资讯域名（防 SSRF / 内网探测）
_ALLOWED_NEWS_HOSTS = {"search-api-web.eastmoney.com"}


def _guard_external_url(url: str) -> str:
    parsed = parse.urlparse(url)
    if parsed.scheme != "https" or parsed.hostname not in _ALLOWED_NEWS_HOSTS:
        raise ValueError(f"outbound url not allowed: {parsed.hostname}")
    return url
_TIMEOUT = 4.0
_NEWS_TTL = 120.0
_TAG_RE = re.compile(r"<[^>]+>")


@dataclass(frozen=True)
class NewsQuery:
    keyword: str = ""
    symbol: str = ""

    @property
    def search_term(self) -> str:
        if self.keyword.strip():
            return self.keyword.strip()
        digits = "".join(ch for ch in self.symbol if ch.isdigit())
        return digits or "A股"

    @property
    def cache_key(self) -> str:
        return f"{self.symbol.strip().lower()}|{self.search_term}"


def _plain(value: object) -> str:
    text = html.unescape(str(value or ""))
    text = _TAG_RE.sub("", text)
    return " ".join(text.replace("\u3000", " ").split())


def _news_id(url: str, title: str) -> str:
    digest = hashlib.sha1(f"{url}|{title}".encode("utf-8")).hexdigest()[:16]
    return f"em-{digest}"


def _fetch_eastmoney(query: NewsQuery) -> list[dict]:
    """Fetch Eastmoney's public search result, following AKShare's adapter shape."""
    callback = "zhiniuNewsCallback"
    inner = {
        "uid": "",
        "keyword": query.search_term,
        "type": ["cmsArticleWebOld"],
        "client": "web",
        "clientType": "web",
        "clientVersion": "curr",
        "param": {
            "cmsArticleWebOld": {
                "searchScope": "default",
                "sort": "default",
                "pageIndex": 1,
                "pageSize": 12,
                "preTag": "<em>",
                "postTag": "</em>",
            }
        },
    }
    params = parse.urlencode(
        {"cb": callback, "param": json.dumps(inner, ensure_ascii=False), "_": str(int(time.time() * 1000))}
    )
    req = request.Request(
        _guard_external_url(f"{_EASTMONEY_SEARCH}?{params}"),
        headers={
            "Accept": "*/*",
            "Referer": f"https://so.eastmoney.com/news/s?keyword={parse.quote(query.search_term)}",
            "User-Agent": "Mozilla/5.0 zhiniu-gateway",
        },
    )
    with request.urlopen(req, timeout=_TIMEOUT) as response:
        raw = response.read().decode("utf-8", errors="replace").strip()
    prefix = f"{callback}("
    if not raw.startswith(prefix):
        raise ValueError("unexpected Eastmoney JSONP wrapper")
    payload = json.loads(raw[len(prefix) :].rstrip(");"))
    rows = payload.get("result", {}).get("cmsArticleWebOld", [])
    if not isinstance(rows, list):
        raise ValueError("unexpected Eastmoney result shape")

    items: list[dict] = []
    for row in rows:
        title = _plain(row.get("title"))
        code = _plain(row.get("code"))
        if not title or not code:
            continue
        url = f"https://finance.eastmoney.com/a/{code}.html"
        published = _plain(row.get("date"))
        items.append(
            {
                "id": _news_id(url, title),
                "time": published,
                "publishedAt": published,
                "tag": "个股" if query.symbol else "市场",
                "symbol": query.symbol,
                "title": title,
                "content": _plain(row.get("content")),
                "url": url,
                "source": _plain(row.get("mediaName")) or "东方财富",
                "provider": "eastmoney-search",
                "isStale": False,
            }
        )
    if not items:
        raise ValueError("Eastmoney returned no usable news")
    items.sort(key=lambda item: item.get("publishedAt", ""), reverse=True)
    return items


_news_cache: Dict[str, tuple[float, list[dict]]] = {}


async def fetch_market_news(query: NewsQuery) -> tuple[list[dict], bool]:
    """Return normalized news and whether a stale cached result was used."""
    now = time.time()
    cached = _news_cache.get(query.cache_key)
    if cached and now - cached[0] <= _NEWS_TTL:
        return [dict(item) for item in cached[1]], False
    try:
        items = await asyncio.to_thread(_fetch_eastmoney, query)
        _news_cache[query.cache_key] = (now, items)
        return [dict(item) for item in items], False
    except Exception:
        if cached:
            stale = [{**item, "isStale": True} for item in cached[1]]
            return stale, True
        return [], True


def remember_news(items: Iterable[dict], details: Dict[str, dict]) -> None:
    """Index only normalized provider results; detail never dereferences user URLs."""
    for item in items:
        news_id = str(item.get("id", ""))
        if news_id:
            details[news_id] = dict(item)
