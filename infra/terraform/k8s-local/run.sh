#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# StableBridge Platform — Local K8s Setup
#
# Usage:
#   ./run.sh up      # Build images, create kind cluster, deploy services
#   ./run.sh down    # Destroy kind cluster
#   ./run.sh deploy  # Re-build images, reload into kind, re-deploy manifests
#   ./run.sh build   # Build all container images (BE via Jib, FE via Docker)
#   ./run.sh status  # Show cluster and pod status
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FE_ROOT="$(cd "$PROJECT_ROOT/../stablebridge-web" 2>/dev/null && pwd || echo "")"
INFRA_TF_DIR="$SCRIPT_DIR/../local"
K8S_TF_DIR="$SCRIPT_DIR"

CLUSTER_NAME="stablebridge-local"

# All images to load into kind
IMAGES=(
  "stablebridge/api-gateway-iam:latest"
  "stablebridge/merchant-onboarding:latest"
  "stablebridge/merchant-iam:latest"
  "stablebridge/merchant-portal:latest"
)

# ─────────────────────────────────────────────
# Build BE container images via Jib
# ─────────────────────────────────────────────
build_be_images() {
  echo "==> Building BE container images via Jib..."
  cd "$PROJECT_ROOT"
  ./gradlew :api-gateway-iam:api-gateway-iam:jibDockerBuild \
            :merchant-onboarding:merchant-onboarding:jibDockerBuild \
            :merchant-iam:merchant-iam:jibDockerBuild \
            --parallel --no-daemon
  echo "==> BE images built successfully"
}

# ─────────────────────────────────────────────
# Build FE container images via Docker
# ─────────────────────────────────────────────
build_fe_images() {
  if [ -z "$FE_ROOT" ]; then
    echo "==> WARN: stablebridge-web repo not found at $PROJECT_ROOT/../stablebridge-web — skipping FE build"
    return 0
  fi
  echo "==> Building FE container images via Docker..."
  docker build --build-arg APP_NAME=merchant-portal \
    -t stablebridge/merchant-portal:latest \
    "$FE_ROOT"
  echo "==> FE images built successfully"
}

# ─────────────────────────────────────────────
# Build all images
# ─────────────────────────────────────────────
build_images() {
  build_be_images
  build_fe_images
}

# ─────────────────────────────────────────────
# Load images into kind cluster
# ─────────────────────────────────────────────
load_images() {
  echo "==> Loading images into kind cluster..."
  for img in "${IMAGES[@]}"; do
    if docker image inspect "$img" >/dev/null 2>&1; then
      kind load docker-image "$img" --name "$CLUSTER_NAME"
    else
      echo "    WARN: $img not found locally — skipping"
    fi
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
  echo "  Merchant Portal:       http://localhost"
  echo "  API Gateway (S10):     http://localhost/gateway"
  echo "  Onboarding (S11):      http://localhost/onboarding"
  echo "  IAM (S13):             http://localhost/iam"
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
