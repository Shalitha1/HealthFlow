#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TAG="${1:-1.0.0}"

build_image() {
  local image_name="$1"
  local context="$2"

  echo "Building ${image_name}:${TAG} in minikube"
  minikube image build -t "${image_name}:${TAG}" "${ROOT_DIR}/${context}"
}

# Build sequentially to limit memory pressure on development machines.
build_image api-gateway api-gateway
build_image auth-service auth-service
build_image patient-service patient-management
build_image billing-service billing-service
build_image audit-service audit-service
build_image notification-service notification-service
