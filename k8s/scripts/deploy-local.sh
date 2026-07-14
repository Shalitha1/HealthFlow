#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

kubectl apply -k "${ROOT_DIR}/k8s/overlays/local"

kubectl rollout status \
  statefulset/postgres \
  --namespace patient-platform \
  --timeout=300s

for deployment in \
  kafka api-gateway auth-service patient-service \
  billing-service audit-service notification-service; do
  kubectl rollout status \
    "deployment/${deployment}" \
    --namespace patient-platform \
    --timeout=300s || {
      kubectl get pods --namespace patient-platform
      exit 1
    }
done

kubectl get pods,services,ingress,hpa --namespace patient-platform
