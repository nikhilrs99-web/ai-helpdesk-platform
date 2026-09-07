# AWS Terraform Deployment (Phase 12)

This document outlines the Infrastructure-as-Code (IaC) setup for deploying the AI Helpdesk Platform to AWS.

## Architecture
- **VPC**: A highly available VPC spanning 3 Availability Zones, complete with Public, Private, and Database subnets. Includes a NAT Gateway.
- **EKS**: Amazon Elastic Kubernetes Service cluster (`ai-helpdesk-cluster-prod`) running Kubernetes 1.29. Node groups are provisioned on `t3.medium` instances.
- **RDS**: Managed PostgreSQL 16 database (`db.t4g.micro`) with the `pgvector` extension enabled for AI embeddings.
- **ElastiCache**: Managed Redis 7.0 for agent presence pub/sub, rate limiting, and caching.
- **ECR**: 7 private Docker registries to hold our microservice and frontend images.
- **S3 State**: Remote Terraform state is stored securely in an encrypted S3 bucket and locked via DynamoDB.

## Deployment Instructions (Dry Run)

To validate the deployment without incurring AWS costs, we execute a dry run:
```bash
cd infrastructure/terraform
terraform init
terraform plan -var="db_password=secure_dry_run_pass" -out=tfplan
```

## Teardown Script

Because this is a portfolio project, you must tear down the infrastructure to avoid incurring long-term AWS charges.

```bash
cd infrastructure/terraform
terraform destroy -var="db_password=secure_dry_run_pass" -auto-approve
```
