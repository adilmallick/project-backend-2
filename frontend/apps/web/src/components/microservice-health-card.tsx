"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@swiggy/config";
import { checkAllServices } from "@/lib/api-client";
import { StatusBadge } from "@/components/status-badge";
import { RefreshCw, Server, Cpu, Database, Zap, ExternalLink } from "lucide-react";

export function MicroserviceHealthCard() {
  const {
    data: services,
    isLoading,
    isFetching,
    refetch,
    dataUpdatedAt,
  } = useQuery({
    queryKey: QUERY_KEYS.HEALTH,
    queryFn: checkAllServices,
    refetchInterval: 15000, // Automatic background polling every 15s
  });

  const getServiceIcon = (name: string) => {
    if (name.includes("User")) return <Cpu className="w-5 h-5 text-indigo-400" />;
    if (name.includes("Catalog")) return <Database className="w-5 h-5 text-amber-400" />;
    if (name.includes("Order")) return <Zap className="w-5 h-5 text-rose-400" />;
    return <Server className="w-5 h-5 text-emerald-400" />;
  };

  return (
    <div className="glass-card rounded-2xl p-6 border border-white/10 relative overflow-hidden">
      {/* Background glow accent */}
      <div className="absolute -top-24 -right-24 w-60 h-60 bg-[#fc8019]/10 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2.5">
            <h3 className="text-xl font-bold text-white">Microservice Ingress & Health</h3>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
              TanStack Query v5
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Real-time actuator probes across all 4 Spring Boot microservices
          </p>
        </div>

        <div className="flex items-center gap-3">
          {dataUpdatedAt > 0 && (
            <span className="text-[11px] text-slate-400 font-mono">
              Last probe: {new Date(dataUpdatedAt).toLocaleTimeString()}
            </span>
          )}
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 border border-slate-700/80 text-xs font-medium text-slate-200 transition-all cursor-pointer disabled:opacity-50"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 text-[#fc8019] ${isFetching ? "animate-spin" : ""}`}
            />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Services Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {isLoading
          ? Array.from({ length: 4 }).map((_, idx) => (
              <div
                key={idx}
                className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 animate-pulse h-32 flex flex-col justify-between"
              >
                <div className="h-4 bg-slate-800 rounded w-2/3" />
                <div className="h-3 bg-slate-800 rounded w-1/3" />
                <div className="h-6 bg-slate-800 rounded w-1/2" />
              </div>
            ))
          : services?.map((svc) => (
              <div
                key={svc.port}
                className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:border-slate-700 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="p-2 rounded-lg bg-slate-800/80 border border-slate-700/50">
                      {getServiceIcon(svc.name)}
                    </div>
                    <StatusBadge status={svc.status} />
                  </div>
                  <h4 className="text-sm font-semibold text-white leading-tight">
                    {svc.name.replace(" Service", "")}
                  </h4>
                  <p className="text-xs text-slate-400 font-mono mt-0.5">Port :{svc.port}</p>
                </div>

                <div className="pt-3 mt-3 border-t border-slate-800/60 flex items-center justify-between text-xs">
                  <span className="text-slate-400 font-mono">
                    {svc.responseTimeMs !== undefined ? `${svc.responseTimeMs}ms latency` : "—"}
                  </span>
                  <a
                    href={`${svc.url}/swagger-ui.html`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 text-[#fc8019] hover:underline"
                  >
                    <span>Swagger</span>
                    <ExternalLink className="w-3 h-3" />
                  </a>
                </div>
              </div>
            ))}
      </div>
    </div>
  );
}
