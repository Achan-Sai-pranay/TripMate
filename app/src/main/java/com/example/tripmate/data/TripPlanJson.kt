package com.example.tripmate.data

import com.example.tripmate.model.BudgetEntry
import com.example.tripmate.model.DiningOption
import com.example.tripmate.model.ExpenseCategory
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.PlaceDetails
import com.example.tripmate.model.StayOption
import com.example.tripmate.model.TravelLeg
import com.example.tripmate.model.TripPlan
import com.example.tripmate.util.ActivityIconMapper
import com.example.tripmate.model.TripDocument
import com.example.tripmate.model.DocumentCategory
import com.example.tripmate.model.DocumentFileType
import com.example.tripmate.util.CostParser
import org.json.JSONArray
import org.json.JSONObject

object TripPlanJson {

    fun toJson(plan: TripPlan): JSONObject {
        val daysArray = JSONArray()
        plan.days.forEach { day ->
            val itemsArray = JSONArray()
            day.items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("id", item.id)
                    put("time", item.time)
                    put("title", item.title)
                    put("durationLabel", item.durationLabel)
                    put("costLabel", item.costLabel)
                    put("costAmount", item.costAmount)
                    put("category", item.category.name)
                    put("whyThis", item.whyThis)
                    put("imageUrl", item.imageUrl ?: JSONObject.NULL)
                    put("isFixed", item.isFixed)
                    item.placeName?.let { put("placeName", it) }
                    item.wikipediaTitle?.let { put("wikipediaTitle", it) }
                    put("votes", JSONObject().apply {
                        put("upvotes", item.votes.upvotes)
                        put("downvotes", item.votes.downvotes)
                        put("userVote", item.votes.userVote ?: JSONObject.NULL)
                    })
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

        val customExpensesArray = JSONArray()
        plan.customExpenses.forEach { exp ->
            customExpensesArray.put(JSONObject().apply {
                put("id", exp.id)
                put("title", exp.title)
                put("amount", exp.amount)
                put("category", exp.category.name)
                exp.dayNumber?.let { put("dayNumber", it) }
                exp.paidBy?.let { put("paidBy", it) }
            })
        }

        val documentsArray = JSONArray()
        plan.documents.forEach { doc ->
            documentsArray.put(JSONObject().apply {
                put("id", doc.id)
                doc.tripId?.let { put("tripId", it) }
                put("title", doc.title)
                put("category", doc.category.name)
                doc.confirmationNumber?.let { put("confirmationNumber", it) }
                doc.provider?.let { put("provider", it) }
                doc.dayIndex?.let { put("dayIndex", it) }
                doc.dayLabel?.let { put("dayLabel", it) }
                doc.linkedItemId?.let { put("linkedItemId", it) }
                doc.linkedItemTitle?.let { put("linkedItemTitle", it) }
                doc.fileUri?.let { put("fileUri", it) }
                put("fileType", doc.fileType.name)
                doc.fileName?.let { put("fileName", it) }
                doc.fileSizeBytes?.let { put("fileSizeBytes", it) }
                doc.dateTimeLabel?.let { put("dateTimeLabel", it) }
                doc.notes?.let { put("notes", it) }
                put("createdAt", doc.createdAt)
            })
        }

        return JSONObject().apply {
            put("id", plan.id)
            put("userId", plan.userId ?: JSONObject.NULL)
            put("destination", plan.destination)
            put("dateRangeLabel", plan.dateRangeLabel)
            put("travelerCount", plan.travelerCount)
            put("healthScore", plan.healthScore)
            put("budget", plan.budget)
            put("days", daysArray)
            put("stays", staysArray)
            put("dining", diningArray)
            put("supabaseTripId", plan.supabaseTripId ?: JSONObject.NULL)
            put("isShared", plan.isShared)
            put("membersCount", plan.membersCount)
            put("customExpenses", customExpensesArray)
            put("documents", documentsArray)
        }
    }

    fun fromJson(root: JSONObject): TripPlan {
        val travelerCount = root.optInt("travelerCount", 1)
        val daysArray = root.getJSONArray("days")
        val days = (0 until daysArray.length()).map { i ->
            val dayObj = daysArray.getJSONObject(i)
            val itemsArray = dayObj.getJSONArray("items")
            val items = (0 until itemsArray.length()).map { j ->
                val itemObj = itemsArray.getJSONObject(j)
                val itemId = itemObj.optString("id", java.util.UUID.randomUUID().toString())
                val title = itemObj.getString("title")
                val costLabel = itemObj.optString("costLabel", "₹0")

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

                val parsedCostAmount = if (itemObj.has("costAmount")) {
                    itemObj.getInt("costAmount")
                } else {
                    CostParser.parseRupees(costLabel, travelerCount = travelerCount)
                }

                val parsedCategory = itemObj.optString("category").takeIf { it.isNotBlank() }?.let { catStr ->
                    runCatching { ExpenseCategory.valueOf(catStr) }.getOrNull()
                } ?: ActivityIconMapper.categoryFor(title)

                val itemVotes = itemObj.optJSONObject("votes")?.let { v ->
                    com.example.tripmate.model.ActivityVote(
                        upvotes = v.optInt("upvotes", 0),
                        downvotes = v.optInt("downvotes", 0),
                        userVote = v.optString("userVote").takeIf { it.isNotBlank() && it != "null" }
                    )
                } ?: com.example.tripmate.model.ActivityVote()

                ItineraryItem(
                    id = itemId,
                    time = itemObj.getString("time"),
                    title = title,
                    durationLabel = itemObj.optString("durationLabel", "1h"),
                    costLabel = costLabel,
                    costAmount = parsedCostAmount,
                    category = parsedCategory,
                    whyThis = itemObj.optString("whyThis", ""),
                    icon = ActivityIconMapper.iconFor(title),
                    imageUrl = itemObj.optString("imageUrl").takeIf { it.isNotBlank() && it != "null" },
                    isFixed = itemObj.optBoolean("isFixed", false),
                    placeDetails = placeDetails,
                    travelToNext = travelLeg,
                    placeName = if (itemObj.has("placeName")) itemObj.optString("placeName").takeIf { it.isNotBlank() && it != "null" } else null,
                    wikipediaTitle = itemObj.optString("wikipediaTitle").takeIf { it.isNotBlank() && it != "null" },
                    votes = itemVotes
                )
            }
            ItineraryDay(
                dayNumber = dayObj.optInt("dayNumber", i + 1),
                dateLabel = dayObj.optString("dateLabel", "Day ${i + 1}"),
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

        val customExpenses = root.optJSONArray("customExpenses")?.let { ceArray ->
            (0 until ceArray.length()).map { idx ->
                val ce = ceArray.getJSONObject(idx)
                val cat = runCatching { ExpenseCategory.valueOf(ce.getString("category")) }.getOrDefault(ExpenseCategory.OTHER)
                BudgetEntry(
                    id = ce.optString("id", java.util.UUID.randomUUID().toString()),
                    title = ce.getString("title"),
                    amount = ce.getInt("amount"),
                    category = cat,
                    dayNumber = if (ce.has("dayNumber") && !ce.isNull("dayNumber")) ce.getInt("dayNumber") else null,
                    paidBy = ce.optString("paidBy").takeIf { it.isNotBlank() && it != "null" }
                )
            }
        } ?: emptyList()

        val documents = root.optJSONArray("documents")?.let { docArray ->
            (0 until docArray.length()).map { idx ->
                val d = docArray.getJSONObject(idx)
                val cat = runCatching { DocumentCategory.valueOf(d.getString("category")) }.getOrDefault(DocumentCategory.GENERAL)
                val fType = runCatching { DocumentFileType.valueOf(d.optString("fileType", DocumentFileType.NONE.name)) }.getOrDefault(DocumentFileType.NONE)
                TripDocument(
                    id = d.optString("id", java.util.UUID.randomUUID().toString()),
                    tripId = d.optString("tripId").takeIf { it.isNotBlank() && it != "null" },
                    title = d.getString("title"),
                    category = cat,
                    confirmationNumber = d.optString("confirmationNumber").takeIf { it.isNotBlank() && it != "null" },
                    provider = d.optString("provider").takeIf { it.isNotBlank() && it != "null" },
                    dayIndex = if (d.has("dayIndex") && !d.isNull("dayIndex")) d.getInt("dayIndex") else null,
                    dayLabel = d.optString("dayLabel").takeIf { it.isNotBlank() && it != "null" },
                    linkedItemId = d.optString("linkedItemId").takeIf { it.isNotBlank() && it != "null" },
                    linkedItemTitle = d.optString("linkedItemTitle").takeIf { it.isNotBlank() && it != "null" },
                    fileUri = d.optString("fileUri").takeIf { it.isNotBlank() && it != "null" },
                    fileType = fType,
                    fileName = d.optString("fileName").takeIf { it.isNotBlank() && it != "null" },
                    fileSizeBytes = if (d.has("fileSizeBytes") && !d.isNull("fileSizeBytes")) d.getLong("fileSizeBytes") else null,
                    dateTimeLabel = d.optString("dateTimeLabel").takeIf { it.isNotBlank() && it != "null" },
                    notes = d.optString("notes").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = d.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } ?: emptyList()

        return TripPlan(
            id = root.optString("id", java.util.UUID.randomUUID().toString()),
            userId = root.optString("userId").takeIf { it.isNotBlank() && it != "null" },
            destination = root.getString("destination"),
            dateRangeLabel = root.getString("dateRangeLabel"),
            travelerCount = travelerCount,
            healthScore = root.optInt("healthScore", 80),
            budget = root.optInt("budget", 25_000),
            days = days,
            stays = stays,
            dining = dining,
            supabaseTripId = root.optString("supabaseTripId").takeIf { it.isNotBlank() && it != "null" },
            isShared = root.optBoolean("isShared", false),
            membersCount = root.optInt("membersCount", 1),
            customExpenses = customExpenses,
            documents = documents
        )
    }
}
