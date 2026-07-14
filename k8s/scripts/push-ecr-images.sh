#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
AWS_REGION="${AWS_REGION:-ap-south-1}"
TAG="${1:-1.0.0}"
AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

aws ecr get-login-password --region "${AWS_REGION}" |
  docker login --username AWS --password-stdin "${REGISTRY}"

build_and_push() {
  local image_name="$1"
  local context="$2"

  docker buildx build \
    --platform linux/amd64 \
    --tag "${REGISTRY}/${image_name}:${TAG}" \
    --push \
    "${ROOT_DIR}/${context}"
}

build_and_push api-gateway api-gateway
build_and_push auth-service auth-service
build_and_push patient-service patient-management
build_and_push billing-service billing-service
build_and_push audit-service audit-service
build_and_push notification-service notification-service

echo "Published tag ${TAG} to ${REGISTRY}"
