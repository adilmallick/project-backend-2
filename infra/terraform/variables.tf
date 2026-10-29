variable "aws_region" {
  type        = string
  description = "AWS region for deployment"
  default     = "us-east-1"
}

variable "environment" {
  type        = string
  description = "Environment identifier (e.g. prod, staging)"
  default     = "prod"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "db_password" {
  type        = string
  description = "Master password for PostgreSQL database"
  sensitive   = true
}

variable "alert_email" {
  type        = string
  description = "Email address for AWS Budget alerts and DevOps notifications"
  default     = "devops-alerts@swiggy-platform.internal"
}

