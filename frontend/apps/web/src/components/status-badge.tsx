import React from "react";

interface StatusBadgeProps {
  status: "UP" | "DOWN" | "DEGRADED" | "PENDING";
  label?: string;
  showDot?: boolean;
}

export function StatusBadge({ status, label, showDot = true }: StatusBadgeProps) {
  const displayLabel = label || status;

  const colorStyles = {
    UP: "bg-emerald-500/10 text-emerald-400 border-emerald-500/30",
    DOWN: "bg-rose-500/10 text-rose-400 border-rose-500/30",
    DEGRADED: "bg-amber-500/10 text-amber-400 border-amber-500/30",
    PENDING: "bg-slate-500/10 text-slate-400 border-slate-500/30",
  }[status];

  const dotStyles = {
    UP: "bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)] animate-live-pulse",
    DOWN: "bg-rose-400 shadow-[0_0_8px_rgba(251,113,133,0.8)]",
    DEGRADED: "bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.8)]",
    PENDING: "bg-slate-400",
  }[status];

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${colorStyles}`}
    >
      {showDot && <span className={`w-1.5 h-1.5 rounded-full ${dotStyles}`} />}
      {displayLabel}
    </span>
  );
}
