#!/bin/bash

QUEUE_URL="http://localhost:4566/000000000000/tier-change-queue"

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

echo "=== spring-shop is STANDARD (10 req/10s) ==="
for i in $(seq 1 12); do
  response=$(send_message "spring-shop")
  code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  remaining=$(echo "$response" | grep -i "^X-Rate-Limit-Remaining:" | awk '{print $2}' | tr -d '\r')
  retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')
  extra="${remaining:+remaining: $remaining}${retry:+retry-after: ${retry}s}"
  echo "SpringShop  #$i -> $code ${extra:+($extra)}"
done

echo ""
echo "=== Sending SQS message: move spring-shop to PREMIUM ==="
docker exec sms-localstack awslocal sqs send-message \
  --queue-url "$QUEUE_URL" \
  --message-body '{"tenantId":"spring-shop","tier":"PREMIUM"}'

echo ""
echo "Waiting for tier change to be processed..."
sleep 3

echo ""
echo "=== spring-shop is now PREMIUM (100 req/10s) ==="
for i in $(seq 1 12); do
  response=$(send_message "spring-shop")
  code=$(echo "$response" | grep -i "^HTTP" | awk '{print $2}')
  remaining=$(echo "$response" | grep -i "^X-Rate-Limit-Remaining:" | awk '{print $2}' | tr -d '\r')
  retry=$(echo "$response" | grep -i "^Retry-After:" | awk '{print $2}' | tr -d '\r')
  extra="${remaining:+remaining: $remaining}${retry:+retry-after: ${retry}s}"
  echo "SpringShop  #$i -> $code ${extra:+($extra)}"
done
