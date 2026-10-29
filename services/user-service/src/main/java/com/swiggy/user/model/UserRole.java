package com.swiggy.user.model;

/**
 * Domain roles for Role-Based Access Control (RBAC).
 *
 * <p>DECISION: Enum representation stored as VARCHAR strings in PostgreSQL
 * for human readability and straightforward schema migrations.
 *
 * <p>SEE: ADR-003
 */
public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    RIDER,
    ADMIN
}
