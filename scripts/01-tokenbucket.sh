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

docker exec sms-redis redis-cli FLUSHALL > /dev/null 2>&1

for i in $(seq 1 25); do
  response=$(send_message "spring-shop")
  code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  remaining=$(echo "$response" | grep -i "^X-Rate-Limit-Remaining:" | awk '{print $2}' | tr -d '\r')
  retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')
  extra="${remaining:+remaining: $remaining}${retry:+retry-after: ${retry}s}"
  echo "SpringShop  #$i -> $code ${extra:+($extra)}"
done

echo ""