# Secrets Management

As part of our final security hardening (Phase 14), all plaintext `.env` configurations have been stripped from the repository and migrated to enterprise-grade secret managers.

## AWS Secrets Manager Integration
We utilize AWS Secrets Manager as our single source of truth for all sensitive configuration in the production EKS environment, including:
- `POSTGRES_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `SPRING_AI_OPENAI_API_KEY`
- `GRAFANA_ADMIN_PASSWORD`

## External Secrets Operator (ESO)
Rather than manually creating Kubernetes `Secret` objects, we deploy the **External Secrets Operator** to our EKS cluster. ESO authenticates with AWS via IRSA (IAM Roles for Service Accounts) and automatically synchronizes the AWS Secrets Manager values into native Kubernetes Secrets, which are then mounted as environment variables in our Pods.

This ensures zero plaintext credentials exist in our GitOps repository (`values.yaml`) or in our CI/CD logs.
