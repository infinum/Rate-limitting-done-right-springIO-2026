#!/bin/bash
set -euo pipefail

REGION="us-east-1"
PREFIX="/config/ratelimiting"

echo "Creating Parameter Store entries"

# STANDARD tier limits
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_0_.capacity" --value "10" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_0_.refill-tokens" --value "10" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_0_.refill-duration" --value "10s" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_1_.capacity" --value "1000" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_1_.refill-tokens" --value "1000" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.STANDARD.limits_1_.refill-duration" --value "1h" --type String --region "$REGION"

# PREMIUM tier limits
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_0_.capacity" --value "100" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_0_.refill-tokens" --value "100" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_0_.refill-duration" --value "10s" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_1_.capacity" --value "10000" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_1_.refill-tokens" --value "10000" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tiers.PREMIUM.limits_1_.refill-duration" --value "1h" --type String --region "$REGION"

# Tenant-tier mappings
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tenant-tiers.spring-shop" --value "STANDARD" --type String --region "$REGION"
awslocal ssm put-parameter --name "$PREFIX/rate-limiting.tenant-tiers.spring-bank" --value "PREMIUM" --type String --region "$REGION"

echo "Parameter Store entries created:"
awslocal ssm get-parameters-by-path --path "$PREFIX" --recursive --region "$REGION" --query "Parameters[].Name" --output table

# SQS queue
QUEUE_NAME="tier-change-queue"
echo "Creating SQS queue: $QUEUE_NAME"
awslocal sqs create-queue --queue-name "$QUEUE_NAME" --region "$REGION"

echo "SQS queue URL:"
awslocal sqs get-queue-url --queue-name "$QUEUE_NAME"
