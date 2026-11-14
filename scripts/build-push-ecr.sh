#!/usr/bin/env bash
set -euo pipefail

: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
: "${AWS_REGION:=eu-west-2}"
: "${IMAGE_TAG:=0.1.0}"

REPOSITORY="${ECR_REPOSITORY:-wallet-service}"
REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

aws ecr describe-repositories --repository-names "$REPOSITORY" --region "$AWS_REGION" >/dev/null 2>&1 ||   aws ecr create-repository --repository-name "$REPOSITORY" --region "$AWS_REGION" >/dev/null

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$REGISTRY"

docker build -t "${REPOSITORY}:${IMAGE_TAG}" .
docker tag "${REPOSITORY}:${IMAGE_TAG}" "${REGISTRY}/${REPOSITORY}:${IMAGE_TAG}"
docker push "${REGISTRY}/${REPOSITORY}:${IMAGE_TAG}"

echo "Image: ${REGISTRY}/${REPOSITORY}:${IMAGE_TAG}"
