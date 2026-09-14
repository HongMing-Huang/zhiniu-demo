#!/usr/bin/env bash
# 知牛 · 一键开发启动脚本（dev-prompt.md B9）
# 启动服务：
#   8000  后端 LLM 网关（FastAPI）
#   8082  H5 入口（index.html + nativevue2.js 业务同源 + h5App.js 渲染框架）—— 单端口完整可跑
#
# 用法：./scripts/dev.sh
set -e
cd "$(dirname "$0")/.."
ROOT="$(pwd)"

echo "==> [1/2] 启动后端网关 :8000"
(cd "$ROOT/backend" && source .venv/bin/activate 2>/dev/null; nohup uvicorn app.main:app --host 0.0.0.0 --port 8000 > /tmp/zhiniu_backend.log 2>&1 &)
echo "    后端日志: /tmp/zhiniu_backend.log"

echo "==> [2/2] 启动 H5 主入口 :8082（bundle 同源，单端口完整可跑）"
(cd "$ROOT/web-host" && nohup python3 -m http.server 8082 > /tmp/zhiniu_webhost.log 2>&1 &)
echo "    8082 日志: /tmp/zhiniu_webhost.log"

sleep 2
echo ""
echo "✅ 服务就绪："
echo "   - 后端网关  http://localhost:8000/healthz"
echo "   - H5 主入口  http://127.0.0.1:8082/index.html?page_name=MarketList&use_spa=1"
echo ""
echo "可访问页面（?page_name=）：MarketList / StockDetail / AiResearch"
echo "（详情页需带 symbol，如 StockDetail&symbol=sh600519）"
