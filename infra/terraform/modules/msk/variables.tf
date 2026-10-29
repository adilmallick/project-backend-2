variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "prod"
}

variable "database_subnet_ids" {
  type        = list(string)
  description = "Subnets for MSK broker nodes across multi-AZ"
}

variable "security_group_id" {
  type        = string
  description = "Security group ID allowing ECS ingress"
}

variable "broker_instance_type" {
  type        = string
  description = "EC2 instance type for Kafka brokers"
  default     = "kafka.t3.small"
}
