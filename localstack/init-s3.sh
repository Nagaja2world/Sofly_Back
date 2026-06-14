#!/bin/bash
# LocalStack 시작 시 자동으로 S3 버킷 생성
awslocal s3 mb s3://sofly-bucket --region ap-northeast-2
awslocal s3api put-bucket-acl --bucket sofly-bucket --acl public-read
echo "LocalStack: sofly-bucket 생성 완료"
