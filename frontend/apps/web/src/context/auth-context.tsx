"use client";

import React, { createContext, useContext, useState, useEffect, useCallback } from "react";
import { UserRole, AuthResponse, UserResponse, LoginRequest, RegisterRequest } from "@swiggy/types";
import { SERVICES } from "@swiggy/config";
import { apiFetch, ApiError } from "@/lib/api-client";

interface AuthContextType {
  user: UserResponse | null;
  token: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  switchDemoRole: (role: UserRole) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Pre-seeded Demo accounts matching backend test suite
export const DEMO_USERS: Record<UserRole, { user: UserResponse; token: string }> = {
  CUSTOMER: {
    user: {
      id: "d3b07384-d113-40e1-bbd5-f86749964d7d",
      email: "customer@example.com",
      role: "CUSTOMER",
      name: "Alice Johnson",
      phone: "+91 9876543210",
      createdAt: new Date().toISOString(),
    },
    token: "demo-jwt-token-customer-alice",
  },
  RESTAURANT_OWNER: {
    user: {
      id: "b1b07384-d113-40e1-bbd5-f86749964d7b",
      email: "owner@punjabigrill.com",
      role: "RESTAURANT_OWNER",
      name: "Chef Rajesh Sharma",
      phone: "+91 9876543211",
      createdAt: new Date().toISOString(),
    },
    token: "demo-jwt-token-owner-rajesh",
  },
  RIDER: {
    user: {
      id: "c2b07384-d113-40e1-bbd5-f86749964d7c",
      email: "rider@speedy.com",
      role: "RIDER",
      name: "Rider Vikram Singh",
      phone: "+91 9876543212",
      createdAt: new Date().toISOString(),
    },
    token: "demo-jwt-token-rider-vikram",
  },
  ADMIN: {
    user: {
      id: "a0b07384-d113-40e1-bbd5-f86749964d7a",
      email: "admin@swiggy.local",
      role: "ADMIN",
      name: "Platform Super Admin",
      phone: "+91 9876543213",
      createdAt: new Date().toISOString(),
    },
    token: "demo-jwt-token-admin",
  },
};

const TOKEN_KEY = "swiggy_jwt_token";
const USER_KEY = "swiggy_user_data";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize session from localStorage on client mount
  useEffect(() => {
    try {
      const storedToken = localStorage.getItem(TOKEN_KEY);
      const storedUser = localStorage.getItem(USER_KEY);
      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      } else {
        // Default to demo customer for immediate interactive experience
        const defaultDemo = DEMO_USERS.CUSTOMER;
        setToken(defaultDemo.token);
        setUser(defaultDemo.user);
        localStorage.setItem(TOKEN_KEY, defaultDemo.token);
        localStorage.setItem(USER_KEY, JSON.stringify(defaultDemo.user));
      }
    } catch {
      // Fallback
      setUser(DEMO_USERS.CUSTOMER.user);
      setToken(DEMO_USERS.CUSTOMER.token);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    setIsLoading(true);
    const userUrl = process.env.NEXT_PUBLIC_USER_SERVICE_URL || SERVICES.USER.defaultUrl;
    try {
      const res = await apiFetch<AuthResponse>(userUrl, "/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(credentials),
      });

      const authenticatedUser: UserResponse = {
        id: res.userId,
        email: res.email,
        role: res.role,
        name: res.email.split("@")[0],
        createdAt: new Date().toISOString(),
      };

      setToken(res.accessToken);
      setUser(authenticatedUser);
      localStorage.setItem(TOKEN_KEY, res.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(authenticatedUser));
    } catch (err: unknown) {
      // If service is offline or credentials fail, surface error or fallback
      if (err instanceof ApiError && err.code === "NETWORK_ERROR") {
        // Fallback to customer demo on connection error
        const demo = DEMO_USERS.CUSTOMER;
        setToken(demo.token);
        setUser(demo.user);
        localStorage.setItem(TOKEN_KEY, demo.token);
        localStorage.setItem(USER_KEY, JSON.stringify(demo.user));
        return;
      }
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    setIsLoading(true);
    const userUrl = process.env.NEXT_PUBLIC_USER_SERVICE_URL || SERVICES.USER.defaultUrl;
    try {
      const res = await apiFetch<AuthResponse>(userUrl, "/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(data),
      });

      const authenticatedUser: UserResponse = {
        id: res.userId,
        email: res.email,
        role: res.role,
        name: data.name,
        phone: data.phone,
        createdAt: new Date().toISOString(),
      };

      setToken(res.accessToken);
      setUser(authenticatedUser);
      localStorage.setItem(TOKEN_KEY, res.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(authenticatedUser));
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }, []);

  const switchDemoRole = useCallback((newRole: UserRole) => {
    const demo = DEMO_USERS[newRole];
    setToken(demo.token);
    setUser(demo.user);
    localStorage.setItem(TOKEN_KEY, demo.token);
    localStorage.setItem(USER_KEY, JSON.stringify(demo.user));
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        role: user?.role ?? null,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
        switchDemoRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
