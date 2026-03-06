#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# StableBridge Platform — Local K8s Setup
#
# Usage:
#   ./run.sh up      # Build images, create kind cluster, deploy services
#   ./run.sh down    # Destroy kind cluster
#   ./run.sh deploy  # Re-build images, reload into kind, re-deploy manifests
#   ./run.sh build   # Build container images only (Jib → local Docker daemon)
#   ./run.sh status  # Show cluster and pod status
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
INFRA_TF_DIR="$SCRIPT_DIR/../local"
K8S_TF_DIR="$SCRIPT_DIR"

CLUSTER_NAME="stablebridge-local"

# Service images built by Jib
IMAGES=(
  "stablebridge/api-gateway-iam:latest"
  "stablebridge/merchant-onboarding:latest"
  "stablebridge/merchant-iam:latest"
)

# ─────────────────────────────────────────────
# Build container images via Jib
# ─────────────────────────────────────────────
build_images() {
  echo "==> Building container images via Jib..."
  cd "$PROJECT_ROOT"
  ./gradlew :api-gateway-iam:api-gateway-iam:jibDockerBuild \
            :merchant-onboarding:merchant-onboarding:jibDockerBuild \
            :merchant-iam:merchant-iam:jibDockerBuild \
            --parallel --no-daemon
  echo "==> Container images built successfully"
}

# ─────────────────────────────────────────────
# Load images into kind cluster
# ─────────────────────────────────────────────
load_images() {
  echo "==> Loading images into kind cluster..."
  for img in "${IMAGES[@]}"; do
    kind load docker-image "$img" --name "$CLUSTER_NAME"
  done
  echo "==> Images loaded into kind"
}

# ─────────────────────────────────────────────
# Start infra (Terraform Docker provider)
# ─────────────────────────────────────────────
start_infra() {
  echo "==> Starting infrastructure containers..."
  cd "$INFRA_TF_DIR"
  terraform init -input=false
  terraform apply -auto-approve
  echo "==> Infrastructure running"
}

# ─────────────────────────────────────────────
# Create kind cluster + deploy
# ─────────────────────────────────────────────
create_cluster() {
  echo "==> Creating kind cluster + NGINX Ingress..."
  cd "$K8S_TF_DIR"
  terraform init -input=false
  terraform apply -auto-approve
  echo "==> Cluster ready"

  export KUBECONFIG=$(terraform output -raw kubeconfig_path)

  echo "==> Loading images into kind..."
  load_images

  echo "==> Waiting for ingress controller to be ready..."
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=120s

  echo "==> Waiting for application pods to be ready..."
  kubectl wait --namespace stablebridge \
    --for=condition=ready pod \
    --all \
    --timeout=180s || true

  show_status
}

# ─────────────────────────────────────────────
# Re-deploy: rebuild images, reload, apply manifests
# ─────────────────────────────────────────────
deploy() {
  build_images

  cd "$K8S_TF_DIR"
  export KUBECONFIG=$(terraform output -raw kubeconfig_path)

  load_images

  echo "==> Applying kustomize manifests..."
  kubectl apply -k "$PROJECT_ROOT/infra/k8s/overlays/local"
  echo "==> Restarting deployments..."
  kubectl rollout restart deployment -n stablebridge
  kubectl rollout status deployment -n stablebridge --timeout=120s || true
  show_status
}

# ─────────────────────────────────────────────
# Destroy kind cluster
# ─────────────────────────────────────────────
destroy_cluster() {
  echo "==> Destroying kind cluster..."
  cd "$K8S_TF_DIR"
  terraform destroy -auto-approve
  echo "==> Cluster destroyed"
}

# ─────────────────────────────────────────────
# Show status
# ─────────────────────────────────────────────
show_status() {
  cd "$K8S_TF_DIR"
  export KUBECONFIG=$(terraform output -raw kubeconfig_path)
  echo ""
  echo "=== Cluster Nodes ==="
  kubectl get nodes
  echo ""
  echo "=== Pods (stablebridge) ==="
  kubectl get pods -n stablebridge -o wide
  echo ""
  echo "=== Services (stablebridge) ==="
  kubectl get svc -n stablebridge
  echo ""
  echo "=== Ingress ==="
  kubectl get ingress -n stablebridge
  echo ""
  echo "=== Access URLs (path-based routing) ==="
  echo "  API Gateway (S10):     http://localhost/gateway"
  echo "  Onboarding (S11):      http://localhost/onboarding"
  echo "  IAM (S13):             http://localhost/iam"
  echo "  Merchant Portal:       http://localhost"
  echo ""
  echo "  OpenAPI docs:"
  echo "    http://localhost/gateway/swagger-ui.html"
  echo "    http://localhost/onboarding/swagger-ui.html"
  echo "    http://localhost/iam/swagger-ui.html"
}

# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────
case "${1:-help}" in
  up)
    build_images
    start_infra
    create_cluster
    ;;
  down)
    destroy_cluster
    ;;
  deploy)
    deploy
    ;;
  build)
    build_images
    ;;
  status)
    show_status
    ;;
  *)
    echo "Usage: $0 {up|down|deploy|build|status}"
    exit 1
    ;;
esac
