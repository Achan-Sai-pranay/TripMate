export interface TripPlanData {
  destination: string;
  dateRangeLabel: string;
  travelerCount: number;
  healthScore: number;
  supabaseTripId?: string;
  days: {
    dayNumber: number;
    dateLabel: string;
    items: {
      time: string;
      title: string;
      durationLabel: string;
      costLabel: string;
      whyThis: string;
      imageUrl?: string;
      placeDetails?: {
        rating: number;
        reviewCount: number;
        openingHours: string;
      };
      travelToNext?: {
        distanceLabel: string;
        durationLabel: string;
        transportMode: string;
      };
    }[];
  }[];
  stays?: {
    name: string;
    tier: string;
    pricePerNight: string;
    location: string;
    rating: number;
    whyRecommended: string;
  }[];
  dining?: {
    name: string;
    cuisine: string;
    priceRange: string;
    famousFor: string;
    rating: number;
  }[];
}
