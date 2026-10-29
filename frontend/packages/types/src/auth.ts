/**
 * Domain roles for Role-Based Access Control (RBAC).
 * Mirrors backend `com.swiggy.user.model.UserRole`.
 */
export type UserRole = 'CUSTOMER' | 'RESTAURANT_OWNER' | 'RIDER' | 'ADMIN';

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone?: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  role: UserRole;
}

export interface UserResponse {
  id: string;
  email: string;
  role: UserRole;
  name: string;
  phone?: string;
  createdAt: string;
}
