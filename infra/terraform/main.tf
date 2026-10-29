# =============================================================================
# Production Food Delivery Platform - AWS Infrastructure (Terraform)
# Architecture: Multi-AZ VPC, ECS Fargate, RDS PostgreSQL, ElastiCache, MSK
# SEE: ADR-001, ADR-006, ADR-008, ADR-010
# =============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
  }

  # In production, state is stored in an S3 backend with DynamoDB state locking
  # backend "s3" {
  #   bucket         = "delivery-platform-tfstate"
  #   key            = "production/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "delivery-platform-tflocks"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "SwiggyMiniDelivery"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# -----------------------------------------------------------------------------
# 1. VPC & Networking Module
# -----------------------------------------------------------------------------
module "vpc" {
  source      = "./modules/vpc"
  environment = var.environment
  vpc_cidr    = var.vpc_cidr
}

# -----------------------------------------------------------------------------
# 2. Relational Database Module (RDS PostgreSQL Multi-AZ)
# -----------------------------------------------------------------------------
module "rds" {
  source              = "./modules/rds"
  environment         = var.environment
  database_subnet_ids = module.vpc.private_db_subnet_ids
  security_group_id   = module.vpc.rds_security_group_id
  admin_password      = var.db_password
}

# -----------------------------------------------------------------------------
# 3. In-Memory Caching & State Module (ElastiCache Redis)
# -----------------------------------------------------------------------------
module "elasticache" {
  source              = "./modules/elasticache"
  environment         = var.environment
  database_subnet_ids = module.vpc.private_db_subnet_ids
  security_group_id   = module.vpc.redis_security_group_id
}

# -----------------------------------------------------------------------------
# 4. Asynchronous Messaging Module (Amazon MSK Kafka)
# -----------------------------------------------------------------------------
module "msk" {
  source              = "./modules/msk"
  environment         = var.environment
  database_subnet_ids = module.vpc.private_db_subnet_ids
  security_group_id   = module.vpc.msk_security_group_id
}

# -----------------------------------------------------------------------------
# 5. Compute & Routing Module (ECS Fargate + Application Load Balancer)
# -----------------------------------------------------------------------------
module "ecs" {
  source                = "./modules/ecs"
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  public_subnet_ids     = module.vpc.public_subnet_ids
  private_subnet_ids    = module.vpc.private_app_subnet_ids
  alb_security_group_id = module.vpc.alb_security_group_id
  ecs_security_group_id = module.vpc.ecs_security_group_id

  db_address    = module.rds.db_address
  redis_address = module.elasticache.primary_endpoint_address
  kafka_brokers = module.msk.bootstrap_brokers
}

# -----------------------------------------------------------------------------
# 6. AWS Budgets & Cost Governance
# DECISION: Proactive monthly budget alert ($50 USD) at 80% and 100% actual spend.
# SEE: ADR-011, docs/stage-6-aws-deployment.md
# -----------------------------------------------------------------------------
resource "aws_budgets_budget" "monthly_spend" {
  name              = "delivery-${var.environment}-monthly-budget"
  budget_type       = "COST"
  limit_amount      = "50"
  limit_unit        = "USD"
  time_unit         = "MONTHLY"
  time_period_start = "2026-01-01_00:00"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.alert_email]
  }
}

