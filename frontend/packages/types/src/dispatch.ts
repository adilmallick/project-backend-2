/**
 * Dispatch & Live Tracking Domain types.
 * Mirrors backend `com.swiggy.dispatch.dto.*`.
 */
export interface UpdateRiderLocationRequest {
  riderId: string;
  latitude: number;
  longitude: number;
}

export interface NearbyRiderResponse {
  riderId: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
}

export interface TrackingLocationPayload {
  orderId: string;
  riderId: string;
  latitude: number;
  longitude: number;
  status: string;
  timestamp: string;
}
