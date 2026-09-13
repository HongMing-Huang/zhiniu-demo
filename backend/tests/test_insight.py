# 知牛 · 洞察推导单元测试（风险五档 + 点位区间，全部确定性规则）
from app.insight import derive_risk_level, derive_trade_advice


def test_risk_level_high_on_flags_and_breakdown():
    out = derive_risk_level(["跌破 MA20", "量能萎缩", "利空新闻"], "down", 18.0, -6.0)
    assert out["level"] == "high"


def test_risk_level_low_on_clean_uptrend():
    out = derive_risk_level([], "up", 55.0, 1.2)
    assert out["level"] == "low"


def test_risk_level_medium_with_one_flag():
    out = derive_risk_level(["量能异常"], "sideways", 50.0, 0.0)
    assert out["level"] == "medium"


def test_advice_from_trader_plan():
    out = derive_trade_advice(185.0, 200.0, 175.0, {"entry": 189.5, "stop": 178.6}, 180.0, 45.0)
    assert out["available"] is True
    assert "178.60" in out["buyRange"] and "189.50" in out["buyRange"]
    assert "交易员计划" in out["basis"][0]


def test_advice_from_levels_without_trader():
    out = derive_trade_advice(100.0, 110.0, 90.0, None, 98.0, 50.0)
    assert out["available"] is True
    assert "90.00" in out["buyRange"] and "110.00" in out["sellRange"]


def test_advice_unavailable_without_levels():
    out = derive_trade_advice(None, None, None, None, None, None)
    assert out["available"] is False
    assert "不编造" in out["rationale"]
