# =============================================================================
# ElastiCache Redis Module: Multi-AZ In-Memory Cluster
# SEE: ADR-006, ADR-007, ADR-009
# =============================================================================

resource "aws_elasticache_subnet_group" "redis" {
  name        = "delivery-${var.environment}-redis-subnet-group"
  subnet_ids  = var.database_subnet_ids
  description = "Isolated database subnets for ElastiCache Redis"

  tags = {
    Name        = "delivery-${var.environment}-redis-subnet-group"
    Environment = var.environment
  }
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "delivery-${var.environment}-redis"
  description                = "Redis cluster for caching, rate limiting, and pub/sub backplane"
  node_type                  = var.node_type
  num_cache_clusters         = var.num_cache_clusters
  port                       = 6379
  parameter_group_name       = "default.redis7"
  subnet_group_name          = aws_elasticache_subnet_group.redis.name
  security_group_ids         = [var.security_group_id]

  automatic_failover_enabled = true
  multi_az_enabled           = true
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  tags = {
    Name        = "delivery-${var.environment}-redis"
    Environment = var.environment
  }
}
