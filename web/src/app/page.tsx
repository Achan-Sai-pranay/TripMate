import Link from "next/link";
import { Compass, MapPin, Calendar, Users, Sparkles } from "lucide-react";

export default function HomePage() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="w-16 h-16 rounded-2xl bg-teal-600 text-white flex items-center justify-center shadow-lg shadow-teal-500/20 mb-6">
        <Compass className="w-9 h-9" />
      </div>
      <h1 className="text-4xl sm:text-5xl font-black text-slate-900 tracking-tight max-w-xl">
        Share and Explore Travel Itineraries
      </h1>
      <p className="mt-4 text-lg text-slate-600 max-w-lg">
        View AI-crafted travel plans generated on the TripMate Android app with interactive itineraries, stays, dining, and maps.
      </p>

      <div className="mt-8 flex flex-col sm:flex-row gap-4">
        <Link
          href="/trip/sample"
          className="inline-flex items-center justify-center px-6 py-3 rounded-xl bg-teal-600 text-white font-semibold hover:bg-teal-700 transition shadow-sm"
        >
          <Sparkles className="w-5 h-5 mr-2" />
          View Sample Itinerary
        </Link>
      </div>

      <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl w-full text-left">
        <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs">
          <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-600 flex items-center justify-center mb-4">
            <MapPin className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-slate-900">Curated Stops</h3>
          <p className="mt-2 text-sm text-slate-600">
            Ratings, review counts, opening hours, and travel time between stops.
          </p>
        </div>
        <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs">
          <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4">
            <Calendar className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-slate-900">Budgeted Stays</h3>
          <p className="mt-2 text-sm text-slate-600">
            Categorized accommodations across Budget, Mid-range, and Luxury tiers.
          </p>
        </div>
        <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs">
          <div className="w-10 h-10 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center mb-4">
            <Users className="w-5 h-5" />
          </div>
          <h3 className="font-bold text-slate-900">Culinary Hotspots</h3>
          <p className="mt-2 text-sm text-slate-600">
            Handpicked local dining recommendations with specialty dishes and cuisine.
          </p>
        </div>
      </div>
    </div>
  );
}
