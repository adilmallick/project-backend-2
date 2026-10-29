/**
 * Catalog Domain types.
 * Mirrors backend `com.swiggy.catalog.dto.*`.
 */
export interface MenuItemResponse {
  id: string;
  name: string;
  description: string;
  priceCents: number;
  isAvailable: boolean;
}

export interface RestaurantResponse {
  id: string;
  name: string;
  description: string;
  address: string;
  ownerId: string;
  isActive: boolean;
  menuItems: MenuItemResponse[];
}

export interface RestaurantSummaryResponse {
  id: string;
  name: string;
  description: string;
  address: string;
  isActive: boolean;
}

export interface CreateRestaurantRequest {
  name: string;
  description: string;
  address: string;
  ownerId: string;
}

export interface CreateMenuItemRequest {
  name: string;
  description: string;
  priceCents: number;
}
