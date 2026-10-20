package com.example.weathersnap

import com.example.weathersnap.domain.model.CommunityReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityReportTest {

    @Test
    fun communityReport_authorName_defaultsCorrectly() {
        val userReport = CommunityReport(
            id = "1",
            cityName = "Ghaziabad, Uttar Pradesh, India",
            temperature = 27.0,
            condition = "Clear sky",
            humidity = 55,
            windSpeed = 6.2,
            pressure = 990.0,
            notes = "Local field note",
            timestamp = 1000L,
            isLocalUser = true
        )
        assertEquals("You", userReport.authorName)
        assertTrue(userReport.isLocalUser)

        val demoReport = CommunityReport(
            id = "2",
            cityName = "Jaipur, Rajasthan, India",
            temperature = 34.0,
            condition = "Sunny",
            humidity = 30,
            windSpeed = 8.0,
            pressure = 1004.0,
            notes = "Sunny day",
            timestamp = 1000L,
            isLocalUser = false,
            authorName = "PinkCity Demo"
        )
        assertEquals("PinkCity Demo", demoReport.authorName)
        assertFalse(demoReport.isLocalUser)
    }

    @Test
    fun communityReport_filtering_filtersByCityCorrectly() {
        val reports = listOf(
            CommunityReport(
                id = "1",
                cityName = "Ghaziabad, Uttar Pradesh, India",
                temperature = 27.0,
                condition = "Clear sky",
                humidity = 55,
                windSpeed = 6.2,
                pressure = 990.0,
                notes = "Local note",
                timestamp = 2000L,
                isLocalUser = true
            ),
            CommunityReport(
                id = "2",
                cityName = "Jaipur, Rajasthan, India",
                temperature = 34.0,
                condition = "Sunny",
                humidity = 30,
                windSpeed = 8.0,
                pressure = 1004.0,
                notes = "Demo note",
                timestamp = 1000L,
                isLocalUser = false
            )
        )

        val ghaziabadMatches = reports.filter { it.cityName.contains("Ghaziabad", ignoreCase = true) }
        assertEquals(1, ghaziabadMatches.size)
        assertEquals("1", ghaziabadMatches[0].id)

        val myReportsOnly = reports.filter { it.isLocalUser }
        assertEquals(1, myReportsOnly.size)
        assertTrue(myReportsOnly[0].isLocalUser)
    }

    @Test
    fun communityReport_structuredCityMetadata_populatesProperly() {
        val report = CommunityReport(
            id = "supabase-uuid-1",
            userId = "auth-user-123",
            cityId = 1269743,
            cityName = "Ghaziabad",
            cityAdmin1 = "Uttar Pradesh",
            cityCountry = "India",
            displayName = "Ghaziabad, Uttar Pradesh, India",
            latitude = 28.6692,
            longitude = 77.4538,
            temperature = 28.5,
            condition = "Haze",
            humidity = 60,
            windSpeed = 4.2,
            pressure = 1008.0,
            notes = "Air quality moderate",
            imagePath = "auth-user-123/photo.jpg",
            imageUrl = "https://example.supabase.co/storage/v1/object/public/report-photos/auth-user-123/photo.jpg",
            timestamp = 1713500000000L,
            isLocalUser = false
        )

        assertEquals("Ghaziabad", report.cityName)
        assertEquals("Uttar Pradesh", report.cityAdmin1)
        assertEquals("India", report.cityCountry)
        assertEquals("Ghaziabad, Uttar Pradesh, India", report.displayName)
        assertEquals(1269743, report.cityId)
        assertEquals(28.6692, report.latitude, 0.0001)
        assertEquals("Community Member", report.authorName)
        assertEquals("auth-user-123/photo.jpg", report.imagePath)
    }

    @Test
    fun storagePath_formatMatchesSupabaseRlsRequirement() {
        val userId = "11111111-2222-3333-4444-555555555555"
        val filename = "photo_123456789.jpg"
        val storagePath = "$userId/$filename"

        // Verify the path starts with userId/ and doesn't contain bucket name prefix
        val segments = storagePath.split("/")
        assertEquals(2, segments.size)
        assertEquals(userId, segments[0])
        assertEquals(filename, segments[1])
        assertFalse(storagePath.startsWith("report-photos/"))
    }

    @Test
    fun postgrestEscape_correctlyQuotesAndEscapesReservedCharacters() {
        // Commas and spaces (which previously caused logic tree unexpected '+' syntax errors)
        val addressPattern = "%Ghaziabad, Uttar Pradesh, India%"
        val escapedAddress = com.example.weathersnap.data.repository.escapePostgrestFilterValue(addressPattern)
        assertEquals("\"%Ghaziabad, Uttar Pradesh, India%\"", escapedAddress)
        assertTrue(escapedAddress.startsWith("\"") && escapedAddress.endsWith("\""))

        // Parentheses and colons
        val complexPattern = "%St. John's (City): Zone A%"
        val escapedComplex = com.example.weathersnap.data.repository.escapePostgrestFilterValue(complexPattern)
        assertEquals("\"%St. John's (City): Zone A%\"", escapedComplex)

        // Quotes and backslashes
        val quotePattern = "%Area \"Alpha\"\\Beta%"
        val escapedQuote = com.example.weathersnap.data.repository.escapePostgrestFilterValue(quotePattern)
        assertEquals("\"%Area \\\"Alpha\\\"\\\\Beta%\"", escapedQuote)
    }

    @Test
    fun sanitizeErrorMessage_redactsCredentialsTokensAndUrls() {
        val rawError = "failed to parse logic tree (city_name.ilike.%Ghaziabad,+Uttar+Pradesh,+India%) " +
                "URL: https://demo-project.supabase.co/rest/v1/community_reports?or=... " +
                "Headers: [Authorization:[Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummySecretToken], apikey: sb_anon_key_12345]"

        val sanitized = com.example.weathersnap.data.repository.sanitizeErrorMessage(rawError)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummySecretToken"))
        assertFalse(sanitized.contains("sb_anon_key_12345"))
        assertFalse(sanitized.contains("https://demo-project.supabase.co"))
        assertTrue(sanitized.contains("Bearer [REDACTED]"))
        assertTrue(sanitized.contains("apikey: [REDACTED]"))
        assertTrue(sanitized.contains("[URL_REDACTED]"))
    }

    @Test
    fun cityIdFiltering_matchesExactNumericCityIdWithoutAddressComparison() {
        val selectedCityId = 1269743
        val reports = listOf(
            CommunityReport(
                id = "1",
                cityId = 1269743,
                cityName = "Ghaziabad",
                displayName = "Ghaziabad, Uttar Pradesh, India",
                temperature = 28.0,
                condition = "Clear",
                humidity = 50,
                windSpeed = 5.0,
                pressure = 1000.0,
                notes = "Note 1",
                timestamp = 1000L,
                isLocalUser = false
            ),
            CommunityReport(
                id = "2",
                cityId = 1269515,
                cityName = "Jaipur",
                displayName = "Jaipur, Rajasthan, India",
                temperature = 34.0,
                condition = "Sunny",
                humidity = 30,
                windSpeed = 6.0,
                pressure = 1004.0,
                notes = "Note 2",
                timestamp = 1000L,
                isLocalUser = false
            )
        )

        // Exact match on numeric city_id
        val filtered = reports.filter { it.cityId == selectedCityId }
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
        assertEquals("Ghaziabad", filtered[0].cityName)
    }

    @Test
    fun crossDeviceFeed_distinguishesMyReportFromCommunityReportCorrectly() {
        val xiaomiUserId = "xiaomi-device-user-62cf"
        val samsungUserId = "samsung-device-user-b91e"

        // Simulated Supabase DTO mappings on Xiaomi device
        val reportFromXiaomi = CommunityReport(
            id = "r1",
            userId = xiaomiUserId,
            cityId = 1271308,
            cityName = "Ghaziabad",
            temperature = 23.8,
            condition = "Clear sky",
            humidity = 69,
            windSpeed = 6.4,
            pressure = 989.1,
            notes = "Note from Xiaomi",
            timestamp = 1000L,
            isLocalUser = (xiaomiUserId == xiaomiUserId) // true on Xiaomi
        )

        val reportFromSamsung = CommunityReport(
            id = "r2",
            userId = samsungUserId,
            cityId = 1271308,
            cityName = "Ghaziabad",
            temperature = 23.5,
            condition = "Clear sky",
            humidity = 71,
            windSpeed = 6.5,
            pressure = 989.4,
            notes = "Note from Samsung",
            timestamp = 2000L,
            isLocalUser = (samsungUserId == xiaomiUserId) // false on Xiaomi
        )

        val feedOnXiaomi = listOf(reportFromXiaomi, reportFromSamsung)

        // On Xiaomi: exactly 1 report is labelled "YOUR REPORT" and 1 is "COMMUNITY REPORT"
        val myReportsOnXiaomi = feedOnXiaomi.filter { it.isLocalUser }
        val communityReportsOnXiaomi = feedOnXiaomi.filter { !it.isLocalUser }

        assertEquals(1, myReportsOnXiaomi.size)
        assertEquals("r1", myReportsOnXiaomi[0].id)
        assertEquals("You", myReportsOnXiaomi[0].authorName)

        assertEquals(1, communityReportsOnXiaomi.size)
        assertEquals("r2", communityReportsOnXiaomi[0].id)
        assertEquals("Community Member", communityReportsOnXiaomi[0].authorName)
    }

    @Test
    fun crossDeviceBridge_matchesBothNumericCityIdAndLegacyReports() {
        val ghaziabadId = 1271308
        val allDbReports = listOf(
            CommunityReport(
                id = "samsung-1",
                userId = "samsung-user",
                cityId = 0, // Legacy / unsaved ID
                cityName = "Ghaziabad",
                temperature = 23.5,
                condition = "Clear",
                humidity = 71,
                windSpeed = 6.5,
                pressure = 989.4,
                notes = "Legacy report from Samsung",
                timestamp = 1000L,
                isLocalUser = false
            ),
            CommunityReport(
                id = "xiaomi-1",
                userId = "xiaomi-user",
                cityId = 1271308, // Proper Open-Meteo ID
                cityName = "Ghaziabad",
                temperature = 23.8,
                condition = "Clear",
                humidity = 69,
                windSpeed = 6.4,
                pressure = 989.1,
                notes = "Report from Xiaomi",
                timestamp = 2000L,
                isLocalUser = true
            ),
            CommunityReport(
                id = "jaipur-1",
                userId = "other-user",
                cityId = 1269515,
                cityName = "Jaipur",
                temperature = 34.0,
                condition = "Sunny",
                humidity = 30,
                windSpeed = 6.0,
                pressure = 1004.0,
                notes = "Different city",
                timestamp = 3000L,
                isLocalUser = false
            )
        )

        // Filter matching stable cityId OR legacy city_id == 0 with matching cityName
        val matchedFeed = allDbReports.filter {
            it.cityId == ghaziabadId || (it.cityId == 0 && it.cityName.equals("Ghaziabad", true))
        }

        assertEquals(2, matchedFeed.size)
        assertTrue(matchedFeed.any { it.id == "samsung-1" })
        assertTrue(matchedFeed.any { it.id == "xiaomi-1" })
        assertFalse(matchedFeed.any { it.id == "jaipur-1" })
    }
}
