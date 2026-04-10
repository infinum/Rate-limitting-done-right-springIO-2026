#!/bin/bash

TENANT="spring-shop"

send_message() {
  curl -s -i -X POST "http://localhost:8080/api/sms/send" \
    -H "X-Tenant-Id: $TENANT" \
    -H "Content-Type: application/json" \
    -d '{"phone": "+385911111111", "message": "Your OTP code is 123456"}'
}

print_response() {
  local index=$1
  local response=$2
  local code remaining retry extra

  code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  remaining=$(echo "$response" | grep -i "^X-Rate-Limit-Remaining:" | awk '{print $2}' | tr -d '\r')
  retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')
  extra="${remaining:+remaining: $remaining}${retry:+retry-after: ${retry}s}"
  printf "#%-3s -> %s %s\n" "$index" "$code" "${extra:+($extra)}"
}

docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1

# --- Phase 1: Normal operation ---
read -rp "Press Enter to start Phase 1: Normal sending (~5 req/s)..."
echo ""
echo "=== Phase 1: Normal sending (~5 req/s) — all should pass ==="
for i in $(seq 1 8); do
  response=$(send_message)
  print_response "$i" "$response"
  sleep 0.2
done

echo ""

# --- Phase 2: Sudden burst ---
read -rp "Press Enter to start Phase 2: Sudden spike..."
echo ""
echo "=== Phase 2: Sudden spike (15 req at once) — short-term limit kicks in ==="
for i in $(seq 9 23); do
  response=$(send_message)
  print_response "$i" "$response"
done

echo ""
echo "Waiting 2s for short-term bucket to refill..."
sleep 2
echo ""

# --- Phase 3: Back to normal ---
read -rp "Press Enter to start Phase 3: Back to normal after cooldown..."
echo ""
echo "=== Phase 3: Back to normal — passes again ==="
for i in $(seq 24 28); do
  response=$(send_message)
  print_response "$i" "$response"
  sleep 0.2
done

echo ""

# --- Phase 4: Drain hourly budget ---
read -rp "Press Enter to start Phase 4: Exhaust the long-term limit..."
echo ""
echo "=== Phase 4: Keep sending — long-term limit (100/hour) will be exhausted ==="
i=29
while true; do
  response=$(send_message)
  print_response "$i" "$response"
  code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  i=$((i + 1))
  # Stop after we've seen the hourly limit kick in (a few 429s after short-term refills)
  if [ "$i" -gt 130 ]; then
    break
  fi
  sleep 0.05
done

echo ""