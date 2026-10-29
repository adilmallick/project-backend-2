variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "prod"
}

variable "database_subnet_ids" {
  type        = list(string)
  description = "Subnets for the ElastiCache subnet group"
}

variable "security_group_id" {
  type        = string
  description = "Security group ID allowing ECS ingress"
}

variable "node_type" {
  type        = string
  description = "Instance type for Redis cache nodes"
  default     = "cache.t4g.small"
}

variable "num_cache_clusters" {
  type        = number
  description = "Number of cache nodes (1 primary + replicas)"
  default     = 2
}
