# =============================================================================
# ECS Fargate & Application Load Balancer Module
# SEE: ADR-001, ADR-009, ADR-010
# =============================================================================

resource "aws_ecs_cluster" "main" {
  name = "delivery-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name        = "delivery-${var.environment}-ecs-cluster"
    Environment = var.environment
  }
}

# -----------------------------------------------------------------------------
# IAM Roles for ECS Execution & Task
# -----------------------------------------------------------------------------
resource "aws_iam_role" "ecs_execution" {
  name = "delivery-${var.environment}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task" {
  name = "delivery-${var.environment}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

# -----------------------------------------------------------------------------
# CloudWatch Log Groups for Microservices
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "logs" {
  for_each          = toset(["user-service", "catalog-service", "order-service", "dispatch-service"])
  name              = "/ecs/delivery-${var.environment}-${each.key}"
  retention_in_days = 30

  tags = {
    Environment = var.environment
    Service     = each.key
  }
}

# -----------------------------------------------------------------------------
# Public Application Load Balancer (ALB)
# -----------------------------------------------------------------------------
resource "aws_lb" "main" {
  name               = "delivery-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = var.environment == "prod" ? true : false

  tags = {
    Name        = "delivery-${var.environment}-alb"
    Environment = var.environment
  }
}

# -----------------------------------------------------------------------------
# ALB Target Groups for each Microservice
# -----------------------------------------------------------------------------
resource "aws_lb_target_group" "user" {
  name        = "delivery-${var.environment}-user-tg"
  port        = 8081
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_target_group" "catalog" {
  name        = "delivery-${var.environment}-catalog-tg"
  port        = 8082
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_target_group" "order" {
  name        = "delivery-${var.environment}-order-tg"
  port        = 8083
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_target_group" "dispatch" {
  name        = "delivery-${var.environment}-dispatch-tg"
  port        = 8084
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  # DECISION: Stickiness enabled for STOMP WebSocket session stability.
  # SEE: ADR-009
  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400
    enabled         = true
  }

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# -----------------------------------------------------------------------------
# ALB Listener & Path-Based Routing Rules
# -----------------------------------------------------------------------------
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "application/json"
      message_body = "{\"error\": \"Resource Not Found - Invalid route\"}"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "user_rule" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.user.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/auth/*", "/api/v1/users/*"]
    }
  }
}

resource "aws_lb_listener_rule" "catalog_rule" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 20

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.catalog.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/restaurants/*"]
    }
  }
}

resource "aws_lb_listener_rule" "order_rule" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 30

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.order.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/orders/*"]
    }
  }
}

resource "aws_lb_listener_rule" "dispatch_rule" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 40

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.dispatch.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/dispatch/*", "/ws-tracking", "/ws-tracking/*"]
    }
  }
}

# -----------------------------------------------------------------------------
# ECS Fargate Task Definitions & Services
# DECISION: Fargate serverless containers with multi-AZ replication.
# ALTERNATIVES: EC2 ECS instances (higher operational overhead).
# TRADE-OFF: Fargate eliminates host patching at slightly higher per-vCPU cost.
# INVARIANT: minimum_healthy_percent = 100 ensures zero-downtime rolling deploys.
# SEE: ADR-001, ADR-010, ADR-011
# -----------------------------------------------------------------------------

# 1. User Service (Port 8081)
resource "aws_ecs_task_definition" "user" {
  family                   = "delivery-${var.environment}-user-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "user-service"
      image     = var.user_service_image
      essential = true
      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_address}:5432/user_db" },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.logs["user-service"].name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "user"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "user" {
  name            = "delivery-${var.environment}-user-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.user.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.user.arn
    container_name   = "user-service"
    container_port   = 8081
  }

  depends_on = [aws_lb_listener_rule.user_rule]
}

# 2. Catalog Service (Port 8082)
resource "aws_ecs_task_definition" "catalog" {
  family                   = "delivery-${var.environment}-catalog-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "catalog-service"
      image     = var.catalog_service_image
      essential = true
      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_address}:5432/catalog_db" },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.logs["catalog-service"].name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "catalog"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "catalog" {
  name            = "delivery-${var.environment}-catalog-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.catalog.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.catalog.arn
    container_name   = "catalog-service"
    container_port   = 8082
  }

  depends_on = [aws_lb_listener_rule.catalog_rule]
}

# 3. Order Service (Port 8083)
resource "aws_ecs_task_definition" "order" {
  family                   = "delivery-${var.environment}-order-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "order-service"
      image     = var.order_service_image
      essential = true
      portMappings = [
        {
          containerPort = 8083
          hostPort      = 8083
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_address}:5432/order_db" },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
        { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = var.kafka_brokers }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.logs["order-service"].name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "order"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "order" {
  name            = "delivery-${var.environment}-order-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.order.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.order.arn
    container_name   = "order-service"
    container_port   = 8083
  }

  depends_on = [aws_lb_listener_rule.order_rule]
}

# 4. Dispatch Service (Port 8084)
resource "aws_ecs_task_definition" "dispatch" {
  family                   = "delivery-${var.environment}-dispatch-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "dispatch-service"
      image     = var.dispatch_service_image
      essential = true
      portMappings = [
        {
          containerPort = 8084
          hostPort      = 8084
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.logs["dispatch-service"].name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "dispatch"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "dispatch" {
  name            = "delivery-${var.environment}-dispatch-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.dispatch.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.dispatch.arn
    container_name   = "dispatch-service"
    container_port   = 8084
  }

  depends_on = [aws_lb_listener_rule.dispatch_rule]
}
