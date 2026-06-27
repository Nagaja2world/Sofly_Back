variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "Deployment environment (prod | staging)"
  type        = string
  default     = "prod"

  validation {
    condition     = contains(["prod", "staging"], var.environment)
    error_message = "environment must be 'prod' or 'staging'."
  }
}

variable "project" {
  description = "Project name used as a prefix for all resources"
  type        = string
  default     = "sofly"
}

# ── Lightsail ─────────────────────────────────────────────

variable "lightsail_bundle_id" {
  description = "Lightsail bundle (instance size). medium_3_0 = 2 vCPU / 4 GB RAM"
  type        = string
  default     = "medium_3_0"
}

variable "lightsail_blueprint_id" {
  description = "Lightsail OS blueprint"
  type        = string
  default     = "ubuntu_22_04"
}

variable "lightsail_availability_zone" {
  description = "AZ for the Lightsail instance"
  type        = string
  default     = "ap-northeast-2a"
}

variable "lightsail_ssh_public_key" {
  description = "SSH public key material imported as a Lightsail key pair"
  type        = string
  sensitive   = true
}

# ── S3 ────────────────────────────────────────────────────

variable "s3_media_bucket_name" {
  description = "Name of the S3 bucket used for user-uploaded media"
  type        = string
  default     = "sofly-media-prod"
}

variable "s3_media_lifecycle_days" {
  description = "Days after which non-current object versions are expired"
  type        = number
  default     = 90
}
