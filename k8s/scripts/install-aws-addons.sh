#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TF_DIR="${ROOT_DIR}/infra/terraform/aws"
EXTERNAL_SECRETS_ROLE_ARN="$(terraform -chdir="${TF_DIR}" output -raw external_secrets_role_arn)"

helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace

helm upgrade --install external-secrets external-secrets \
  --repo https://charts.external-secrets.io \
  --namespace external-secrets \
  --create-namespace \
  --set-string "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=${EXTERNAL_SECRETS_ROLE_ARN}"

helm upgrade --install metrics-server metrics-server \
  --repo https://kubernetes-sigs.github.io/metrics-server/ \
  --namespace kube-system

kubectl wait \
  --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

kubectl wait \
  --namespace external-secrets \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/name=external-secrets \
  --timeout=300s
