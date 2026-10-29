/**
 * Order Lifecycle State Machine and models.
 * Mirrors backend `com.swiggy.order.model.OrderStatus` and `com.swiggy.order.dto.*`.
 */
export type OrderStatus =
  | 'ORDER_PLACED'
  | 'RESTAURANT_ACCEPTED'
  | 'RIDER_ASSIGNED'
  | 'PICKED_UP'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface OrderItemRequest {
  menuItemId: string;
  name: string;
  unitPriceCents: number;
  quantity: number;
}

export interface OrderItemResponse {
  id: string;
  menuItemId: string;
  name: string;
  priceCents: number;
  quantity: number;
}

export interface CreateOrderRequest {
  customerId: string;
  restaurantId: string;
  deliveryAddress: string;
  items: OrderItemRequest[];
}

export interface OrderResponse {
  id: string;
  customerId: string;
  restaurantId: string;
  riderId: string | null;
  status: OrderStatus;
  totalAmountCents: number;
  deliveryAddress: string;
  version: number;
  items: OrderItemResponse[];
  createdAt: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
}
