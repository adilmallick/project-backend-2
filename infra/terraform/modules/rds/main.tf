# =============================================================================
# RDS Module: High-Availability Multi-AZ PostgreSQL 16
# SEE: ADR-001, ADR-010
# =============================================================================

resource "aws_db_subnet_group" "rds" {
  name        = "delivery-${var.environment}-rds-subnet-group"
  subnet_ids  = var.database_subnet_ids
  description = "Isolated database subnets for Multi-AZ PostgreSQL"

  tags = {
    Name        = "delivery-${var.environment}-rds-subnet-group"
    Environment = var.environment
  }
}

resource "aws_db_instance" "postgres" {
  identifier                  = "delivery-${var.environment}-postgres"
  engine                      = "postgres"
  engine_version              = "16.2"
  instance_class              = var.db_instance_class
  allocated_storage           = var.allocated_storage
  max_allocated_storage       = var.max_allocated_storage
  storage_type                = "gp3"
  storage_encrypted           = true
  multi_az                    = true
  publicly_accessible         = false
  db_subnet_group_name        = aws_db_subnet_group.rds.name
  vpc_security_group_ids      = [var.security_group_id]

  username                    = var.admin_username
  password                    = var.admin_password

  backup_retention_period     = 7
  backup_window               = "03:00-04:00"
  maintenance_window          = "Sun:04:30-Sun:05:30"
  auto_minor_version_upgrade  = true
  deletion_protection         = var.environment == "prod" ? true : false
  skip_final_snapshot         = var.environment == "prod" ? false : true
  final_snapshot_identifier   = "delivery-${var.environment}-postgres-final-snapshot"

  tags = {
    Name        = "delivery-${var.environment}-postgres"
    Environment = var.environment
  }
}
