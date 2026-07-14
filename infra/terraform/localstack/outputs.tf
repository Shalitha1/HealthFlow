output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "kafka_bootstrap_brokers" {
  value = aws_msk_cluster.main.bootstrap_brokers
}

output "ecr_repository_urls" {
  value = {
    for name, repository in aws_ecr_repository.service :
    name => repository.repository_url
  }
}

output "application_secret_arn" {
  value = aws_secretsmanager_secret.application.arn
}

output "external_secrets_role_arn" {
  value = aws_iam_role.external_secrets.arn
}

output "database_endpoints" {
  value = {
    for name, database in aws_db_instance.service : name => database.address
  }
}
