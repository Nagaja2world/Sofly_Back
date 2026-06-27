resource "aws_lightsail_key_pair" "main" {
  name       = "${local.name_prefix}-keypair"
  public_key = var.lightsail_ssh_public_key
}

resource "aws_lightsail_instance" "app" {
  name              = "${local.name_prefix}-app"
  availability_zone = var.lightsail_availability_zone
  blueprint_id      = var.lightsail_blueprint_id
  bundle_id         = var.lightsail_bundle_id
  key_pair_name     = aws_lightsail_key_pair.main.name

  # cloud-init: Docker + Docker Compose 자동 설치
  user_data = <<-EOT
    #!/bin/bash
    set -e
    apt-get update -y
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
      | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable --now docker
    usermod -aG docker ubuntu
    mkdir -p /opt/sofly
  EOT
}

resource "aws_lightsail_static_ip" "app" {
  name = "${local.name_prefix}-static-ip"
}

resource "aws_lightsail_static_ip_attachment" "app" {
  static_ip_name = aws_lightsail_static_ip.app.name
  instance_name  = aws_lightsail_instance.app.name
}

# ── 인바운드 방화벽 규칙 ───────────────────────────────────

resource "aws_lightsail_instance_public_ports" "app" {
  instance_name = aws_lightsail_instance.app.name

  # SSH
  port_info {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22
  }

  # HTTP (Nginx → HTTPS 리다이렉트용)
  port_info {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80
  }

  # HTTPS
  port_info {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443
  }

  # Prometheus 스크래핑 (내부 모니터링 서버에서만 접근)
  port_info {
    protocol  = "tcp"
    from_port = 9090
    to_port   = 9090
    cidrs     = ["10.0.0.0/8"]
  }
}
