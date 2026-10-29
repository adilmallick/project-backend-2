output "alb_dns_name" {
  description = "Public entrypoint URL for the Application Load Balancer"
  value       = module.ecs.alb_dns_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL instance endpoint"
  value       = module.rds.db_endpoint
}

output "redis_primary_endpoint" {
  description = "ElastiCache Redis primary write endpoint"
  value       = module.elasticache.primary_endpoint_address
}

output "kafka_bootstrap_brokers" {
  description = "Amazon MSK Kafka bootstrap brokers"
  value       = module.msk.bootstrap_brokers
}
