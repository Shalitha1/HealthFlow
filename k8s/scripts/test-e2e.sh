#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://patient.local}"
EMAIL="phase12.$(date +%s)@example.com"

command -v jq >/dev/null || {
  echo "jq is required to read the login response." >&2
  exit 1
}

TOKEN="$(curl --fail --silent --show-error \
  --request POST "${BASE_URL}/auth/login" \
  --header 'Content-Type: application/json' \
  --data '{"email":"admin@pm.com","password":"admin123"}' |
  jq -r '.token')"

if [[ -z "${TOKEN}" || "${TOKEN}" == "null" ]]; then
  echo "Login did not return a JWT." >&2
  exit 1
fi

curl --fail --silent --show-error \
  --request POST "${BASE_URL}/api/patients" \
  --header "Authorization: Bearer ${TOKEN}" \
  --header 'Content-Type: application/json' \
  --data "{\"name\":\"Phase 12 Patient\",\"email\":\"${EMAIL}\",\"address\":\"Colombo\",\"dateOfBirth\":\"1995-05-10\"}" |
  jq .

curl --fail --silent --show-error \
  "${BASE_URL}/api/patients" \
  --header "Authorization: Bearer ${TOKEN}" |
  jq .

curl --fail --silent --show-error \
  "${BASE_URL}/audit-logs" \
  --header "Authorization: Bearer ${TOKEN}" |
  jq .

echo "End-to-end test completed for ${EMAIL}."
