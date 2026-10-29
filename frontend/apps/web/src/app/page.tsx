"use client";

import React, { useState } from "react";
import { MicroserviceHealthCard } from "@/components/microservice-health-card";
import { RestaurantGrid } from "@/components/catalog/restaurant-grid";
import { OrdersTracker } from "@/components/orders/orders-tracker";
import { CartDrawer } from "@/components/cart/cart-drawer";
import { StageMatrix } from "@/components/stage-matrix";
import { OrderResponse } from "@swiggy/types";
import {
  Sparkles,
  Zap,
  ShieldCheck,
  Radio,
  Layers,
  ArrowRight,
  Terminal,
} from "lucide-react";

export default function HomePage() {
  const [orders, setOrders] = useState<OrderResponse[]>([
    {
      id: "11111111-1111-1111-1111-111111111111",
      customerId: "d3b07384-d113-40e1-bbd5-f86749964d7d",
      restaurantId: "c4b07384-d113-40e1-bbd5-f86749964d7c",
      riderId: "rider-101",
      status: "RESTAURANT_ACCEPTED",
      totalAmountCents: 56300,
      deliveryAddress: "Flat 402, Prestige Tower, Indiranagar, Bengaluru",
      version: 1,
      items: [
        {
          id: "item-1",
          menuItemId: "a1b07384-d113-40e1-bbd5-f86749964d7a",
          name: "Paneer Butter Masala",
          priceCents: 24900,
          quantity: 2,
        },
        {
          id: "item-2",
          menuItemId: "a2b07384-d113-40e1-bbd5-f86749964d7b",
          name: "Butter Garlic Naan",
          priceCents: 6500,
          quantity: 1,
        },
      ],
      createdAt: new Date().toISOString(),
    },
  ]);

  const handleOrderPlaced = (newOrder: OrderResponse) => {
    setOrders((prev) => [newOrder, ...prev]);
  };

  return (
    <div className="space-y-12">
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-3xl glass-card p-8 md:p-12 border border-white/10">
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-br from-[#fc8019]/20 to-[#e23744]/20 rounded-full blur-3xl -z-10 pointer-events-none" />
        <div className="absolute -bottom-20 -left-20 w-80 h-80 bg-blue-500/10 rounded-full blur-3xl -z-10 pointer-events-none" />

        <div className="max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900/80 border border-[#fc8019]/30 text-xs font-medium text-[#fc8019] mb-6">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Frontend Stage 0 & Stage 1: Core Domain & Auth Active</span>
          </div>

          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-white leading-tight">
            Swiggy Rush / Zomato Pulse{" "}
            <span className="bg-gradient-to-r from-[#fc8019] to-[#e23744] bg-clip-text text-transparent">
              Platform Client
            </span>
          </h1>

          <p className="mt-4 text-base md:text-lg text-slate-300 leading-relaxed">
            High-performance web client architected with{" "}
            <strong className="text-white font-semibold">Turborepo</strong>,{" "}
            <strong className="text-white font-semibold">Next.js 15 (App Router)</strong>,{" "}
            <strong className="text-white font-semibold">TypeScript</strong>,{" "}
            <strong className="text-white font-semibold">Tailwind CSS v4</strong>, and{" "}
            <strong className="text-white font-semibold">TanStack Query v5</strong>.
            Synchronized directly with Java 21 Virtual Threads & Spring Boot 3.4 microservices.
          </p>

          <div className="mt-8 flex flex-wrap items-center gap-4">
            <a
              href="#catalog"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-[#fc8019] to-[#e23744] text-white font-semibold text-sm shadow-lg shadow-[#fc8019]/25 hover:opacity-95 transition-all cursor-pointer"
            >
              <span>Order Food Now</span>
              <ArrowRight className="w-4 h-4" />
            </a>

            <a
              href="#services"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-slate-200 font-medium text-sm transition-all cursor-pointer"
            >
              <Zap className="w-4 h-4 text-[#fc8019]" />
              <span>Microservices Status</span>
            </a>
          </div>
        </div>

        {/* Feature Pills */}
        <div className="mt-10 pt-8 border-t border-white/10 grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-indigo-500/10 text-indigo-400">
              <Zap className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-400">Build System</p>
              <p className="text-sm font-bold text-white">Turborepo</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-400">Contracts</p>
              <p className="text-sm font-bold text-white">@swiggy/types</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-[#fc8019]/10 text-[#fc8019]">
              <Radio className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-400">Server State</p>
              <p className="text-sm font-bold text-white">TanStack Query v5</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-400">Styling</p>
              <p className="text-sm font-bold text-white">Tailwind CSS v4</p>
            </div>
          </div>
        </div>
      </section>

      {/* Live Orders Section */}
      <OrdersTracker orders={orders} />

      {/* Restaurant Catalog Section */}
      <RestaurantGrid />

      {/* Live Microservices Health Section */}
      <section id="services">
        <MicroserviceHealthCard />
      </section>

      {/* Stage Roadmap Matrix */}
      <StageMatrix />

      {/* Developer Terminal Guide */}
      <section id="architecture" className="glass-card rounded-2xl p-6 border border-white/10">
        <div className="flex items-center gap-2 mb-4">
          <Terminal className="w-5 h-5 text-[#fc8019]" />
          <h3 className="text-lg font-bold text-white">Monorepo Developer Runbook</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs">
            <p className="text-slate-400 mb-2 font-sans font-semibold">1. Start Backend Microservices Tier</p>
            <p className="text-[#fc8019]"># Infrastructure (PostgreSQL, Redis, Kafka)</p>
            <p className="text-slate-300">cd infra/docker && docker compose up -d</p>
            <p className="text-[#fc8019] mt-2"># Run Microservices via Gradle</p>
            <p className="text-slate-300">./gradlew :services:user-service:bootRun</p>
            <p className="text-slate-300">./gradlew :services:catalog-service:bootRun</p>
            <p className="text-slate-300">./gradlew :services:order-service:bootRun</p>
            <p className="text-slate-300">./gradlew :services:dispatch-service:bootRun</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs">
            <p className="text-slate-400 mb-2 font-sans font-semibold">2. Start Frontend Turborepo Workspace</p>
            <p className="text-[#fc8019]"># Install workspace dependencies</p>
            <p className="text-slate-300">cd frontend && npm install</p>
            <p className="text-[#fc8019] mt-2"># Run Turborepo Dev Server (port :3001)</p>
            <p className="text-slate-300">npm run dev</p>
            <p className="text-[#fc8019] mt-2"># Type Check & Production Build</p>
            <p className="text-slate-300">npm run type-check && npm run build</p>
          </div>
        </div>
      </section>

      {/* Cart Drawer Overlay */}
      <CartDrawer onOrderPlaced={handleOrderPlaced} />
    </div>
  );
}
