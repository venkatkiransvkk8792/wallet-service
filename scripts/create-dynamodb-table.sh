#!/usr/bin/env bash
set -euo pipefail

aws dynamodb create-table   --table-name wallet-transfer-read-model   --billing-mode PAY_PER_REQUEST   --attribute-definitions       AttributeName=walletId,AttributeType=S       AttributeName=sortKey,AttributeType=S   --key-schema       AttributeName=walletId,KeyType=HASH       AttributeName=sortKey,KeyType=RANGE   --region "${AWS_REGION:-eu-west-2}" || true
