# Phase 12 Kubernetes deployment

The manifests use a reusable base and two overlays:

- `overlays/local`: minikube, local PostgreSQL, local Kafka and development secrets.
- `overlays/aws`: EKS, RDS, MSK, ECR and AWS Secrets Manager.

All public application traffic enters through NGINX and is sent to the API
Gateway. The Gateway keeps JWT validation in front of Patient, Billing and
Audit services.

## Resource map

Each application has a ConfigMap, Secret, Deployment and ClusterIP Service in
`base/`. Patient and Auth also have HorizontalPodAutoscalers. Every Deployment
uses `/actuator/health` for startup, readiness and liveness probes.

The empty base Secrets are intentional:

- the local overlay adds development-only values;
- the AWS overlay uses External Secrets Operator to merge values from the
  `patient-management/application` AWS Secrets Manager secret.

## Local minikube workflow

Do not run the full LocalStack stack at the same time on an 8 GB machine.

```bash
minikube start --cpus=2 --memory=4096
./k8s/scripts/install-local-addons.sh
./k8s/scripts/build-local-images.sh
./k8s/scripts/deploy-local.sh
```

Map the hostname to the minikube address:

```bash
echo "$(minikube ip) patient.local" | sudo tee -a /etc/hosts
```

Then test:

```bash
./k8s/scripts/test-e2e.sh http://patient.local
```

The local PostgreSQL StatefulSet creates the three databases. Flyway migrations
in Auth, Patient and Audit services create their tables and seed the admin user.

## AWS workflow

1. Apply `infra/terraform/aws` only after reviewing cost and `terraform plan`.
2. Connect kubectl with `aws eks update-kubeconfig`.
3. Publish amd64 images with `./k8s/scripts/push-ecr-images.sh`.
4. Install cluster add-ons with `./k8s/scripts/install-aws-addons.sh`.
5. Edit `overlays/aws/kustomization.yaml`:
   - replace `AWS_ACCOUNT_ID` in all ECR image names;
   - replace `REPLACE_WITH_MSK_BOOTSTRAP_BROKERS` using the Terraform output;
   - replace `REPLACE_WITH_INGRESS_HOST` with the DNS host.
6. Run `./k8s/scripts/deploy-aws.sh`.
7. Point DNS at the NGINX load balancer and run the end-to-end test.

Useful Terraform values:

```bash
terraform -chdir=infra/terraform/aws output eks_cluster_name
terraform -chdir=infra/terraform/aws output kafka_bootstrap_brokers
terraform -chdir=infra/terraform/aws output ecr_repository_urls
terraform -chdir=infra/terraform/aws output external_secrets_role_arn
```

## Validation and troubleshooting

Render without applying:

```bash
kubectl kustomize k8s/overlays/local
kubectl kustomize k8s/overlays/aws
```

Inspect a failed rollout:

```bash
kubectl get pods -n patient-platform
kubectl describe pod POD_NAME -n patient-platform
kubectl logs POD_NAME -n patient-platform
kubectl get events -n patient-platform --sort-by=.metadata.creationTimestamp
```

Ingress NGINX is included because Phase 12 explicitly requires it. The upstream
project retired in March 2026; a new production deployment should select a
maintained ingress or Gateway API implementation.
