"use client";

import React from "react";
import Link from "next/link";
import { Utensils, Activity, Layers, ShoppingBag, UtensilsCrossed } from "lucide-react";
import { RoleSwitcher } from "@/components/auth/role-switcher";
import { useCart } from "@/context/cart-context";

export function Navbar() {
  const { itemCount, setIsCartOpen } = useCart();

  return (
    <header className="sticky top-0 z-40 glass-panel border-b border-white/5 bg-[#090d16]/85 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-3 group shrink-0">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#fc8019] to-[#e23744] flex items-center justify-center shadow-lg shadow-[#fc8019]/20 group-hover:scale-105 transition-transform">
            <Utensils className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
              Swiggy Rush
            </span>
            <span className="ml-2 text-xs font-semibold px-1.5 py-0.5 rounded bg-[#fc8019]/20 text-[#fc8019] border border-[#fc8019]/30">
              Turbo Monorepo
            </span>
            <p className="text-[10px] text-slate-400 tracking-wider uppercase font-mono">
              Distributed Food Delivery
            </p>
          </div>
        </Link>

        {/* Navigation items */}
        <nav className="hidden lg:flex items-center gap-6 text-xs font-medium">
          <a
            href="#catalog"
            className="flex items-center gap-1.5 text-slate-300 hover:text-[#fc8019] transition-colors"
          >
            <UtensilsCrossed className="w-4 h-4 text-[#fc8019]" />
            Restaurant Catalog
          </a>
          <a
            href="#services"
            className="flex items-center gap-1.5 text-slate-300 hover:text-[#fc8019] transition-colors"
          >
            <Activity className="w-4 h-4 text-emerald-400" />
            Services Health
          </a>
          <a
            href="#stages"
            className="flex items-center gap-1.5 text-slate-300 hover:text-[#fc8019] transition-colors"
          >
            <Layers className="w-4 h-4 text-blue-400" />
            Stage Roadmap
          </a>
        </nav>

        {/* Actions & Role Switcher */}
        <div className="flex items-center gap-3">
          <div className="hidden sm:block">
            <RoleSwitcher />
          </div>

          {/* Cart Button */}
          <button
            onClick={() => setIsCartOpen(true)}
            className="relative p-2.5 rounded-xl bg-slate-900 border border-slate-700 hover:border-[#fc8019] text-slate-200 hover:text-white transition-all cursor-pointer group"
            title="Open Shopping Cart"
          >
            <ShoppingBag className="w-5 h-5 text-[#fc8019] group-hover:scale-105 transition-transform" />
            {itemCount > 0 && (
              <span className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-gradient-to-r from-[#fc8019] to-[#e23744] text-white text-[11px] font-bold flex items-center justify-center shadow-md shadow-[#fc8019]/50 animate-bounce">
                {itemCount}
              </span>
            )}
          </button>
        </div>
      </div>
    </header>
  );
}
