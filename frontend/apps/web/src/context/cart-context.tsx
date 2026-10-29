"use client";

import React, { createContext, useContext, useState, useEffect, useCallback } from "react";
import { MenuItemResponse, OrderItemRequest } from "@swiggy/types";

export interface CartItem extends OrderItemRequest {
  restaurantId: string;
  restaurantName: string;
}

interface CartContextType {
  items: CartItem[];
  restaurantId: string | null;
  restaurantName: string | null;
  addItem: (restaurant: { id: string; name: string }, item: MenuItemResponse) => boolean;
  removeItem: (menuItemId: string) => void;
  updateQuantity: (menuItemId: string, delta: number) => void;
  clearCart: () => void;
  totalAmountCents: number;
  itemCount: number;
  isCartOpen: boolean;
  setIsCartOpen: (open: boolean) => void;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

const CART_KEY = "swiggy_cart_items";

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [isCartOpen, setIsCartOpen] = useState(false);

  // Load cart from localStorage
  useEffect(() => {
    try {
      const stored = localStorage.getItem(CART_KEY);
      if (stored) {
        setItems(JSON.parse(stored));
      }
    } catch {
      setItems([]);
    }
  }, []);

  // Save cart to localStorage
  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(items));
    } catch {
      // Ignore
    }
  }, [items]);

  const restaurantId = items.length > 0 ? items[0].restaurantId : null;
  const restaurantName = items.length > 0 ? items[0].restaurantName : null;

  const addItem = useCallback(
    (restaurant: { id: string; name: string }, item: MenuItemResponse): boolean => {
      // Restaurant boundary invariant check
      if (items.length > 0 && items[0].restaurantId !== restaurant.id) {
        const confirmClear = window.confirm(
          `Your cart contains items from "${items[0].restaurantName}". Discard current cart and start a new order from "${restaurant.name}"?`
        );
        if (!confirmClear) return false;

        setItems([
          {
            menuItemId: item.id,
            name: item.name,
            unitPriceCents: item.priceCents,
            quantity: 1,
            restaurantId: restaurant.id,
            restaurantName: restaurant.name,
          },
        ]);
        setIsCartOpen(true);
        return true;
      }

      setItems((prev) => {
        const existingIdx = prev.findIndex((i) => i.menuItemId === item.id);
        if (existingIdx >= 0) {
          const updated = [...prev];
          updated[existingIdx] = {
            ...updated[existingIdx],
            quantity: updated[existingIdx].quantity + 1,
          };
          return updated;
        }
        return [
          ...prev,
          {
            menuItemId: item.id,
            name: item.name,
            unitPriceCents: item.priceCents,
            quantity: 1,
            restaurantId: restaurant.id,
            restaurantName: restaurant.name,
          },
        ];
      });

      setIsCartOpen(true);
      return true;
    },
    [items]
  );

  const updateQuantity = useCallback((menuItemId: string, delta: number) => {
    setItems((prev) => {
      return prev
        .map((item) => {
          if (item.menuItemId === menuItemId) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : null;
          }
          return item;
        })
        .filter((item): item is CartItem => item !== null);
    });
  }, []);

  const removeItem = useCallback((menuItemId: string) => {
    setItems((prev) => prev.filter((i) => i.menuItemId !== menuItemId));
  }, []);

  const clearCart = useCallback(() => {
    setItems([]);
  }, []);

  const totalAmountCents = items.reduce(
    (sum, item) => sum + item.unitPriceCents * item.quantity,
    0
  );

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <CartContext.Provider
      value={{
        items,
        restaurantId,
        restaurantName,
        addItem,
        removeItem,
        updateQuantity,
        clearCart,
        totalAmountCents,
        itemCount,
        isCartOpen,
        setIsCartOpen,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return context;
}
