import { TripPlanData } from "@/lib/types";

export const sampleTripPlan: TripPlanData = {
  destination: "Goa, India",
  dateRangeLabel: "Nov 14 - Nov 17 (4 Days)",
  travelerCount: 2,
  healthScore: 92,
  supabaseTripId: "sample",
  days: [
    {
      dayNumber: 1,
      dateLabel: "Day 1 - Coastal Warmup",
      items: [
        {
          time: "09:30 AM",
          title: "Aguada Fort & Lighthouse",
          durationLabel: "2h",
          costLabel: "\u20B950",
          whyThis: "17th-century Portuguese fortress overlooking Sinquerim beach with panoramic Arabian Sea views.",
          placeDetails: {
            rating: 4.6,
            reviewCount: 3820,
            openingHours: "9:30 AM - 5:30 PM",
          },
          travelToNext: {
            distanceLabel: "4.8 km",
            durationLabel: "15 mins",
            transportMode: "Cab/Scooter",
          },
        },
        {
          time: "01:00 PM",
          title: "Lunch at Fisherman's Wharf",
          durationLabel: "1.5h",
          costLabel: "\u20B91,200",
          whyThis: "Celebrated riverside seafood restaurant serving traditional Goan curry and kingfish rava fry.",
          placeDetails: {
            rating: 4.5,
            reviewCount: 4120,
            openingHours: "12:00 PM - 11:00 PM",
          },
          travelToNext: {
            distanceLabel: "7.2 km",
            durationLabel: "20 mins",
            transportMode: "Drive",
          },
        },
        {
          time: "04:30 PM",
          title: "Anjuna Beach Sunset Walk",
          durationLabel: "2.5h",
          costLabel: "Free",
          whyThis: "Vibrant rocky beach known for red laterite cliffs, trance roots, and sunset shacks.",
          placeDetails: {
            rating: 4.4,
            reviewCount: 9400,
            openingHours: "Open 24 hours",
          },
        },
      ],
    },
    {
      dayNumber: 2,
      dateLabel: "Day 2 - Heritage & Culture",
      items: [
        {
          time: "10:00 AM",
          title: "Basilica of Bom Jesus",
          durationLabel: "1.5h",
          costLabel: "Free",
          whyThis: "UNESCO World Heritage site holding the sacred relics of St. Francis Xavier.",
          placeDetails: {
            rating: 4.7,
            reviewCount: 15400,
            openingHours: "9:00 AM - 6:30 PM",
          },
          travelToNext: {
            distanceLabel: "1.2 km",
            durationLabel: "5 mins",
            transportMode: "Walk",
          },
        },
        {
          time: "12:30 PM",
          title: "Fontainhas Latin Quarter Stroll",
          durationLabel: "2h",
          costLabel: "Free",
          whyThis: "Colorful Portuguese-colonial streets, heritage balconies, and quaint bakeries in Panjim.",
          placeDetails: {
            rating: 4.6,
            reviewCount: 5200,
            openingHours: "Open 24 hours",
          },
        },
      ],
    },
  ],
  stays: [
    {
      name: "The Hosteller Goa",
      tier: "Budget",
      pricePerNight: "\u20B91,100/night",
      location: "Anjuna",
      rating: 4.5,
      whyRecommended: "Vibrant community, co-working space, and close walk to the flea market.",
    },
    {
      name: "Heritage Village Resort & Spa",
      tier: "Mid-range",
      pricePerNight: "\u20B95,400/night",
      location: "Arossim Beach",
      rating: 4.6,
      whyRecommended: "Traditional Portuguese architecture with manicured lawns and private beach pathway.",
    },
    {
      name: "Taj Exotica Resort & Spa",
      tier: "Luxury",
      pricePerNight: "\u20B918,500/night",
      location: "Benaulim",
      rating: 4.9,
      whyRecommended: "56 acres of Mediterranean-style landscaped beachfront sanctuary with world-class dining.",
    },
  ],
  dining: [
    {
      name: "Vinayak Family Restaurant",
      cuisine: "Goan Seafood",
      priceRange: "\u20B9\u20B9",
      famousFor: "Fish Thali with Crab Xec Xec",
      rating: 4.7,
    },
    {
      name: "Gunpowder",
      cuisine: "South Indian Coastal",
      priceRange: "\u20B9\u20B9\u20B9",
      famousFor: "Kerala Mutton Curry & Appams",
      rating: 4.8,
    },
    {
      name: "Mum's Kitchen",
      cuisine: "Traditional Goan Heritage",
      priceRange: "\u20B9\u20B9\u20B9",
      famousFor: "Pork Vindaloo & Alle Belle",
      rating: 4.6,
    },
  ],
};
