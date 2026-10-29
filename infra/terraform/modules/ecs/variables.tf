variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "prod"
}

variable "vpc_id" {
  type        = string
  description = "VPC ID"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for ALB"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for ECS tasks"
}

variable "alb_security_group_id" {
  type        = string
  description = "Security group ID for the ALB"
}

variable "ecs_security_group_id" {
  type        = string
  description = "Security group ID for ECS tasks"
}

variable "db_address" {
  type        = string
  description = "RDS PostgreSQL host address"
}

variable "redis_address" {
  type        = string
  description = "ElastiCache Redis primary endpoint"
}

variable "kafka_brokers" {
  type        = string
  description = "Kafka bootstrap brokers list"
}

variable "user_service_image" {
  type        = string
  description = "ECR Image URI for user-service"
  default     = "123456789012.dkr.ecr.us-east-1.amazonaws.com/user-service:latest"
}

variable "catalog_service_image" {
  type        = string
  description = "ECR Image URI for catalog-service"
  default     = "123456789012.dkr.ecr.us-east-1.amazonaws.com/catalog-service:latest"
}

variable "order_service_image" {
  type        = string
  description = "ECR Image URI for order-service"
  default     = "123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:latest"
}

variable "dispatch_service_image" {
  type        = string
  description = "ECR Image URI for dispatch-service"
  default     = "123456789012.dkr.ecr.us-east-1.amazonaws.com/dispatch-service:latest"
}
