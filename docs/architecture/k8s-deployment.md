# Kubernetes Deployment Guide

This document outlines the steps to deploy the AI Helpdesk Platform to a local `kind` cluster or a managed Kubernetes environment using our Umbrella Helm Chart.

## Prerequisites
- Docker & `kind` (Kubernetes in Docker)
- `kubectl`
- `helm` (v3+)

## Local Deployment on `kind`

### 1. Create the Cluster
Create a kind cluster with Ingress controller ports exposed:
```bash
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
EOF
```

### 2. Install NGINX Ingress Controller
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
```

### 3. Deploy the Umbrella Helm Chart
Navigate to the root directory where the `helpdesk-platform` chart resides and install it. This umbrella chart automatically provisions Bitnami PostgreSQL, Kafka, Redis, and Keycloak, along with all our custom microservices and the React frontend.

```bash
cd infrastructure/helm/helpdesk
helm dependency update
helm install helpdesk-release . --namespace helpdesk-system --create-namespace
```

### 4. Configure Local DNS
Add the following to your `/etc/hosts` (or `C:\Windows\System32\drivers\etc\hosts`):
```
127.0.0.1 helpdesk.local
```

### 5. Verify and Access
Wait for all pods to reach `Running` state:
```bash
kubectl get pods -n helpdesk-system -w
```
Once everything is healthy, open your browser to `http://helpdesk.local`.
