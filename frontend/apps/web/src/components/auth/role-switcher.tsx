"use client";

import React from "react";
import { UserRole } from "@swiggy/types";
import { useAuth, DEMO_USERS } from "@/context/auth-context";
import { User, Shield, Bike, Store } from "lucide-react";

export function RoleSwitcher() {
  const { user, role, switchDemoRole } = useAuth();

  const getRoleIcon = (r: UserRole) => {
    switch (r) {
      case "CUSTOMER":
        return <User className="w-3.5 h-3.5 text-emerald-400" />;
      case "RESTAURANT_OWNER":
        return <Store className="w-3.5 h-3.5 text-amber-400" />;
      case "RIDER":
        return <Bike className="w-3.5 h-3.5 text-cyan-400" />;
      case "ADMIN":
        return <Shield className="w-3.5 h-3.5 text-rose-400" />;
    }
  };

  const roles: UserRole[] = ["CUSTOMER", "RESTAURANT_OWNER", "RIDER", "ADMIN"];

  return (
    <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800">
      <div className="flex items-center gap-1.5 mr-2 pr-2 border-r border-slate-800">
        {role && getRoleIcon(role)}
        <span className="text-xs font-semibold text-slate-200">
          {user?.name || "Guest"}
        </span>
      </div>

      <div className="flex items-center gap-1">
        {roles.map((r) => {
          const isCurrent = role === r;
          return (
            <button
              key={r}
              onClick={() => switchDemoRole(r)}
              className={`px-2 py-0.5 rounded-md text-[11px] font-medium transition-all cursor-pointer ${
                isCurrent
                  ? "bg-[#fc8019] text-white font-bold shadow-sm shadow-[#fc8019]/40"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800"
              }`}
              title={`Switch session to ${DEMO_USERS[r].user.name} (${r})`}
            >
              {r.replace("_", " ")}
            </button>
          );
        })}
      </div>
    </div>
  );
}
