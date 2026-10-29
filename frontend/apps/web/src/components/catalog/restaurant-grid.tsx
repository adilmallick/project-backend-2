"use client";

import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { QUERY_KEYS } from "@swiggy/config";
import { fetchRestaurants } from "@/lib/api-client";
import { useAuth } from "@/context/auth-context";
import { useCart } from "@/context/cart-context";
import { RestaurantResponse, MenuItemResponse } from "@swiggy/types";
import {
  Utensils,
  Search,
  Star,
  MapPin,
  Clock,
  Plus,
  ShoppingBag,
  X,
  Sparkles,
} from "lucide-react";

export function RestaurantGrid() {
  const { token } = useAuth();
  const { addItem, items } = useCart();
  const [searchTerm, setSearchTerm] = useState("");
  const [activeCuisine, setActiveCuisine] = useState("All");
  const [selectedRestaurant, setSelectedRestaurant] = useState<RestaurantResponse | null>(null);

  const { data: restaurants, isLoading } = useQuery({
    queryKey: QUERY_KEYS.RESTAURANTS,
    queryFn: () => fetchRestaurants(token || undefined),
    staleTime: 1000 * 60 * 15, // 15-min cache mirroring backend Redis TTL
  });

  const cuisines = ["All", "North Indian", "Biryani", "Italian", "South Indian"];

  const filteredRestaurants = (restaurants || []).filter((r) => {
    const matchesSearch =
      r.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.address.toLowerCase().includes(searchTerm.toLowerCase());

    if (activeCuisine === "All") return matchesSearch;
    if (activeCuisine === "North Indian")
      return matchesSearch && (r.name.includes("Punjabi") || r.description.includes("Indian"));
    if (activeCuisine === "Biryani")
      return matchesSearch && r.name.includes("Biryani");
    if (activeCuisine === "Italian")
      return matchesSearch && (r.name.includes("Pizza") || r.name.includes("Pasta"));
    if (activeCuisine === "South Indian")
      return matchesSearch && (r.name.includes("South") || r.name.includes("Tiffins"));
    return matchesSearch;
  });

  const formatPrice = (cents: number) => `₹${(cents / 100).toFixed(2)}`;

  const getItemCartQuantity = (menuItemId: string) => {
    const found = items.find((i) => i.menuItemId === menuItemId);
    return found ? found.quantity : 0;
  };

  return (
    <div id="catalog" className="space-y-6">
      {/* Header and Filter Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-2xl font-bold text-white">Explore Restaurant Catalog</h3>
            <span className="text-xs px-2 py-0.5 rounded-full bg-[#fc8019]/20 text-[#fc8019] border border-[#fc8019]/30 font-mono">
              Stage 1 Core Domain
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Optimized with single JPA @EntityGraph query & 15-min Redis TTL cache
          </p>
        </div>

        {/* Search Input */}
        <div className="relative w-full md:w-72">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search restaurants or dishes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-900 border border-slate-700/80 text-xs text-slate-200 placeholder-slate-400 focus:outline-none focus:border-[#fc8019] transition-all"
          />
        </div>
      </div>

      {/* Cuisine Filter Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2">
        {cuisines.map((c) => (
          <button
            key={c}
            onClick={() => setActiveCuisine(c)}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-medium transition-all whitespace-nowrap cursor-pointer ${
              activeCuisine === c
                ? "bg-[#fc8019] text-white shadow-md shadow-[#fc8019]/20 font-semibold"
                : "bg-slate-900/80 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-800"
            }`}
          >
            {c}
          </button>
        ))}
      </div>

      {/* Restaurant Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {isLoading
          ? Array.from({ length: 4 }).map((_, idx) => (
              <div
                key={idx}
                className="glass-card rounded-2xl p-5 border border-slate-800 animate-pulse h-64 flex flex-col justify-between"
              >
                <div className="h-6 bg-slate-800 rounded w-3/4 mb-3" />
                <div className="h-4 bg-slate-800 rounded w-full mb-2" />
                <div className="h-4 bg-slate-800 rounded w-2/3" />
                <div className="h-10 bg-slate-800 rounded mt-4" />
              </div>
            ))
          : filteredRestaurants.map((restaurant) => (
              <div
                key={restaurant.id}
                className="glass-card rounded-2xl p-5 border border-white/10 hover:border-[#fc8019]/50 transition-all flex flex-col justify-between group"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-slate-800 to-slate-700 flex items-center justify-center border border-white/10 group-hover:scale-105 transition-transform">
                      <Utensils className="w-6 h-6 text-[#fc8019]" />
                    </div>
                    <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                      <Star className="w-3 h-3 fill-amber-400" />
                      4.7
                    </span>
                  </div>

                  <h4 className="text-base font-bold text-white group-hover:text-[#fc8019] transition-colors leading-snug">
                    {restaurant.name}
                  </h4>

                  <p className="text-xs text-slate-300 mt-1 line-clamp-2 leading-relaxed">
                    {restaurant.description}
                  </p>

                  <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-1 text-xs text-slate-400">
                    <div className="flex items-center gap-1.5 truncate">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{restaurant.address}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>25-35 mins • {restaurant.menuItems.length} dishes</span>
                    </div>
                  </div>
                </div>

                <div className="mt-5">
                  <button
                    onClick={() => setSelectedRestaurant(restaurant)}
                    className="w-full py-2.5 rounded-xl bg-slate-800/80 hover:bg-[#fc8019] hover:text-white border border-slate-700 hover:border-[#fc8019] text-slate-200 font-semibold text-xs transition-all flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <span>View Menu Items</span>
                    <Sparkles className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
      </div>

      {/* Menu Modal Drawer */}
      {selectedRestaurant && (
        <div className="fixed inset-0 z-50 overflow-hidden bg-black/70 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="w-full max-w-2xl bg-[#0d131f] border border-white/10 rounded-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
            {/* Modal Header */}
            <div className="p-6 border-b border-white/10 flex items-start justify-between bg-gradient-to-r from-slate-900 to-[#0d131f]">
              <div>
                <span className="text-[11px] font-mono text-[#fc8019] font-bold uppercase tracking-wider">
                  Restaurant Menu
                </span>
                <h3 className="text-xl font-bold text-white mt-0.5">
                  {selectedRestaurant.name}
                </h3>
                <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-[#fc8019]" />
                  <span>{selectedRestaurant.address}</span>
                </p>
              </div>

              <button
                onClick={() => setSelectedRestaurant(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Menu Items List */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4 divide-y divide-slate-800/80">
              {selectedRestaurant.menuItems.map((item) => {
                const inCartQty = getItemCartQuantity(item.id);

                return (
                  <div
                    key={item.id}
                    className="pt-4 first:pt-0 flex items-start justify-between gap-4"
                  >
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="w-3 h-3 rounded-full border border-emerald-500 flex items-center justify-center">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                        </span>
                        <h4 className="text-sm font-bold text-white">{item.name}</h4>
                      </div>

                      <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                        {item.description}
                      </p>

                      <p className="text-sm font-bold text-[#fc8019] font-mono mt-2">
                        {formatPrice(item.priceCents)}
                      </p>
                    </div>

                    <div className="shrink-0 flex items-center">
                      <button
                        onClick={() =>
                          addItem(
                            { id: selectedRestaurant.id, name: selectedRestaurant.name },
                            item
                          )
                        }
                        className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-[#fc8019] text-white border border-slate-700 hover:border-[#fc8019] text-xs font-semibold transition-all flex items-center gap-1.5 shadow-sm cursor-pointer"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>{inCartQty > 0 ? `Add (${inCartQty})` : "Add to Cart"}</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-white/10 bg-slate-900/60 flex items-center justify-between text-xs">
              <span className="text-slate-400 font-mono">
                {selectedRestaurant.menuItems.length} items available
              </span>
              <button
                onClick={() => setSelectedRestaurant(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium transition-colors cursor-pointer"
              >
                Close Menu
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
