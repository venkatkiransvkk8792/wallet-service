#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:=eu-west-2}"
: "${ECR_IMAGE:?Set ECR_IMAGE to the pushed wallet-service image}"
: "${RDS_URL:?Set RDS_URL, e.g. jdbc:postgresql://host:5432/walletdb}"
: "${RDS_USERNAME:?Set RDS_USERNAME}"
: "${RDS_PASSWORD:?Set RDS_PASSWORD}"

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml

kubectl -n wallet create secret generic wallet-service   --from-literal=DB_URL="$RDS_URL"   --from-literal=DB_USERNAME="$RDS_USERNAME"   --from-literal=DB_PASSWORD="$RDS_PASSWORD"   --from-literal=AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"   --from-literal=AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"   --dry-run=client -o yaml | kubectl apply -f -

sed "s|YOUR_ECR_REPOSITORY/wallet-service:0.1.0|${ECR_IMAGE}|g" k8s/wallet-service.yaml |
  kubectl apply -f -

kubectl apply -f k8s/hpa.yaml
kubectl -n wallet rollout status deployment/wallet-service

kubectl -n wallet get pods
kubectl -n wallet get service wallet-service
