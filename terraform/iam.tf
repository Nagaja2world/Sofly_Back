resource "aws_iam_user" "app_s3" {
  name = "${local.name_prefix}-app-s3"
  path = "/sofly/"
}

resource "aws_iam_access_key" "app_s3" {
  user = aws_iam_user.app_s3.name
}

resource "aws_iam_user_policy" "app_s3" {
  name = "${local.name_prefix}-s3-rw"
  user = aws_iam_user.app_s3.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "MediaBucketReadWrite"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.media.arn,
          "${aws_s3_bucket.media.arn}/*",
        ]
      }
    ]
  })
}

output "app_s3_access_key_id" {
  description = "AWS_S3_ACCESS_KEY_ID for the Spring Boot services — store in GitHub Actions secrets"
  value       = aws_iam_access_key.app_s3.id
}

output "app_s3_secret_access_key" {
  description = "AWS_S3_SECRET_ACCESS_KEY — store in GitHub Actions secrets, never in code"
  value       = aws_iam_access_key.app_s3.secret
  sensitive   = true
}
