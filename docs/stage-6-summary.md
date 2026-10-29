# Stage 6 Summary: Cloud Architecture, Multi-AZ High Availability & AWS Terraform IaC

## Executive Overview
Stage 6 delivers production cloud infrastructure as code (Terraform) and containerization across all microservices (`user-service`, `catalog-service`, `order-service`, `dispatch-service`). The architecture guarantees zero-downtime rolling deployments, multi-AZ high availability, private network isolation for stateful tiers, sticky sessions for STOMP WebSockets, and cost controls with AWS Budget alerts.

---

## Implemented Architecture & Capabilities

### 1. Multi-AZ VPC Network Topology (`infra/terraform/modules/vpc`)
- **VPC Subnet Layout:** Multi-AZ CIDR `10.0.0.0/16` across `us-east-1a` and `us-east-1b`.
- **Subnet Segmentation:**
  - **Public Subnets:** Hosts the Application Load Balancer and NAT Gateway.
  - **Private Application Subnets:** Hosts ECS Fargate container tasks with outbound internet access via NAT Gateway.
  - **Private Database Subnets:** Completely isolated subnets for RDS PostgreSQL, ElastiCache Redis, and MSK Kafka with zero public internet routing.
- **Zero-Trust Security Groups:** Strict security groups allowing ingress only from adjacent tiers: ALB -> ECS -> RDS / Redis / MSK.

### 2. Multi-AZ Managed Stateful Data Tier (`rds`, `elasticache`, `msk`)
- **RDS PostgreSQL 16 Multi-AZ (`modules/rds`):** Automatic synchronous standby replication, daily automated snapshots, gp3 auto-scaling storage (50GB to 200GB), and KMS encryption at rest.
- **ElastiCache Redis 7 Multi-AZ (`modules/elasticache`):** Multi-node replication group with in-transit TLS, at-rest encryption, and automatic failover for caching, rate limiting, and the live STOMP pub/sub backplane.
- **Amazon MSK Apache Kafka 3.6 (`modules/msk`):** Multi-broker Kafka cluster deployed across private database subnets for resilient Transactional Outbox event delivery.

### 3. Serverless Container Orchestration & Ingress (`modules/ecs`)
- **ECS Fargate Cluster:** Serverless container execution for all 4 microservices with CloudWatch log group aggregation (`/ecs/delivery-prod-*`).
- **Application Load Balancer (ALB):** Single public ingress endpoint with path-based routing:
  - `/api/v1/auth/*`, `/api/v1/users/*` -> `user-service` (:8081)
  - `/api/v1/restaurants/*` -> `catalog-service` (:8082)
  - `/api/v1/orders/*` -> `order-service` (:8083)
  - `/api/v1/dispatch/*`, `/ws-tracking/*` -> `dispatch-service` (:8084)
- **STOMP WebSocket Session Stickiness:** Cookie-based stickiness on the `dispatch-service` target group to ensure stable WebSocket connections.
- **Zero-Downtime Rolling Deployments:** `minimum_healthy_percent = 100` and `maximum_percent = 200` coupled with Spring Boot Actuator `/actuator/health` probes.

### 4. Production Containerization (`services/*/Dockerfile`)
- **Minimal Footprint:** `eclipse-temurin:21-jre-alpine` (~180MB base).
- **Security:** Runs as unprivileged user `appuser` (UID 10001).
- **JVM Optimization:** `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`.
- **Health Probing:** Embedded `curl` healthcheck hitting `/actuator/health`.

### 5. Cost Governance & Budget Alarms
- **AWS Budgets:** Configured via `aws_budgets_budget` with monthly threshold of $50 USD.
- **Proactive Notifications:** Automated email notifications triggered at 80% and 100% actual cloud expenditure.
- **Graviton Pricing:** ARM-based Graviton instances (`t4g`) specified for RDS and ElastiCache.

---

## Verification & Documentation
- **Production Runbook:** [`docs/stage-6-aws-deployment.md`](file:///Users/adil/Desktop/project-backend-2/docs/stage-6-aws-deployment.md).
- **Architectural Decision Record:** [ADR-011: Cloud Architecture, Multi-AZ High Availability & AWS Terraform IaC](file:///Users/adil/Desktop/project-backend-2/docs/adr/ADR-011-cloud-infrastructure-aws-terraform.md).
- **Microservices Fat-Jar Build:** `./gradlew bootJar` passing cleanly for all services.
