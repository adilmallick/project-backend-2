"use client";

import React from "react";
import { CheckCircle2, Clock, ArrowRight, ShieldCheck, Cpu, Database, Network, Radio, Gauge, Cloud } from "lucide-react";

interface StageItem {
  stage: string;
  name: string;
  status: "ACTIVE" | "PENDING";
  description: string;
  adr: string;
  icon: React.ReactNode;
  highlights: string[];
}

const STAGES: StageItem[] = [
  {
    stage: "Stage 0",
    name: "Turborepo & Next.js 15 Foundation",
    status: "ACTIVE",
    description: "Turborepo workspace layout, Next.js 15 App Router, TypeScript contracts (@swiggy/types), Tailwind CSS v4 & TanStack Query v5.",
    adr: "ADR-012",
    icon: <Cpu className="w-5 h-5 text-[#fc8019]" />,
    highlights: ["Turborepo Workspaces", "@swiggy/types synchronization", "TanStack Query v5 Client", "Actuator Health Probes"],
  },
  {
    stage: "Stage 1",
    name: "Core Domain, Auth & Catalog",
    status: "ACTIVE",
    description: "Stateless JWT Auth with RBAC, Restaurant Catalog & Menu Viewer, Cart Management, Order Placement with Idempotency Key.",
    adr: "ADR-013",
    icon: <ShieldCheck className="w-5 h-5 text-indigo-400" />,
    highlights: ["JWT Token Storage", "Role-Based Views", "Redis-Cached Menu Views", "Optimistic Cart & Checkout"],
  },
  {
    stage: "Stage 2",
    name: "Client Caching & Rate-Limit Handling",
    status: "PENDING",
    description: "TanStack Query 15-min TTL caching, cache invalidation on mutations, HTTP 429 backoff handling, and rider geospatial locator.",
    adr: "ADR-014",
    icon: <Database className="w-5 h-5 text-amber-400" />,
    highlights: ["Query Cache Invalidation", "429 Retry-After Handling", "Redis GEO Radius Search", "Nearby Rider Cards"],
  },
  {
    stage: "Stage 3",
    name: "Order Sagas & State Machine",
    status: "PENDING",
    description: "Interactive timeline matching the backend order state machine: ORDER_PLACED -> DELIVERED, and payment saga simulation.",
    adr: "ADR-015",
    icon: <Network className="w-5 h-5 text-rose-400" />,
    highlights: ["State Machine Timeline", "Optimistic UI Transitions", "Payment Decline Handling", "Saga Compensations"],
  },
  {
    stage: "Stage 4",
    name: "Live Tracking & STOMP WebSockets",
    status: "PENDING",
    description: "Real-time STOMP/SockJS client subscribing to /topic/orders/{orderId}/tracking, live rider GPS path & ETA countdown.",
    adr: "ADR-016",
    icon: <Radio className="w-5 h-5 text-cyan-400" />,
    highlights: ["STOMP /ws-tracking Client", "Redis Pub/Sub Subscriber", "Live Simulated Map View", "Sub-second ETA Updates"],
  },
  {
    stage: "Stage 5",
    name: "Production Hardening & Tracing",
    status: "PENDING",
    description: "Global error boundaries, toast notifications, OpenTelemetry W3C traceparent injection, and Actuator metrics visualization.",
    adr: "ADR-017",
    icon: <Gauge className="w-5 h-5 text-emerald-400" />,
    highlights: ["Distributed Trace Propagation", "React Error Boundaries", "Toast Alert System", "Prometheus Metrics View"],
  },
  {
    stage: "Stage 6",
    name: "Production Containerization & Cloud IaC",
    status: "PENDING",
    description: "Multi-stage Dockerfile with Next.js standalone output, Docker Compose integration with backend, and AWS ALB routing alignment.",
    adr: "ADR-018",
    icon: <Cloud className="w-5 h-5 text-blue-400" />,
    highlights: ["Standalone Next.js Output", "Alpine Non-root Container", "Docker Compose Multi-Tier", "ECS Fargate Readiness"],
  },
];

export function StageMatrix() {
  return (
    <div id="stages" className="mt-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h3 className="text-xl font-bold text-white">Frontend Architectural Stages</h3>
          <p className="text-xs text-slate-400 mt-1">
            Structured delivery roadmap mirroring all 7 backend stages
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-mono">
          <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#fc8019]/10 text-[#fc8019] border border-[#fc8019]/20 font-semibold">
            Stage 0: Live Active
          </span>
          <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
            Stages 1-6: Queued
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {STAGES.map((s) => (
          <div
            key={s.stage}
            className={`glass-card rounded-xl p-5 border flex flex-col justify-between transition-all ${
              s.status === "ACTIVE"
                ? "border-[#fc8019]/40 bg-[#fc8019]/5 shadow-lg shadow-[#fc8019]/5"
                : "border-white/5 opacity-85 hover:opacity-100"
            }`}
          >
            <div>
              <div className="flex items-center justify-between gap-2 mb-3">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-lg bg-slate-900 border border-slate-800">
                    {s.icon}
                  </div>
                  <div>
                    <span className="text-[11px] font-mono text-[#fc8019] font-bold block">
                      {s.stage}
                    </span>
                    <h4 className="text-sm font-semibold text-white leading-tight">
                      {s.name}
                    </h4>
                  </div>
                </div>

                {s.status === "ACTIVE" ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    <CheckCircle2 className="w-3 h-3" />
                    ACTIVE
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-800 text-slate-400 border border-slate-700">
                    <Clock className="w-3 h-3" />
                    NEXT
                  </span>
                )}
              </div>

              <p className="text-xs text-slate-300 leading-relaxed mb-4">
                {s.description}
              </p>

              <div className="space-y-1.5 mb-4">
                {s.highlights.map((h, i) => (
                  <div key={i} className="flex items-center gap-2 text-[11px] text-slate-400">
                    <div className="w-1 h-1 rounded-full bg-[#fc8019]" />
                    <span>{h}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs">
              <span className="font-mono text-slate-400 text-[11px]">{s.adr}</span>
              <span className="text-[#fc8019] flex items-center gap-1 text-[11px] font-medium">
                {s.status === "ACTIVE" ? "Configured" : "Queued"}
                <ArrowRight className="w-3 h-3" />
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
