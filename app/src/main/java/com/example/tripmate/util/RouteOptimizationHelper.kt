package com.example.tripmate.util

import com.example.tripmate.model.ExpenseCategory
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.TravelLeg
import java.util.Locale
import kotlin.math.*

object RouteOptimizationHelper {

    fun distanceBetweenKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lon2 - lon1)
        val a = (sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).let { it * it }).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
        return 6371.0 * c
    }

    fun buildTravelLeg(from: ItineraryItem, to: ItineraryItem): TravelLeg? {
        val lat1 = from.placeDetails?.latitude ?: return null
        val lng1 = from.placeDetails?.longitude ?: return null
        val lat2 = to.placeDetails?.latitude ?: return null
        val lng2 = to.placeDetails?.longitude ?: return null

        val distKm = distanceBetweenKm(lat1, lng1, lat2, lng2)
        val distLabel = if (distKm < 0.95) {
            "${((distKm * 10).toInt() * 100).coerceAtLeast(100)} m"
        } else {
            String.format(Locale.US, "%.1f km", distKm)
        }

        val mode = if (distKm <= 1.0) "Walk" else "Drive"
        val avgSpeed = if (mode == "Walk") 4.5 else 25.0
        val mins = max(2, (distKm / avgSpeed * 60).toInt())
        val durationLabel = if (mins >= 60) "${mins / 60} hr ${mins % 60} mins" else "$mins mins"

        return TravelLeg(
            distanceLabel = distLabel,
            durationLabel = durationLabel,
            transportMode = mode
        )
    }

    /**
     * Optimizes the daily itinerary order.
     * Fixed activities and meal stops (lunch/dinner) remain anchored in their chronological positions.
     * Intermediate sightseeing/attraction stops between anchors are permuted to minimize total travel distance.
     */
    fun optimizeDay(items: List<ItineraryItem>): List<ItineraryItem> {
        if (items.size <= 2) return items

        // Partition into anchor items and variable segments
        val isAnchor = items.map { item ->
            item.isFixed || item.category == ExpenseCategory.FOOD || item.placeDetails?.latitude == null
        }

        // If all items are anchors or insufficient places have coordinates, return as-is
        val coordinateCount = items.count { it.placeDetails?.latitude != null }
        if (coordinateCount <= 2) return items

        val result = items.toMutableList()

        // Identify contiguous unanchored segments to optimize
        var segStart = -1
        for (i in items.indices) {
            if (!isAnchor[i]) {
                if (segStart == -1) segStart = i
            } else {
                if (segStart != -1) {
                    optimizeSegment(result, segStart, i - 1)
                    segStart = -1
                }
            }
        }
        if (segStart != -1) {
            optimizeSegment(result, segStart, items.lastIndex)
        }

        // Recalculate travel legs between consecutive items
        val updated = result.mapIndexed { idx, item ->
            val next = result.getOrNull(idx + 1)
            val leg = if (next != null) buildTravelLeg(item, next) ?: item.travelToNext else null
            item.copy(travelToNext = leg)
        }

        return updated
    }

    private fun optimizeSegment(list: MutableList<ItineraryItem>, start: Int, end: Int) {
        val segmentSize = end - start + 1
        if (segmentSize <= 1) return

        val prevItem = if (start > 0) list[start - 1] else null
        val nextItem = if (end < list.lastIndex) list[end + 1] else null

        val subList = list.subList(start, end + 1).toList()

        // For small segment sizes (<= 7), evaluate permutations to find the optimal route
        val bestPermutation = if (subList.size <= 7) {
            findOptimalPermutation(subList, prevItem, nextItem)
        } else {
            twoOpt(subList, prevItem, nextItem)
        }

        for (i in bestPermutation.indices) {
            list[start + i] = bestPermutation[i]
        }
    }

    private fun findOptimalPermutation(
        items: List<ItineraryItem>,
        prev: ItineraryItem?,
        next: ItineraryItem?
    ): List<ItineraryItem> {
        var bestRoute = items
        var bestDist = Double.MAX_VALUE

        fun permute(current: MutableList<ItineraryItem>, l: Int, r: Int) {
            if (l == r) {
                val d = calculateTotalDistance(current, prev, next)
                if (d < bestDist) {
                    bestDist = d
                    bestRoute = current.toList()
                }
                return
            }
            for (i in l..r) {
                current.swap(l, i)
                permute(current, l + 1, r)
                current.swap(l, i)
            }
        }

        permute(items.toMutableList(), 0, items.lastIndex)
        return bestRoute
    }

    private fun twoOpt(
        items: List<ItineraryItem>,
        prev: ItineraryItem?,
        next: ItineraryItem?
    ): List<ItineraryItem> {
        var route = items.toMutableList()
        var bestDist = calculateTotalDistance(route, prev, next)
        var improved = true
        var iterations = 0

        while (improved && iterations < 50) {
            improved = false
            iterations++
            for (i in 0 until route.size - 1) {
                for (k in i + 1 until route.size) {
                    val candidate = twoOptSwap(route, i, k)
                    val dist = calculateTotalDistance(candidate, prev, next)
                    if (dist < bestDist - 0.001) {
                        bestDist = dist
                        route = candidate.toMutableList()
                        improved = true
                        break
                    }
                }
                if (improved) break
            }
        }
        return route
    }

    private fun twoOptSwap(route: List<ItineraryItem>, i: Int, k: Int): List<ItineraryItem> {
        val result = mutableListOf<ItineraryItem>()
        for (c in 0 until i) result.add(route[c])
        for (c in k downTo i) result.add(route[c])
        for (c in k + 1 until route.size) result.add(route[c])
        return result
    }

    private fun calculateTotalDistance(
        route: List<ItineraryItem>,
        prev: ItineraryItem?,
        next: ItineraryItem?
    ): Double {
        var sum = 0.0
        val prevLat = prev?.placeDetails?.latitude
        val prevLng = prev?.placeDetails?.longitude
        val firstLat = route.firstOrNull()?.placeDetails?.latitude
        val firstLng = route.firstOrNull()?.placeDetails?.longitude
        if (prevLat != null && prevLng != null && firstLat != null && firstLng != null) {
            sum += distanceBetweenKm(prevLat, prevLng, firstLat, firstLng)
        }

        for (i in 0 until route.size - 1) {
            val l1 = route[i].placeDetails?.latitude
            val g1 = route[i].placeDetails?.longitude
            val l2 = route[i + 1].placeDetails?.latitude
            val g2 = route[i + 1].placeDetails?.longitude
            if (l1 != null && g1 != null && l2 != null && g2 != null) {
                sum += distanceBetweenKm(l1, g1, l2, g2)
            }
        }

        val lastLat = route.lastOrNull()?.placeDetails?.latitude
        val lastLng = route.lastOrNull()?.placeDetails?.longitude
        val nextLat = next?.placeDetails?.latitude
        val nextLng = next?.placeDetails?.longitude
        if (lastLat != null && lastLng != null && nextLat != null && nextLng != null) {
            sum += distanceBetweenKm(lastLat, lastLng, nextLat, nextLng)
        }

        return sum
    }

    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}
