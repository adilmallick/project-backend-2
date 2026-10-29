"use client";

import React, { useState } from "react";
import { useCart } from "@/context/cart-context";
import { useAuth } from "@/context/auth-context";
import { createOrder } from "@/lib/api-client";
import { OrderResponse } from "@swiggy/types";
import {
  ShoppingBag,
  X,
  Plus,
  Minus,
  Trash2,
  MapPin,
  CheckCircle,
  Loader2,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";

interface CartDrawerProps {
  onOrderPlaced?: (order: OrderResponse) => void;
}

export function CartDrawer({ onOrderPlaced }: CartDrawerProps) {
  const {
    items,
    restaurantName,
    restaurantId,
    updateQuantity,
    removeItem,
    clearCart,
    totalAmountCents,
    itemCount,
    isCartOpen,
    setIsCartOpen,
  } = useCart();

  const { user, token } = useAuth();
  const [deliveryAddress, setDeliveryAddress] = useState(
    "Flat 402, Prestige Tower, Indiranagar, Bengaluru"
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [placedOrder, setPlacedOrder] = useState<OrderResponse | null>(null);

  if (!isCartOpen) return null;

  const handleCheckout = async () => {
    if (!restaurantId || items.length === 0 || !user) return;

    setIsSubmitting(true);
    try {
      const order = await createOrder(
        {
          customerId: user.id,
          restaurantId,
          deliveryAddress,
          items: items.map((i) => ({
            menuItemId: i.menuItemId,
            name: i.name,
            unitPriceCents: i.unitPriceCents,
            quantity: i.quantity,
          })),
        },
        token || undefined
      );

      setPlacedOrder(order);
      clearCart();
      if (onOrderPlaced) {
        onOrderPlaced(order);
      }
    } catch (err: unknown) {
      alert("Order placement failed: " + (err instanceof Error ? err.message : "Error"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatPrice = (cents: number) => `₹${(cents / 100).toFixed(2)}`;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-sm flex justify-end animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-[#0d131f] border-l border-white/10 h-full flex flex-col shadow-2xl">
        {/* Header */}
        <div className="p-5 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-[#fc8019]/20 text-[#fc8019]">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Your Cart</h3>
              {restaurantName && (
                <p className="text-xs text-slate-400">From {restaurantName}</p>
              )}
            </div>
          </div>

          <button
            onClick={() => {
              setIsCartOpen(false);
              setPlacedOrder(null);
            }}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {placedOrder ? (
            <div className="text-center py-8 space-y-4">
              <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto border border-emerald-500/30">
                <CheckCircle className="w-8 h-8" />
              </div>
              <h4 className="text-xl font-bold text-white">Order Confirmed!</h4>
              <p className="text-xs text-slate-300 max-w-xs mx-auto">
                Your order has been staged into PostgreSQL with transactional outbox and optimistic locking.
              </p>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-left font-mono text-xs space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-400">Order ID:</span>
                  <span className="text-[#fc8019] truncate max-w-[180px]">{placedOrder.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Status:</span>
                  <span className="text-emerald-400 font-bold">{placedOrder.status}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Total:</span>
                  <span className="text-white font-bold">{formatPrice(placedOrder.totalAmountCents)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Address:</span>
                  <span className="text-slate-300 truncate max-w-[180px]">{placedOrder.deliveryAddress}</span>
                </div>
              </div>

              <button
                onClick={() => {
                  setPlacedOrder(null);
                  setIsCartOpen(false);
                }}
                className="w-full py-2.5 rounded-xl bg-[#fc8019] text-white font-semibold text-sm hover:opacity-90 transition-all cursor-pointer"
              >
                Track Live Order
              </button>
            </div>
          ) : items.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <ShoppingBag className="w-12 h-12 mx-auto text-slate-600 mb-3" />
              <p className="text-sm font-medium">Your cart is empty</p>
              <p className="text-xs text-slate-400 mt-1">
                Explore restaurants below and add your favorite dishes
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Item rows */}
              <div className="divide-y divide-slate-800/80">
                {items.map((item) => (
                  <div key={item.menuItemId} className="py-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-white truncate">{item.name}</p>
                      <p className="text-xs text-slate-400 font-mono">
                        {formatPrice(item.unitPriceCents)} each
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <div className="flex items-center rounded-lg bg-slate-800 border border-slate-700">
                        <button
                          onClick={() => updateQuantity(item.menuItemId, -1)}
                          className="p-1 hover:text-[#fc8019] text-slate-400 transition-colors cursor-pointer"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="px-2 text-xs font-bold text-white min-w-[20px] text-center">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => updateQuantity(item.menuItemId, 1)}
                          className="p-1 hover:text-[#fc8019] text-slate-400 transition-colors cursor-pointer"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <span className="text-xs font-mono font-bold text-slate-200 min-w-[60px] text-right">
                        {formatPrice(item.unitPriceCents * item.quantity)}
                      </span>

                      <button
                        onClick={() => removeItem(item.menuItemId)}
                        className="p-1 text-slate-500 hover:text-rose-400 transition-colors cursor-pointer"
                        title="Remove item"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Delivery Address */}
              <div className="pt-4 border-t border-slate-800">
                <label className="text-xs font-medium text-slate-300 flex items-center gap-1.5 mb-2">
                  <MapPin className="w-3.5 h-3.5 text-[#fc8019]" />
                  <span>Delivery Address</span>
                </label>
                <input
                  type="text"
                  value={deliveryAddress}
                  onChange={(e) => setDeliveryAddress(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-[#fc8019]"
                />
              </div>

              {/* Idempotency badge */}
              <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center gap-2 text-xs text-emerald-400 font-mono">
                <ShieldCheck className="w-4 h-4 shrink-0" />
                <span>Protected by Redis SETNX Idempotency Key</span>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        {!placedOrder && items.length > 0 && (
          <div className="p-5 border-t border-white/10 bg-slate-900/60 space-y-4">
            <div className="space-y-1.5 text-xs font-mono">
              <div className="flex justify-between text-slate-400">
                <span>Subtotal ({itemCount} items)</span>
                <span>{formatPrice(totalAmountCents)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Delivery Partner Fee</span>
                <span className="text-emerald-400">FREE</span>
              </div>
              <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                <span>Total Amount</span>
                <span className="text-[#fc8019]">{formatPrice(totalAmountCents)}</span>
              </div>
            </div>

            <button
              onClick={handleCheckout}
              disabled={isSubmitting}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-[#fc8019] to-[#e23744] text-white font-bold text-sm shadow-lg shadow-[#fc8019]/30 hover:opacity-95 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Placing Order with Idempotency...</span>
                </>
              ) : (
                <>
                  <span>Place Order • {formatPrice(totalAmountCents)}</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
