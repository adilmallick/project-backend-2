# =============================================================================
# Amazon MSK (Managed Streaming for Apache Kafka) Module
# SEE: ADR-008
# =============================================================================

resource "aws_msk_cluster" "kafka" {
  cluster_name           = "delivery-${var.environment}-msk"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = length(var.database_subnet_ids)

  broker_node_group_info {
    instance_type   = var.broker_instance_type
    client_subnets  = var.database_subnet_ids
    security_groups = [var.security_group_id]
    storage_info {
      ebs_storage_info {
        volume_size = 50
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
      in_cluster    = true
    }
  }

  tags = {
    Name        = "delivery-${var.environment}-msk"
    Environment = var.environment
  }
}
