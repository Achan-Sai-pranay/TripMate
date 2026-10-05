import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TripMate - AI Powered Travel Companion",
  description: "View and share curated smart travel itineraries, stays, and dining recommendations.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-slate-50 antialiased text-slate-900">
        <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-slate-200">
          <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="text-2xl font-black bg-gradient-to-r from-teal-600 to-emerald-500 bg-clip-text text-transparent">
                TripMate
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-teal-50 text-teal-700 border border-teal-200">
                Web Companion
              </span>
            </div>
          </div>
        </header>
        <main className="max-w-5xl mx-auto px-4 py-8">
          {children}
        </main>
      </body>
    </html>
  );
}
