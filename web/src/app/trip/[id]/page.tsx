import { notFound } from "next/navigation";
import { supabase } from "@/lib/supabase";
import { sampleTripPlan } from "@/lib/sampleTrip";
import { TripPlanData } from "@/lib/types";
import { TripViewer } from "@/components/TripViewer";

interface TripPageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ data?: string }>;
}

export default async function TripPage({ params, searchParams }: TripPageProps) {
  const { id } = await params;
  const { data: encodedData } = await searchParams;

  // 1. If encoded data passed directly in query param
  if (encodedData) {
    try {
      const decoded = JSON.parse(decodeURIComponent(encodedData)) as TripPlanData;
      return <TripViewer trip={decoded} />;
    } catch {
      // Fall through
    }
  }

  // 2. Sample trip preview
  if (id === "sample") {
    return <TripViewer trip={sampleTripPlan} />;
  }

  // 3. Look up from Supabase trips table
  try {
    const { data: tripRow, error } = await supabase
      .from("trips")
      .select("*")
      .eq("id", id)
      .single();

    if (error || !tripRow) {
      // Fallback: render sample trip with destination if not found
      return (
        <div className="space-y-6">
          <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            Showing sample preview for shared trip ID <strong>{id}</strong>.
          </div>
          <TripViewer trip={{ ...sampleTripPlan, destination: tripRow?.name || "Shared Adventure", supabaseTripId: id }} />
        </div>
      );
    }

    const tripData: TripPlanData = {
      destination: tripRow.name || tripRow.destination || "Trip Itinerary",
      dateRangeLabel: tripRow.start_date && tripRow.end_date ? `${tripRow.start_date} to ${tripRow.end_date}` : "Upcoming Trip",
      travelerCount: 2,
      healthScore: 88,
      supabaseTripId: tripRow.id,
      days: sampleTripPlan.days,
      stays: sampleTripPlan.stays,
      dining: sampleTripPlan.dining,
    };

    return <TripViewer trip={tripData} />;
  } catch {
    return notFound();
  }
}
