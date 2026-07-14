# Phase 12 Terraform environments

- `aws/` provisions the real VPC, EKS, three RDS instances, MSK, ECR,
  Secrets Manager and the External Secrets IAM role.
- `localstack/` mirrors those resources for local AWS API validation and keeps
  separate Terraform state.

Never use `tflocal` from the `aws/` directory and never commit `.env` or
Terraform state files.

## LocalStack

See `localstack/README.md`. The configuration has been initialized and
statically validated. A LocalStack license that includes EKS is required for a
full apply.

## AWS

```bash
cd infra/terraform/aws
terraform init
terraform fmt -recursive
terraform validate
terraform plan -out=aws.tfplan
```

Review the plan and AWS cost before running `terraform apply aws.tfplan`.
EKS, NAT Gateway, three RDS instances and MSK are chargeable resources.
