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

  if [ -n "$retry" ]; then
    echo "$code (retry-after: ${retry}s)"
  elif [ -n "$source" ]; then
    echo "$code (remaining: $remaining, source: $source)"
  else
    echo "$code"
  fi
}

docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1
curl -s -o /dev/null -X POST "http://localhost:8080/internal/health?healthy=true"

echo "=== System HEALTHY ==="
echo ""
for i in $(seq 1 15); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done

echo ""
read -p "Press Enter to simulate system going UNHEALTHY..."
curl -s -o /dev/null -X POST "http://localhost:8080/internal/health?healthy=false"
docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1
echo ""

echo "=== System UNHEALTHY ==="
echo ""
for i in $(seq 1 15); do
  result=$(parse_response "$(send_message "spring-shop")")
  printf "Request #%-2s -> %s\n" "$i" "$result"
done