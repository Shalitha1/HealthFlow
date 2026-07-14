#!/usr/bin/env bash
set -euo pipefail

minikube status >/dev/null
minikube addons enable ingress
minikube addons enable metrics-server

kubectl wait \
  --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s
