package com.example.tripmate.util

object DestinationImageProvider {

    fun getImageFor(title: String, destination: String? = null): String {
        val combined = "$title ${destination.orEmpty()}".lowercase()

        return when {
            // Kashmir, Dal Lake, Shikara specific
            combined.contains("shikara") || combined.contains("dal lake") || combined.contains("srinagar") || combined.contains("kashmir") ->
                "https://images.unsplash.com/photo-1598091383021-15ddea10925d?auto=format&fit=crop&w=1200&q=80"

            // Snow, Gulmarg, Mountains, Himalayas
            combined.contains("gulmarg") || combined.contains("snow") || combined.contains("ski") || combined.contains("himalaya") ->
                "https://images.unsplash.com/photo-1566837945700-30057527ade0?auto=format&fit=crop&w=1200&q=80"

            // Gardens, Mughal Gardens, Shalimar, Nishat
            combined.contains("shalimar") || combined.contains("nishat") || combined.contains("garden") || combined.contains("park") ->
                "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=1200&q=80"

            // Beaches, Goa, Islands
            combined.contains("beach") || combined.contains("goa") || combined.contains("sea") || combined.contains("ocean") || combined.contains("island") ->
                "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80"

            // Forts, Palaces, Heritage, Monuments
            combined.contains("fort") || combined.contains("palace") || combined.contains("mahal") || combined.contains("heritage") || combined.contains("temple") ->
                "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1200&q=80"

            // Kyoto, Japan
            combined.contains("kyoto") || combined.contains("japan") || combined.contains("tokyo") ->
                "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1200&q=80"

            // Paris, France
            combined.contains("paris") || combined.contains("eiffel") || combined.contains("louvre") ->
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80"

            // Cafes, Food, Restaurants
            combined.contains("cafe") || combined.contains("food") || combined.contains("dining") || combined.contains("dinner") || combined.contains("breakfast") ->
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80"

            // Markets, Bazaars, Shopping
            combined.contains("market") || combined.contains("bazaar") || combined.contains("shop") ->
                "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&w=1200&q=80"

            // General Scenic Travel Default
            else ->
                "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=1200&q=80"
        }
    }
}
