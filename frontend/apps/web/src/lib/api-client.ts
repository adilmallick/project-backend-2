import { ApiResponse, ErrorDetails, ServiceHealth } from "@swiggy/types";
import { SERVICES } from "@swiggy/config";
import { MOCK_RESTAURANTS } from "@/data/mock-catalog";

export interface RequestOptions extends RequestInit {
  token?: string;
  idempotencyKey?: string;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details?: ErrorDetails
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * Generate a random UUID v4 string for distributed request correlation
 */
export function generateCorrelationId(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "corr-" + Math.random().toString(36).substring(2, 15);
}

/**
 * Resilient microservices fetch wrapper.
 * Automatically injects X-Correlation-Id, handles JWT Authorization,
 * distributed idempotency keys (ADR-007), and standard ApiResponse unwrapping.
 */
export async function apiFetch<T>(
  baseUrl: string,
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const url = `${baseUrl.replace(/\/$/, "")}/${endpoint.replace(/^\//, "")}`;
  const correlationId = generateCorrelationId();

  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");
  headers.set("Accept", "application/json");
  headers.set("X-Correlation-Id", correlationId);

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  if (options.idempotencyKey) {
    headers.set("X-Idempotency-Key", options.idempotencyKey);
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    // Handle 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    const json = (await response.json()) as ApiResponse<T>;

    if (!response.ok) {
      const errorCode = json.error?.code || `HTTP_${response.status}`;
      const errorMessage =
        json.error?.message ||
        `Request failed with status ${response.status} (${response.statusText})`;
      throw new ApiError(response.status, errorCode, errorMessage, json.error);
    }

    return (json.data ?? json) as T;
  } catch (err: unknown) {
    if (err instanceof ApiError) {
      throw err;
    }
    const message = err instanceof Error ? err.message : "Network error";
    throw new ApiError(0, "NETWORK_ERROR", message);
  }
}

/**
 * Ping microservice health endpoint with latency measurement
 */
export async function checkServiceHealth(
  name: string,
  url: string,
  port: number,
  healthPath = "/actuator/health"
): Promise<ServiceHealth> {
  const startTime = Date.now();
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    const response = await fetch(`${url}${healthPath}`, {
      method: "GET",
      signal: controller.signal,
      cache: "no-store",
    });

    clearTimeout(timeoutId);
    const responseTimeMs = Date.now() - startTime;

    if (response.ok) {
      const data = await response.json();
      return {
        name,
        status: data.status === "UP" ? "UP" : "DEGRADED",
        port,
        url,
        responseTimeMs,
        details: data,
      };
    }

    return {
      name,
      status: "DEGRADED",
      port,
      url,
      responseTimeMs,
    };
  } catch {
    return {
      name,
      status: "DOWN",
      port,
      url,
      responseTimeMs: Date.now() - startTime,
    };
  }
}

/**
 * Check health across all 4 microservices concurrently
 */
export async function checkAllServices(): Promise<ServiceHealth[]> {
  const userUrl = process.env.NEXT_PUBLIC_USER_SERVICE_URL || SERVICES.USER.defaultUrl;
  const catalogUrl = process.env.NEXT_PUBLIC_CATALOG_SERVICE_URL || SERVICES.CATALOG.defaultUrl;
  const orderUrl = process.env.NEXT_PUBLIC_ORDER_SERVICE_URL || SERVICES.ORDER.defaultUrl;
  const dispatchUrl = process.env.NEXT_PUBLIC_DISPATCH_SERVICE_URL || SERVICES.DISPATCH.defaultUrl;

  return Promise.all([
    checkServiceHealth(SERVICES.USER.name, userUrl, SERVICES.USER.defaultPort),
    checkServiceHealth(SERVICES.CATALOG.name, catalogUrl, SERVICES.CATALOG.defaultPort),
    checkServiceHealth(SERVICES.ORDER.name, orderUrl, SERVICES.ORDER.defaultPort),
    checkServiceHealth(SERVICES.DISPATCH.name, dispatchUrl, SERVICES.DISPATCH.defaultPort),
  ]);
}

/**
 * Fetch restaurant catalog from catalog-service (:8082).
 * Falls back to MOCK_RESTAURANTS when service is unreachable.
 */
export async function fetchRestaurants(token?: string): Promise<import("@swiggy/types").RestaurantResponse[]> {
  const catalogUrl = process.env.NEXT_PUBLIC_CATALOG_SERVICE_URL || SERVICES.CATALOG.defaultUrl;
  try {
    const data = await apiFetch<import("@swiggy/types").RestaurantResponse[]>(
      catalogUrl,
      "/api/v1/restaurants",
      { token }
    );
    return data && data.length > 0 ? data : MOCK_RESTAURANTS;
  } catch {
    return MOCK_RESTAURANTS;
  }
}

/**
 * Submit a new order to order-service (:8083) with distributed idempotency key.
 */
export async function createOrder(
  request: import("@swiggy/types").CreateOrderRequest,
  token?: string,
  idempotencyKey?: string
): Promise<import("@swiggy/types").OrderResponse> {
  const orderUrl = process.env.NEXT_PUBLIC_ORDER_SERVICE_URL || SERVICES.ORDER.defaultUrl;
  const key = idempotencyKey || generateCorrelationId();

  try {
    return await apiFetch<import("@swiggy/types").OrderResponse>(
      orderUrl,
      "/api/v1/orders",
      {
        method: "POST",
        body: JSON.stringify(request),
        token,
        idempotencyKey: key,
        headers: {
          "Idempotency-Key": key,
        },
      }
    );
  } catch (err: unknown) {
    if (err instanceof ApiError && err.code === "NETWORK_ERROR") {
      // Simulate successful local order creation for preview/demo when backend order-service is offline
      const mockOrder: import("@swiggy/types").OrderResponse = {
        id: generateCorrelationId(),
        customerId: request.customerId,
        restaurantId: request.restaurantId,
        riderId: null,
        status: "ORDER_PLACED",
        totalAmountCents: request.items.reduce(
          (sum, i) => sum + i.unitPriceCents * i.quantity,
          0
        ),
        deliveryAddress: request.deliveryAddress,
        version: 0,
        items: request.items.map((i, idx) => ({
          id: `item-${idx}`,
          menuItemId: i.menuItemId,
          name: i.name,
          priceCents: i.unitPriceCents,
          quantity: i.quantity,
        })),
        createdAt: new Date().toISOString(),
      };
      return mockOrder;
    }
    throw err;
  }
}

