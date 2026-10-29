/**
 * Standard API Envelope for all HTTP responses across microservices.
 * Directly mirrors backend `com.swiggy.common.dto.ApiResponse<T>`.
 */
export interface ValidationError {
  field: string;
  message: string;
}

export interface ErrorDetails {
  code: string;
  message: string;
  validationErrors?: ValidationError[];
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ErrorDetails;
  timestamp: string;
  correlationId: string;
}

export interface ServiceHealth {
  name: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  port: number;
  url: string;
  responseTimeMs?: number;
  details?: Record<string, unknown>;
}
