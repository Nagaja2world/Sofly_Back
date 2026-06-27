output "lightsail_instance_name" {
  description = "Name of the Lightsail instance"
  value       = aws_lightsail_instance.app.name
}

output "lightsail_static_ip" {
  description = "Static public IP attached to the Lightsail instance — use this in DNS / GitHub Actions secrets"
  value       = aws_lightsail_static_ip.app.ip_address
}

output "lightsail_key_pair_name" {
  description = "Name of the imported SSH key pair"
  value       = aws_lightsail_key_pair.main.name
}

output "s3_media_bucket_name" {
  description = "Name of the S3 media bucket"
  value       = aws_s3_bucket.media.bucket
}

output "s3_media_bucket_arn" {
  description = "ARN of the S3 media bucket — use in IAM policies"
  value       = aws_s3_bucket.media.arn
}

output "s3_media_bucket_domain" {
  description = "S3 regional domain — set as S3_ENDPOINT in Spring Boot config"
  value       = aws_s3_bucket.media.bucket_regional_domain_name
}
