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
  local source=$(echo "$response" | grep -i "^X-Rate-Limit-Source:" | awk '{print $2}' | tr -d '\r')
  local retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')

  if [ -n "$retry" ] && [ -n "$source" ]; then
    echo "$code (retry-after: ${retry}s, source: $source)"
  elif [ -n "$retry" ]; then
    echo "$code (retry-after: ${retry}s)"
  elif [ -n "$source" ]; then
    echo "$code (remaining: $remaining, source: $source)"
  else
    echo "$code"
  fi
}

docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1

echo "=== Redis UP ==="
echo ""
for i in $(seq 1 12); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done

echo ""
read -p "Press Enter to stop Redis..."
docker stop sms-redis > /dev/null 2>&1
echo ""

echo "=== Redis DOWN — in-memory fallback ==="
echo ""
for i in $(seq 1 12); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done

echo ""
read -p "Press Enter to start Redis again..."
docker start sms-redis > /dev/null 2>&1
echo -n "Waiting for Redis"
until docker exec sms-redis redis-cli ping > /dev/null 2>&1; do
  echo -n "."
  sleep 0.5
done
echo " ready"

read -p "Press Enter to run Redis UP example..."
echo ""

echo "=== Redis UP ==="
echo ""
for i in $(seq 1 12); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done