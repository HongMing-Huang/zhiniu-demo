"""知牛 · 研究洞察推导（确定性规则，无模型调用）。

从研究管线的既有证据（报价 / 关键位 / 交易员计划 / 风险标记）推导：
- 风险五档：low / medium_low / medium / medium_high / high
- 买卖观察区间：仅由服务端已取得的数值（支撑/压力/交易员计划）推导，
  绝不引入外部数字；推导依据随结果返回，前端原样展示。
"""

from __future__ import annotations

from typing import Any

_RISK_LEVELS = ("low", "medium_low", "medium", "medium_high", "high")

_LEVEL_LABELS = {
    "low": "低",
    "medium_low": "中低",
    "medium": "中",
    "medium_high": "中高",
    "high": "高",
}


def _fmt(v: float | int | None) -> str:
    if v is None:
        return "—"
    return f"{v:,.2f}" if isinstance(v, float) else str(v)


def derive_risk_level(
    risk_flags: list[str],
    direction: str,
    rsi14: float | None,
    change_percent: float,
) -> dict[str, str]:
    """风险五档：按规则风险项数量 + 趋势方向 + RSI 极值打分。"""
    flags = len(risk_flags or [])
    rsi = rsi14 if rsi14 is not None else 50.0
    score = flags
    if direction == "down":
        score += 1
    if change_percent <= -5:
        score += 1
    if rsi >= 80 or rsi <= 20:
        score += 1
    level = _RISK_LEVELS[min(max(score, 0), 4)]
    rationale = f"规则风险项 {flags} 项" + (
        f"；趋势{ {'up': '上行', 'down': '下行', 'sideways': '震荡'}.get(direction, direction) }"
        f"；RSI(14) {rsi:.1f}" if rsi14 is not None else ""
    )
    return {"level": level, "label": _LEVEL_LABELS[level], "rationale": rationale}


def derive_trade_advice(
    price: float | None,
    pressure: float | None,
    support: float | None,
    trader: dict | None,
    ma20: float | None,
    rsi14: float | None,
) -> dict[str, Any]:
    """买卖观察区间：全部数值来自服务端已取得的证据，区间边界用确定性规则合成。

    - 有交易员计划（entry/stop）→ 买入观察 [stop, entry]，卖出观察 [entry, pressure或entry*1.05]
    - 否则有支撑/压力 → 买入观察 [support, (support+price)/2]，卖出观察 [(price+pressure)/2, pressure]
    - 都没有 → 明示暂不提供区间（不编造）
    """
    basis: list[str] = []
    entry = trader.get("entry") if isinstance(trader, dict) else None
    stop = trader.get("stop") if isinstance(trader, dict) else None
    if price and entry and stop and stop < entry:
        buy = f"{_fmt(stop)} ~ {_fmt(entry)}"
        sell_top = pressure if pressure and pressure > entry else entry * 1.05
        sell = f"{_fmt(entry)} ~ {_fmt(sell_top)}"
        basis.append(f"交易员计划：入场 {_fmt(entry)} / 止损 {_fmt(stop)}")
        if pressure:
            basis.append(f"压力位 {_fmt(pressure)}")
        rationale = "区间由交易员计划与关键位合成；跌破止损价则观察逻辑失效。"
    elif price and support and pressure and support < price < pressure:
        buy = f"{_fmt(support)} ~ {_fmt((support + price) / 2)}"
        sell = f"{_fmt((price + pressure) / 2)} ~ {_fmt(pressure)}"
        basis.append(f"支撑位 {_fmt(support)} / 压力位 {_fmt(pressure)}")
        if ma20:
            basis.append(f"MA20 {_fmt(ma20)}")
        if rsi14:
            basis.append(f"RSI(14) {rsi14:.1f}")
        rationale = "区间由支撑/压力关键位对折合成；突破或跌破关键位后需重新评估。"
    else:
        return {
            "available": False,
            "buyRange": "",
            "sellRange": "",
            "rationale": "关键位数据不足，暂不提供买卖观察区间（不编造价格）。",
            "basis": [],
        }
    return {"available": True, "buyRange": buy, "sellRange": sell, "rationale": rationale, "basis": basis}


def build_insight(
    quote: dict,
    technical: dict,
    risks: list[str],
    synthesis: dict,
) -> dict[str, Any]:
    """汇总风险分级 + 点位建议，注入研究报告。"""
    levels = synthesis.get("levels") or {}
    trader = synthesis.get("trader") or {}
    direction = (technical or {}).get("direction", "")
    rsi14 = (technical or {}).get("rsi14")
    change_percent = quote.get("changePct", 0.0) or 0.0
    price = quote.get("price")
    return {
        "riskLevel": derive_risk_level(risks, direction, rsi14, change_percent),
        "advice": derive_trade_advice(
            price,
            levels.get("pressure"),
            levels.get("support"),
            trader,
            (technical or {}).get("ma20"),
            rsi14,
        ),
    }
