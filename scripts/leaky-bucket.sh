#!/bin/bash

send_message() {
  local tenant=$1
  local phone=${2:-"+385911111111"}
  local message=${3:-"Your OTP code is 123456"}
  curl -s -i -X POST "http://localhost:8080/api/sms/send" \
    -H "X-Tenant-Id: $tenant" \
    -H "Content-Type: application/json" \
    -d "{\"phone\": \"$phone\", \"message\": \"$message\"}"
}

parse_response() {
  local response=$1
  local code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  local remaining=$(echo "$response" | grep -i "^X-Rate-Limit-Remaining:" | awk '{print $2}' | tr -d '\r')
  local retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')

  if [ -n "$retry" ]; then
    echo "$code (retry-after: ${retry}s)"
  elif [ -n "$remaining" ]; then
    echo "$code (remaining: $remaining)"
  else
    echo "$code"
  fi
}

docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1

echo "=== Leaky Bucket (capacity: 10, leak rate: 2/sec) ==="
echo ""
echo "--- Burst: 15 rapid requests ---"
echo ""
for i in $(seq 1 15); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done

echo ""
echo "--- Wait 3s for bucket to drain (6 drops leak) ---"
sleep 3
echo ""

echo "--- After drain: 5 more requests ---"
echo ""
for i in $(seq 16 20); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done
