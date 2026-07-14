# LocalStack Terraform environment

This directory keeps LocalStack state separate from the real AWS environment.
Use `tflocal` here; do not run `tflocal` from `../aws`.

## Prerequisites

- Docker Desktop is running.
- `terraform-local` is installed and provides the `tflocal` command.
- A LocalStack license with EKS support is assigned to your account.
- `.env` contains your `LOCALSTACK_AUTH_TOKEN`.

## First-time setup

```bash
export PATH="$HOME/Library/Python/3.9/bin:$PATH"
cp .env.example .env
# Edit .env and set LOCALSTACK_AUTH_TOKEN.
docker compose up -d
curl http://localhost:4566/_localstack/health
```

## Terraform workflow

```bash
tflocal init
tflocal fmt -recursive
tflocal validate
tflocal plan -out=localstack.tfplan
tflocal apply localstack.tfplan
tflocal output
```

## Verify the emulated account

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  aws sts get-caller-identity --endpoint-url=http://localhost:4566
```

The default LocalStack account ID is `000000000000`.

## Clean up

```bash
tflocal destroy
docker compose down
```

Use `docker compose down -v` only when the persistent LocalStack volume should
also be deleted.
