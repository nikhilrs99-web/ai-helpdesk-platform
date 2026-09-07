locals {
  services = [
    "ticket-service",
    "kb-service",
    "ai-service",
    "notification-service",
    "analytics-service",
    "api-gateway",
    "frontend"
  ]
}

resource "aws_ecr_repository" "helpdesk_repos" {
  for_each             = toset(local.services)
  name                 = "helpdesk/${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Environment = var.environment
  }
}
