#!/bin/bash
# 知牛 · 网络间歇阻断时的推送重试（GitHub 443 间歇 SSL_ERROR_SYSCALL）
cd /Users/c14h14n3/Desktop/demo/zhiniu
for i in 1 2 3 4 5 6 7 8; do
  sleep 120
  if git push origin main 2>&1 | grep -q "main -> main"; then
    echo "PUSHED at attempt $i $(date)"
    exit 0
  fi
done
echo "ALL RETRIES FAILED"
exit 1
