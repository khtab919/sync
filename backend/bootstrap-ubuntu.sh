#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ "$(id -u)" != 0 ]]; then
  echo 'Run this script as root.'
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker >/dev/null 2>&1; then
    echo 'Docker is already installed but Compose is missing. Install the matching Compose plugin; this script will not replace an existing Docker installation.'
    exit 1
  fi
  . /etc/os-release
  if [[ "$ID" != ubuntu ]]; then
    echo 'This installer supports Ubuntu only.'
    exit 1
  fi
  if dpkg-query -W -f='${Status}\n' docker.io containerd runc podman-docker 2>/dev/null | grep -q '^install ok installed$'; then
    echo 'Existing container packages found. Resolve Docker package compatibility before continuing; nothing has been removed.'
    exit 1
  fi
  apt-get update
  apt-get install -y ca-certificates curl python3
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  cat > /etc/apt/sources.list.d/layali-docker.sources <<SOURCES
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
SOURCES
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl start docker
bash deploy.sh "${1:-165-227-128-113.sslip.io}"
