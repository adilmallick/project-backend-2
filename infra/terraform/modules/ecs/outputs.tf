output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "target_group_user_arn" {
  value = aws_lb_target_group.user.arn
}

output "target_group_catalog_arn" {
  value = aws_lb_target_group.catalog.arn
}

output "target_group_order_arn" {
  value = aws_lb_target_group.order.arn
}

output "target_group_dispatch_arn" {
  value = aws_lb_target_group.dispatch.arn
}
