"use client";

import React from "react";
import { OrderResponse, OrderStatus } from "@swiggy/types";
import {
  PackageCheck,
  ChefHat,
  Bike,
  CheckCircle2,
  Clock,
  MapPin,
  Sparkles,
} from "lucide-react";

interface OrdersTrackerProps {
  orders: OrderResponse[];
}

export function OrdersTracker({ orders }: OrdersTrackerProps) {
  if (orders.length === 0) return null;

  const steps: { status: OrderStatus; label: string; icon: React.ReactNode }[] = [
    {
      status: "ORDER_PLACED",
      label: "Order Placed",
      icon: <Clock className="w-4 h-4" />,
    },
    {
      status: "RESTAURANT_ACCEPTED",
      label: "Preparing",
      icon: <ChefHat className="w-4 h-4" />,
    },
    {
      status: "RIDER_ASSIGNED",
      label: "Rider Assigned",
      icon: <Bike className="w-4 h-4" />,
    },
    {
      status: "PICKED_UP",
      label: "On the Way",
      icon: <PackageCheck className="w-4 h-4" />,
    },
    {
      status: "DELIVERED",
      label: "Delivered",
      icon: <CheckCircle2 className="w-4 h-4" />,
    },
  ];

  const getStepIndex = (status: OrderStatus) => {
    const idx = steps.findIndex((s) => s.status === status);
    return idx >= 0 ? idx : 0;
  };

  const formatPrice = (cents: number) => `₹${(cents / 100).toFixed(2)}`;

  return (
    <div id="orders" className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h3 className="text-xl font-bold text-white">Live Customer Orders</h3>
          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-mono font-semibold">
            {orders.length} Active
          </span>
        </div>
        <p className="text-xs text-slate-400 font-mono">
          Stage 1 Concurrency & State Machine
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {orders.map((order) => {
          const currentStepIdx = getStepIndex(order.status);

          return (
            <div
              key={order.id}
              className="glass-card rounded-2xl p-5 border border-white/10 space-y-4 relative overflow-hidden"
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <span className="text-[10px] font-mono text-[#fc8019] font-bold uppercase tracking-wider block">
                    Order #{order.id.substring(0, 8)}
                  </span>
                  <h4 className="text-sm font-bold text-white mt-0.5">
                    Total: {formatPrice(order.totalAmountCents)}
                  </h4>
                  <p className="text-xs text-slate-400 flex items-center gap-1 mt-1 truncate">
                    <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span className="truncate">{order.deliveryAddress}</span>
                  </p>
                </div>

                <span className="px-2.5 py-1 rounded-full text-xs font-mono font-bold bg-[#fc8019]/10 text-[#fc8019] border border-[#fc8019]/30">
                  {order.status}
                </span>
              </div>

              {/* State Machine Step Progress Bar */}
              <div className="pt-2">
                <div className="flex items-center justify-between relative">
                  {/* Connecting line */}
                  <div className="absolute left-2 right-2 top-1/2 -translate-y-1/2 h-0.5 bg-slate-800 -z-0" />
                  <div
                    className="absolute left-2 top-1/2 -translate-y-1/2 h-0.5 bg-[#fc8019] transition-all -z-0"
                    style={{
                      width: `${(currentStepIdx / (steps.length - 1)) * 95}%`,
                    }}
                  />

                  {steps.map((step, idx) => {
                    const isDone = idx <= currentStepIdx;
                    const isCurrent = idx === currentStepIdx;

                    return (
                      <div
                        key={step.status}
                        className="flex flex-col items-center gap-1 relative z-10"
                      >
                        <div
                          className={`w-7 h-7 rounded-full flex items-center justify-center text-xs transition-all ${
                            isCurrent
                              ? "bg-[#fc8019] text-white shadow-md shadow-[#fc8019]/40 scale-110"
                              : isDone
                              ? "bg-emerald-500 text-white"
                              : "bg-slate-900 border border-slate-700 text-slate-500"
                          }`}
                        >
                          {step.icon}
                        </div>
                        <span
                          className={`text-[9px] font-medium hidden sm:block ${
                            isCurrent
                              ? "text-[#fc8019] font-bold"
                              : isDone
                              ? "text-slate-200"
                              : "text-slate-400"
                          }`}
                        >
                          {step.label}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Items summary */}
              <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 font-mono">
                <span>
                  {order.items.length} items • Version @{order.version}
                </span>
                <span className="text-emerald-400 flex items-center gap-1">
                  <Sparkles className="w-3 h-3" />
                  Live Sync
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
