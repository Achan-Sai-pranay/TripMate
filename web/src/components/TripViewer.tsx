"use client";

import { useState } from "react";
import { TripPlanData } from "@/lib/types";
import { 
  Calendar, 
  Users, 
  Heart, 
  MapPin, 
  Clock, 
  Star, 
  Utensils, 
  Hotel, 
  Share2, 
  ExternalLink,
  Navigation
} from "lucide-react";

export function TripViewer({ trip }: { trip: TripPlanData }) {
  const [selectedDayIndex, setSelectedDayIndex] = useState(0);
  const [activeTab, setActiveTab] = useState<"itinerary" | "stays" | "dining">("itinerary");
  const [copied, setCopied] = useState(false);

  const currentDay = trip.days[selectedDayIndex] || trip.days[0];

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="space-y-8">
      {/* Hero Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-teal-950 to-slate-900 text-white p-8 sm:p-10 shadow-xl">
        <div className="relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-500/20 text-teal-300 border border-teal-500/30 text-xs font-semibold uppercase tracking-wider mb-4">
              <MapPin className="w-3.5 h-3.5" />
              Trip Itinerary
            </div>
            <h1 className="text-3xl sm:text-5xl font-black tracking-tight">{trip.destination}</h1>
            <div className="mt-4 flex flex-wrap items-center gap-4 text-sm text-slate-300">
              <span className="flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-teal-400" />
                {trip.dateRangeLabel}
              </span>
              <span className="flex items-center gap-1.5">
                <Users className="w-4 h-4 text-teal-400" />
                {trip.travelerCount} Travelers
              </span>
              <span className="flex items-center gap-1.5 font-medium text-emerald-400">
                <Heart className="w-4 h-4" />
                Trip Health: {trip.healthScore}/100
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleShare}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 backdrop-blur-md transition text-sm font-semibold border border-white/10"
            >
              <Share2 className="w-4 h-4" />
              {copied ? "Link Copied!" : "Share Link"}
            </button>
            <a
              href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(trip.destination)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-teal-500 hover:bg-teal-600 text-slate-950 font-semibold transition text-sm"
            >
              <ExternalLink className="w-4 h-4" />
              Google Maps
            </a>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => setActiveTab("itinerary")}
          className={`px-6 py-3 font-semibold text-sm border-b-2 transition ${
            activeTab === "itinerary"
              ? "border-teal-600 text-teal-700"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          Daily Itinerary
        </button>
        <button
          onClick={() => setActiveTab("stays")}
          className={`px-6 py-3 font-semibold text-sm border-b-2 transition flex items-center gap-1.5 ${
            activeTab === "stays"
              ? "border-teal-600 text-teal-700"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Hotel className="w-4 h-4" />
          Stays ({trip.stays?.length || 0})
        </button>
        <button
          onClick={() => setActiveTab("dining")}
          className={`px-6 py-3 font-semibold text-sm border-b-2 transition flex items-center gap-1.5 ${
            activeTab === "dining"
              ? "border-teal-600 text-teal-700"
              : "border-transparent text-slate-500 hover:text-slate-800"
          }`}
        >
          <Utensils className="w-4 h-4" />
          Dining ({trip.dining?.length || 0})
        </button>
      </div>

      {/* Tab: Itinerary */}
      {activeTab === "itinerary" && (
        <div className="space-y-6">
          {/* Day Selector */}
          <div className="flex items-center gap-2 overflow-x-auto pb-2">
            {trip.days.map((day, idx) => (
              <button
                key={day.dayNumber}
                onClick={() => setSelectedDayIndex(idx)}
                className={`px-4 py-2 rounded-xl text-sm font-semibold transition shrink-0 ${
                  selectedDayIndex === idx
                    ? "bg-slate-900 text-white shadow-sm"
                    : "bg-white text-slate-600 hover:bg-slate-100 border border-slate-200"
                }`}
              >
                Day {day.dayNumber}
              </button>
            ))}
          </div>

          <div className="p-4 rounded-2xl bg-white border border-slate-200">
            <h2 className="text-xl font-bold text-slate-900">{currentDay.dateLabel}</h2>
            <p className="text-sm text-slate-500 mt-1">{currentDay.items.length} activities scheduled</p>
          </div>

          {/* Activities Timeline */}
          <div className="space-y-4">
            {currentDay.items.map((item, idx) => (
              <div key={idx} className="relative">
                <div className="p-5 rounded-2xl bg-white border border-slate-200 hover:border-slate-300 transition shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-full bg-teal-50 text-teal-700 flex items-center justify-center font-bold text-sm shrink-0 border border-teal-100">
                      {idx + 1}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs font-semibold text-teal-600 flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5" />
                          {item.time} ({item.durationLabel})
                        </span>
                        <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
                          {item.costLabel}
                        </span>
                      </div>
                      <h3 className="text-lg font-bold text-slate-900 mt-1">{item.title}</h3>
                      <p className="text-sm text-slate-600 mt-1 max-w-2xl">{item.whyThis}</p>

                      {item.placeDetails && (
                        <div className="mt-3 flex items-center gap-4 text-xs text-slate-500 flex-wrap">
                          <span className="flex items-center gap-1 font-semibold text-slate-800">
                            <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                            {item.placeDetails.rating.toFixed(1)}
                            <span className="text-slate-400 font-normal">
                              ({item.placeDetails.reviewCount} reviews)
                            </span>
                          </span>
                          <span>•</span>
                          <span>{item.placeDetails.openingHours}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <a
                    href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(item.title + ", " + trip.destination)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="self-end md:self-center inline-flex items-center gap-1 text-xs font-semibold text-teal-600 hover:text-teal-700 bg-teal-50 px-3 py-1.5 rounded-lg shrink-0"
                  >
                    View Map <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>

                {/* Connecting travel indicator */}
                {item.travelToNext && idx < currentDay.items.length - 1 && (
                  <div className="pl-9 py-2 flex items-center gap-3 text-xs text-slate-500">
                    <div className="w-0.5 h-6 bg-slate-200" />
                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-slate-100 border border-slate-200">
                      <Navigation className="w-3 h-3 text-slate-400" />
                      {item.travelToNext.distanceLabel} • {item.travelToNext.durationLabel} ({item.travelToNext.transportMode})
                    </span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab: Stays */}
      {activeTab === "stays" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {trip.stays?.map((stay, idx) => (
            <div key={idx} className="p-6 rounded-2xl bg-white border border-slate-200 flex flex-col justify-between shadow-xs">
              <div>
                <span className={`inline-block text-xs font-bold px-2.5 py-1 rounded-full uppercase tracking-wider ${
                  stay.tier.toLowerCase() === "budget"
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                    : stay.tier.toLowerCase() === "luxury"
                    ? "bg-purple-50 text-purple-700 border border-purple-200"
                    : "bg-teal-50 text-teal-700 border border-teal-200"
                }`}>
                  {stay.tier}
                </span>
                <h3 className="text-xl font-bold text-slate-900 mt-3">{stay.name}</h3>
                <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-slate-400" />
                  {stay.location}
                </p>
                <p className="text-sm text-slate-600 mt-3">{stay.whyRecommended}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <div>
                  <div className="text-base font-bold text-teal-700">{stay.pricePerNight}</div>
                  <div className="flex items-center gap-1 text-xs text-slate-600 mt-0.5">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    <span className="font-semibold">{stay.rating.toFixed(1)}</span>
                  </div>
                </div>
                <a
                  href={`https://www.google.com/search?q=${encodeURIComponent(stay.name + " " + stay.location + " booking")}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition"
                >
                  Check Stay
                </a>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tab: Dining */}
      {activeTab === "dining" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {trip.dining?.map((dish, idx) => (
            <div key={idx} className="p-6 rounded-2xl bg-white border border-slate-200 flex flex-col justify-between shadow-xs">
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                    {dish.cuisine}
                  </span>
                  <span className="text-xs font-medium text-slate-400">
                    {dish.priceRange}
                  </span>
                </div>
                <h3 className="text-xl font-bold text-slate-900 mt-3">{dish.name}</h3>
                <div className="mt-2 text-xs font-medium text-teal-700 bg-teal-50 px-2 py-1 rounded inline-block">
                  Must try: {dish.famousFor}
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-1 text-xs text-slate-600">
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                  <span className="font-semibold">{dish.rating.toFixed(1)}</span>
                </div>
                <a
                  href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dish.name + ", " + trip.destination)}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-3 py-1.5 rounded-lg bg-teal-600 text-white text-xs font-semibold hover:bg-teal-700 transition"
                >
                  Locate
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
