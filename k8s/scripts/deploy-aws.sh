#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OVERLAY_DIR="${ROOT_DIR}/k8s/overlays/aws"

if rg -n 'AWS_ACCOUNT_ID|REPLACE_WITH_' "${OVERLAY_DIR}"; then
  echo "Replace every AWS_ACCOUNT_ID and REPLACE_WITH_* placeholder before deployment." >&2
  exit 1
fi

kubectl apply -k "${OVERLAY_DIR}"

for deployment in \
  api-gateway auth-service patient-service billing-service \
  audit-service notification-service; do
  kubectl rollout status \
    "deployment/${deployment}" \
    --namespace patient-platform \
    --timeout=600s || {
      kubectl get pods --namespace patient-platform
      exit 1
    }
done

kubectl get pods,services,ingress,hpa,externalsecrets --namespace patient-platform
