variable "environment" {
  type        = string
  description = "Deployment environment (e.g., prod, staging, dev)"
  default     = "prod"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  type        = list(string)
  description = "List of Availability Zones to deploy into"
  default     = ["us-east-1a", "us-east-1b"]
}
