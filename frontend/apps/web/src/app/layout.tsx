import type { Metadata } from "next";
import "./globals.css";
import { QueryProvider } from "@/providers/query-provider";
import { AuthProvider } from "@/context/auth-context";
import { CartProvider } from "@/context/cart-context";
import { Navbar } from "@/components/navbar";

export const metadata: Metadata = {
  title: "Swiggy Rush | Distributed Food Delivery Platform",
  description:
    "Production-grade microservices food delivery frontend powered by Turborepo, Next.js 15, TypeScript, Tailwind CSS v4, and TanStack Query.",
  keywords: [
    "Swiggy",
    "Zomato",
    "Food Delivery",
    "Microservices",
    "Next.js 15",
    "Turborepo",
    "TanStack Query",
    "Spring Boot",
  ],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="bg-[#090d16] text-slate-100 min-h-screen antialiased flex flex-col">
        <QueryProvider>
          <AuthProvider>
            <CartProvider>
              <Navbar />
              <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {children}
              </main>
              <footer className="glass-panel border-t border-white/5 py-6 text-center text-xs text-slate-400">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
                  <p>
                    Swiggy / Zomato Mini Delivery Platform • Turborepo & Next.js 15
                  </p>
                  <p className="font-mono text-slate-400">
                    Connected to Java 21 Virtual Threads & Spring Boot 3.4 Microservices
                  </p>
                </div>
              </footer>
            </CartProvider>
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
