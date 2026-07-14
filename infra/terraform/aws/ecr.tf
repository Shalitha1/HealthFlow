locals {
  services = toset([
    "api-gateway",
    "auth-service",
    "patient-service",
    "billing-service",
    "audit-service",
    "notification-service"
  ])
}

resource "aws_ecr_repository" "service" {
  for_each = local.services

  name                 = each.key
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each = aws_ecr_repository.service

  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the most recent 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
