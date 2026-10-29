variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "prod"
}

variable "database_subnet_ids" {
  type        = list(string)
  description = "Subnet IDs for the RDS subnet group"
}

variable "security_group_id" {
  type        = string
  description = "Security group ID allowing ECS ingress"
}

variable "db_instance_class" {
  type        = string
  description = "Compute and memory capacity for the RDS instance"
  default     = "db.t4g.medium"
}

variable "allocated_storage" {
  type        = number
  description = "Allocated storage in GB"
  default     = 50
}

variable "max_allocated_storage" {
  type        = number
  description = "Max auto-scaling storage in GB"
  default     = 200
}

variable "admin_username" {
  type        = string
  description = "PostgreSQL master username"
  default     = "delivery_admin"
}

variable "admin_password" {
  type        = string
  description = "PostgreSQL master password"
  sensitive   = true
}
