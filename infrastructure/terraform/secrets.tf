# Single Secrets Manager secret holding everything the helpdesk pods read from the
# "helpdesk-secrets" Kubernetes Secret. The External Secrets Operator (see
# infrastructure/helm/helpdesk/templates/externalsecret.yaml) syncs it into the cluster.

variable "openai_api_key" {
  description = "OpenAI API key used by ai-service"
  type        = string
  sensitive   = true
}

resource "aws_secretsmanager_secret" "helpdesk" {
  name                    = "helpdesk/${var.environment}"
  description             = "Runtime credentials for the AI helpdesk platform"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "helpdesk" {
  secret_id = aws_secretsmanager_secret.helpdesk.id
  secret_string = jsonencode({
    POSTGRES_USER     = "postgres"
    POSTGRES_PASSWORD = var.db_password
    OPENAI_API_KEY    = var.openai_api_key
  })
}

# IRSA role the External Secrets Operator's service account assumes to read the secret above
# (and nothing else).
data "aws_iam_policy_document" "eso_read_helpdesk_secret" {
  statement {
    actions   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
    resources = [aws_secretsmanager_secret.helpdesk.arn]
  }
}

resource "aws_iam_policy" "eso_read_helpdesk_secret" {
  name   = "helpdesk-${var.environment}-eso-read-secret"
  policy = data.aws_iam_policy_document.eso_read_helpdesk_secret.json
}

module "eso_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name = "helpdesk-${var.environment}-external-secrets"

  role_policy_arns = {
    read_secret = aws_iam_policy.eso_read_helpdesk_secret.arn
  }

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["default:external-secrets-sa"]
    }
  }
}

output "external_secrets_role_arn" {
  description = "Annotate the external-secrets-sa ServiceAccount with eks.amazonaws.com/role-arn=<this>"
  value       = module.eso_irsa.iam_role_arn
}

output "helpdesk_secret_name" {
  value = aws_secretsmanager_secret.helpdesk.name
}
