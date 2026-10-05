package com.example.tripmate.data

import com.example.tripmate.model.DiningOption
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.PlaceDetails
import com.example.tripmate.model.StayOption
import com.example.tripmate.model.TravelLeg
import com.example.tripmate.model.TripPlan
import com.example.tripmate.util.ActivityIconMapper
import org.json.JSONArray
import org.json.JSONObject

object TripPlanJson {

    fun toJson(plan: TripPlan): JSONObject {
        val daysArray = JSONArray()
        plan.days.forEach { day ->
            val itemsArray = JSONArray()
            day.items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("time", item.time)
                    put("title", item.title)
                    put("durationLabel", item.durationLabel)
                    put("costLabel", item.costLabel)
                    put("whyThis", item.whyThis)
                    put("imageUrl", item.imageUrl ?: JSONObject.NULL)
                    put("isFixed", item.isFixed)
                }

                item.placeDetails?.let { place ->
                    itemObj.put("placeDetails", JSONObject().apply {
                        put("rating", place.rating)
                        put("reviewCount", place.reviewCount)
                        put("openingHours", place.openingHours)
                        place.latitude?.let { put("latitude", it) }
                        place.longitude?.let { put("longitude", it) }
                    })
                }

                item.travelToNext?.let { leg ->
                    itemObj.put("travelToNext", JSONObject().apply {
                        put("distanceLabel", leg.distanceLabel)
                        put("durationLabel", leg.durationLabel)
                        put("transportMode", leg.transportMode)
                    })
                }

                itemsArray.put(itemObj)
            }
            daysArray.put(
                JSONObject().apply {
                    put("dayNumber", day.dayNumber)
                    put("dateLabel", day.dateLabel)
                    put("items", itemsArray)
                }
            )
        }

        val staysArray = JSONArray()
        plan.stays.forEach { stay ->
            staysArray.put(JSONObject().apply {
                put("name", stay.name)
                put("tier", stay.tier)
                put("pricePerNight", stay.pricePerNight)
                put("location", stay.location)
                put("rating", stay.rating)
                put("whyRecommended", stay.whyRecommended)
            })
        }

        val diningArray = JSONArray()
        plan.dining.forEach { dining ->
            diningArray.put(JSONObject().apply {
                put("name", dining.name)
                put("cuisine", dining.cuisine)
                put("priceRange", dining.priceRange)
                put("famousFor", dining.famousFor)
                put("rating", dining.rating)
            })
        }

        return JSONObject().apply {
            put("destination", plan.destination)
            put("dateRangeLabel", plan.dateRangeLabel)
            put("travelerCount", plan.travelerCount)
            put("healthScore", plan.healthScore)
            put("budget", plan.budget)
            put("days", daysArray)
            put("stays", staysArray)
            put("dining", diningArray)
            put("supabaseTripId", plan.supabaseTripId ?: JSONObject.NULL)
        }
    }

    fun fromJson(root: JSONObject): TripPlan {
        val daysArray = root.getJSONArray("days")
        val days = (0 until daysArray.length()).map { i ->
            val dayObj = daysArray.getJSONObject(i)
            val itemsArray = dayObj.getJSONArray("items")
            val items = (0 until itemsArray.length()).map { j ->
                val itemObj = itemsArray.getJSONObject(j)
                val title = itemObj.getString("title")

                val placeDetails = itemObj.optJSONObject("placeDetails")?.let { p ->
                    PlaceDetails(
                        rating = p.optDouble("rating", 4.5),
                        reviewCount = p.optInt("reviewCount", 1200),
                        openingHours = p.optString("openingHours", "9:00 AM - 6:00 PM"),
                        latitude = if (p.has("latitude")) p.getDouble("latitude") else null,
                        longitude = if (p.has("longitude")) p.getDouble("longitude") else null
                    )
                }

                val travelLeg = itemObj.optJSONObject("travelToNext")?.let { t ->
                    TravelLeg(
                        distanceLabel = t.optString("distanceLabel", "2.0 km"),
                        durationLabel = t.optString("durationLabel", "10 mins"),
                        transportMode = t.optString("transportMode", "Drive")
                    )
                }

                ItineraryItem(
                    time = itemObj.getString("time"),
                    title = title,
                    durationLabel = itemObj.getString("durationLabel"),
                    costLabel = itemObj.getString("costLabel"),
                    whyThis = itemObj.getString("whyThis"),
                    icon = ActivityIconMapper.iconFor(title),
                    imageUrl = itemObj.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
                    isFixed = itemObj.optBoolean("isFixed", false),
                    placeDetails = placeDetails,
                    travelToNext = travelLeg
                )
            }
            ItineraryDay(
                dayNumber = dayObj.getInt("dayNumber"),
                dateLabel = dayObj.getString("dateLabel"),
                items = items
            )
        }

        val stays = root.optJSONArray("stays")?.let { sArray ->
            (0 until sArray.length()).map { idx ->
                val s = sArray.getJSONObject(idx)
                StayOption(
                    name = s.getString("name"),
                    tier = s.optString("tier", "Mid-range"),
                    pricePerNight = s.optString("pricePerNight", "₹2,500/night"),
                    location = s.optString("location", "City Center"),
                    rating = s.optDouble("rating", 4.4),
                    whyRecommended = s.optString("whyRecommended", "")
                )
            }
        } ?: emptyList()

        val dining = root.optJSONArray("dining")?.let { dArray ->
            (0 until dArray.length()).map { idx ->
                val d = dArray.getJSONObject(idx)
                DiningOption(
                    name = d.getString("name"),
                    cuisine = d.optString("cuisine", "Local Special"),
                    priceRange = d.optString("priceRange", "₹₹"),
                    famousFor = d.optString("famousFor", "Signature dish"),
                    rating = d.optDouble("rating", 4.5)
                )
            }
        } ?: emptyList()

        return TripPlan(
            destination = root.getString("destination"),
            dateRangeLabel = root.getString("dateRangeLabel"),
            travelerCount = root.getInt("travelerCount"),
            healthScore = root.getInt("healthScore"),
            budget = root.optInt("budget", 0),
            days = days,
            stays = stays,
            dining = dining,
            supabaseTripId = root.optString("supabaseTripId").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
