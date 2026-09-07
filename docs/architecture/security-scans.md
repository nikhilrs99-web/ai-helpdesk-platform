# Security Scanning & GitOps (Phase 13)

This document outlines the CI/CD pipeline and security hardening measures implemented via GitHub Actions and Argo CD.

## Continuous Integration (GitHub Actions)
The `.github/workflows/ci-cd.yml` workflow enforces the following gates on every push/PR:
1. **Maven Build & Test**: Compiles all Java services and runs the full JUnit/Testcontainers suite.
2. **OWASP Dependency-Check**: Scans the `pom.xml` dependency tree for known CVEs. Fails the build if critical vulnerabilities are found.
3. **SonarQube Code Quality**: Performs static analysis (SAST) to detect bugs, code smells, and security hotspots.
4. **Docker Build & Push**: Multi-stage Docker builds push immutable images to AWS ECR, tagged with the Git SHA.
5. **Trivy Image Scan**: Scans the compiled Docker images in ECR for OS-level and library vulnerabilities before deployment.

## Continuous Deployment (GitOps via Argo CD)
1. After passing all CI gates, the pipeline updates the image tags in `infrastructure/helm/helpdesk/values.yaml` and commits the change back to the repository.
2. **Argo CD** (installed in the `argocd` namespace on EKS) detects the configuration drift.
3. Argo CD automatically synchronizes the new Helm chart state to the cluster, performing rolling updates on the deployments.

## OWASP ZAP Baseline Scan (DAST)
A Dynamic Application Security Testing (DAST) scan was performed against the staging deployment using OWASP ZAP:
```bash
docker run -t owasp/zap2docker-stable zap-baseline.py -t https://staging.ai-helpdesk.com -g gen.conf -r report.html
```

### Findings Summary
- **Content Security Policy (CSP) Header Not Set**: Addressed via Spring Security configuration.
- **Strict-Transport-Security (HSTS) Header**: Addressed by enforcing HTTPS at the AWS ALB Ingress Controller level.
- **X-Frame-Options Header**: Denied by default in Spring Security to prevent clickjacking.
