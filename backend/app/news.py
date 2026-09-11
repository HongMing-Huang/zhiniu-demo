"""Normalized market-news API with live provider, cache and offline snapshot."""
from __future__ import annotations

from typing import Dict, List

from .data_sources import NewsQuery, fetch_market_news, remember_news

# 离线快照只在上游与 stale 缓存都不可用时返回；绝不标记为实时。
_FLASH = [
    {"id": "n1", "time": "14:52", "tag": "宏观", "title": "央行开展 5000 亿 MLF 操作，利率持平，市场流动性充裕",
     "content": "为维护银行体系流动性合理充裕，央行今日开展中期借贷便利（MLF）操作，利率与此前持平。"},
    {"id": "n2", "time": "14:30", "tag": "行业", "title": "半导体板块午后异动拉升，多股涨停",
     "content": "受国产替代预期提振，半导体板块午后集体走强，封测/设备子板块领涨。"},
    {"id": "n3", "time": "14:05", "tag": "公司", "title": "贵州茅台：上半年营收同比增长约 8%，符合预期",
     "content": "贵州茅台发布业绩快报，营收同比增约 8%，净利稳步增长，机构维持增持评级。"},
    {"id": "n4", "time": "13:40", "tag": "资金", "title": "北向资金今日净流入超 60 亿，加仓食品饮料与新能源",
     "content": "北向资金延续流入态势，食品饮料、新能源板块获明显加仓。"},
    {"id": "n5", "time": "13:15", "tag": "政策", "title": "证监会：进一步优化并购重组审核流程",
     "content": "证监会表示将提高并购重组审核效率，支持上市公司通过重组做大做强。"},
    {"id": "n6", "time": "12:40", "tag": "宏观", "title": "6 月规模以上工业增加值同比增 5.3%，好于预期",
     "content": "统计局数据显示，规模以上工业增加值同比增速好于市场预期。"},
    {"id": "n7", "time": "12:00", "tag": "行业", "title": "新能源汽车 6 月销量创新高，渗透率突破 50%",
     "content": "乘联会公布，新能源汽车零售渗透率首次突破 50%，产业链景气度提升。"},
]

# 个股相关新闻（按 symbol/name 关联）
_STOCK_NEWS = [
    {"id": "s1", "time": "13:05", "tag": "公司", "symbol": "sh600519", "name": "贵州茅台",
     "title": "机构：茅台批价企稳，全年目标可期", "content": "多家券商维持买入评级，看好旺季动销。"},
    {"id": "s2", "time": "11:30", "tag": "公司", "symbol": "sz000001", "name": "平安银行",
     "title": "平安银行：零售转型见效，净息差保持韧性", "content": "公司发布中期业绩，零售 AUM 稳步提升。"},
    {"id": "s3", "time": "10:20", "tag": "公司", "symbol": "sz300750", "name": "宁德时代",
     "title": "宁德时代：与全球车企深化合作，市占率稳居第一", "content": "动力电池装机量全球市占率进一步提升。"},
    {"id": "s4", "time": "09:50", "tag": "公告", "symbol": "sh600036", "name": "招商银行",
     "title": "招商银行：拟派发中期股息，股东回报提升", "content": "董事会通过中期分红预案。"},
]

_DETAILS: Dict[str, dict] = {
    n["id"]: {
        **n,
        "publishedAt": n["time"],
        "url": "",
        "source": "知牛离线快照",
        "provider": "offline-snapshot",
        "isStale": True,
    }
    for n in (_FLASH + _STOCK_NEWS)
}


async def news_list(keyword: str = "", symbol: str = "") -> dict:
    """GET /news/list?keyword=&symbol=：实时优先，stale/离线快照兜底。"""
    live, was_stale = await fetch_market_news(NewsQuery(keyword=keyword, symbol=symbol))
    if live:
        remember_news(live, _DETAILS)
        return {
            "items": live,
            "total": len(live),
            "source": "eastmoney-search",
            "isStale": was_stale,
        }

    items: List[dict] = []
    if symbol:
        items = [n for n in _STOCK_NEWS if n.get("symbol") == symbol]
        if not items:
            # 任意 symbol 也返回通用个股新闻（演示兜底）
            items = _STOCK_NEWS
    else:
        items = _FLASH + _STOCK_NEWS
    items = sorted(items, key=lambda n: n["time"], reverse=True)
    if keyword:
        items = [n for n in items if keyword in (n["title"] + n["content"])]
    normalized = [{**_DETAILS[n["id"]]} for n in items]
    return {
        "items": normalized,
        "total": len(normalized),
        "source": "offline-snapshot",
        "isStale": True,
    }


async def news_detail(news_id: str) -> dict:
    """GET /news/detail?news_id=：单条新闻正文。"""
    detail = _DETAILS.get(news_id)
    if not detail:
        return {"error": f"未找到资讯 {news_id}"}
    return detail
