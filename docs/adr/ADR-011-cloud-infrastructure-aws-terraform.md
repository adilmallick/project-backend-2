# ADR-011: Cloud Architecture, Multi-AZ High Availability & AWS Terraform IaC

## Status
Accepted

## Context
Deploying our distributed food-delivery platform (`user-service`, `catalog-service`, `order-service`, `dispatch-service`) to a production cloud environment requires:
1. Multi-AZ resilience and zero single points of failure across compute, caching, databases, and message brokers.
2. Strict network isolation ensuring database-per-service instances and Kafka brokers are completely unreachable from the public internet.
3. Serverless container orchestration to eliminate virtual machine management and host patching overhead while supporting rolling zero-downtime deployments.
4. WebSocket state persistence across container instances via sticky Application Load Balancer sessions and Redis Pub/Sub cluster backplanes.
5. Strict cloud cost governance and budget alerts to eliminate financial surprises.

## Decisions

### 1. Infrastructure as Code (IaC) with Modular Terraform
- **Decision:** All AWS resources are declared in modular Terraform 1.5+ scripts under `infra/terraform/`.
- **Modularity:** Isolated modules for `vpc`, `rds`, `elasticache`, `msk`, and `ecs`.
- **Reproducibility:** Eliminates snowflake cloud environments, enabling deterministic spin-up of staging and production environments.

### 2. Multi-AZ VPC Network Topology & Zero-Trust Security
- **Decision:** Dedicated VPC (`10.0.0.0/16`) spanning multiple Availability Zones (`us-east-1a`, `us-east-1b`).
- **Tiered Subnets:**
  - **Public Subnets:** ALB and NAT Gateways.
  - **Private Application Subnets:** ECS Fargate tasks with outbound internet access via NAT Gateway for telemetry and package updates.
  - **Private Database Subnets:** Completely isolated subnets for RDS PostgreSQL, ElastiCache Redis, and MSK Kafka with no internet route tables.
- **Security Groups:** Enforce least-privilege traffic flow: Internet -> ALB (80/443) -> ECS Fargate Tasks (8081-8084) -> RDS (5432) / Redis (6379) / MSK (9092-9094).

### 3. Serverless Container Compute (AWS ECS Fargate) & Routing
- **Decision:** Deploy microservices as container tasks on AWS ECS Fargate.
- **Zero-Downtime Deployments:** Configured with `minimum_healthy_percent = 100` and `maximum_percent = 200` coupled with Spring Boot Actuator `/actuator/health` probes.
- **Path-Based Routing:** Single Application Load Balancer routes paths to corresponding target groups:
  - `/api/v1/auth/*`, `/api/v1/users/*` -> `user-service` (:8081)
  - `/api/v1/restaurants/*` -> `catalog-service` (:8082)
  - `/api/v1/orders/*` -> `order-service` (:8083)
  - `/api/v1/dispatch/*`, `/ws-tracking/*` -> `dispatch-service` (:8084)
- **STOMP WebSocket Stickiness:** ALB cookie-based stickiness enabled on the `dispatch-service` target group to preserve long-lived STOMP WebSocket connections.

### 4. Managed Stateful Services Tier
- **RDS PostgreSQL 16 Multi-AZ:** Automated standby replica with synchronous replication, automated daily backups, and gp3 auto-scaling storage (50GB to 200GB).
- **ElastiCache Redis 7 Multi-AZ:** Replication group with in-transit TLS, at-rest encryption, and automatic failover for caching, rate limiting, and the STOMP pub/sub backplane.
- **Amazon MSK Apache Kafka 3.6:** Managed Kafka brokers deployed across private database subnets for reliable Transactional Outbox event streaming.

### 5. Cost Governance & AWS Budgets
- **Decision:** Terraform manages an `aws_budgets_budget` resource alerting at 80% and 100% of a $50/month threshold.
- **Graviton Pricing:** Specifies ARM-based Graviton instances (`t4g`) for database and caching workloads, reducing compute cost by up to 40%.

## Consequences
- **Positive:** Production-ready high availability, automated multi-AZ failover, and zero-downtime rolling updates.
- **Positive:** Zero-trust network segmentation guarantees database credentials and Kafka messages cannot be reached from outside the VPC.
- **Positive:** Fully automated infrastructure lifecycle manageable through GitOps CI/CD pipelines.
- **Negative:** Managed Multi-AZ clusters (RDS, MSK, ElastiCache) incur fixed baseline infrastructure costs; controlled via AWS Budgets and right-sized development tiers.
