package com.example.weathersnap.data.repository

import android.content.Context
import android.util.Log
import com.example.weathersnap.data.remote.dto.CommunityReportDto
import com.example.weathersnap.data.remote.supabase.SupabaseAuthManager
import com.example.weathersnap.data.util.ImageCompressor
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.domain.repository.CommunityReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Singleton
class SupabaseCommunityReportRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val authManager: SupabaseAuthManager,
    private val imageCompressor: ImageCompressor,
    @param:ApplicationContext private val context: Context
) : CommunityReportRepository {

    private suspend fun fetchCommunityReports(
        cityQuery: String?,
        cityId: Int?,
        currentUserId: String?
    ): List<CommunityReport> {
        val queryBuilder = supabase.from("community_reports").select {
            // 1. If filtering by a selected city, match on stable numeric city_id
            // and bridge any legacy reports where city_id was 0 but city_name matches
            if (cityId != null && cityId > 0) {
                val baseCity = if (!cityQuery.isNullOrBlank()) {
                    cityQuery.substringBefore(',').trim()
                } else null

                if (!baseCity.isNullOrBlank() && !baseCity.equals("All", ignoreCase = true)) {
                    val safeBase = escapePostgrestFilterValue("%$baseCity%")
                    filter {
                        or {
                            eq("city_id", cityId)
                            and {
                                eq("city_id", 0)
                                ilike("city_name", safeBase)
                            }
                        }
                    }
                } else {
                    filter {
                        eq("city_id", cityId)
                    }
                }
            } else if (!cityQuery.isNullOrBlank() && !cityQuery.trim().equals("All", ignoreCase = true)) {
                val clean = cityQuery.trim()
                val numericCityId = clean.toIntOrNull()
                if (numericCityId != null && numericCityId > 0) {
                    filter {
                        eq("city_id", numericCityId)
                    }
                } else {
                    // 2. Free-text search across multiple fields (city_name, display_name)
                    // Extract base city name if query contains address segments
                    val baseName = clean.substringBefore(',').trim().ifEmpty { clean }
                    val safeBase = escapePostgrestFilterValue("%$baseName%")
                    val safeFull = escapePostgrestFilterValue("%$clean%")

                    filter {
                        or {
                            ilike("city_name", safeBase)
                            ilike("display_name", safeFull)
                        }
                    }
                }
            }
            // 3. If empty filter or "All", no filter is added: global feed is queried
            order("created_at", Order.DESCENDING)
            limit(50)
        }

        val dtoList = queryBuilder.decodeList<CommunityReportDto>()
        return dtoList.map { dto -> mapDtoToDomain(dto, currentUserId) }
    }

    private suspend fun fetchMyReports(currentUserId: String): List<CommunityReport> {
        val dtoList = supabase.from("community_reports").select {
            filter {
                eq("user_id", currentUserId)
            }
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList<CommunityReportDto>()

        return dtoList.map { dto -> mapDtoToDomain(dto, currentUserId) }
    }

    @OptIn(FlowPreview::class)
    override fun getCommunityReports(cityQuery: String?, cityId: Int?): Flow<List<CommunityReport>> = callbackFlow {
        val currentUserId = try {
            authManager.getOrCreateAuthenticatedUserId()
        } catch (_: Exception) {
            authManager.getCurrentUserId()
        }

        // Step 1: Initial database fetch
        try {
            val initial = fetchCommunityReports(cityQuery, cityId, currentUserId)
            trySend(initial)
        } catch (e: Exception) {
            logSanitizedError("SupabaseCommunityRepo", "Error fetching initial community reports", e)
            close(SafeCommunityReportsException("Unable to load community reports. Please check your network connection and try again."))
            return@callbackFlow
        }

        // Step 2: Set up Realtime channel and subscription for public.community_reports
        val channelId = "community_feed_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val channel = supabase.channel(channelId)

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "community_reports"
        }

        // Debounce trigger to coalesce rapid back-to-back database events
        val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

        val refreshJob = launch {
            refreshTrigger.receiveAsFlow()
                .debounce(300)
                .collect {
                    try {
                        val updated = fetchCommunityReports(cityQuery, cityId, currentUserId)
                        trySend(updated)
                    } catch (e: Exception) {
                        logSanitizedError("SupabaseCommunityRepo", "Error refreshing community reports on realtime update", e)
                    }
                }
        }

        val changeJob = launch {
            changeFlow.collect { _ ->
                refreshTrigger.trySend(Unit)
            }
        }

        val statusJob = launch {
            channel.status.collect { status ->
                if (status == RealtimeChannel.Status.SUBSCRIBED) {
                    refreshTrigger.trySend(Unit)
                }
            }
        }

        launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                logSanitizedError("SupabaseCommunityRepo", "Failed to subscribe to realtime channel", e)
            }
        }

        awaitClose {
            refreshJob.cancel()
            changeJob.cancel()
            statusJob.cancel()
            refreshTrigger.close()
            launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (_: Exception) {}
            }
        }
    }

    @OptIn(FlowPreview::class)
    override fun getMyReports(): Flow<List<CommunityReport>> = callbackFlow {
        val currentUserId = authManager.getOrCreateAuthenticatedUserId()

        try {
            val initial = fetchMyReports(currentUserId)
            trySend(initial)
        } catch (e: Exception) {
            logSanitizedError("SupabaseCommunityRepo", "Error fetching my reports", e)
            close(SafeCommunityReportsException("Unable to load your reports. Please check your network connection and try again."))
            return@callbackFlow
        }

        val channelId = "my_reports_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val channel = supabase.channel(channelId)

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "community_reports"
        }

        val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

        val refreshJob = launch {
            refreshTrigger.receiveAsFlow()
                .debounce(300)
                .collect {
                    try {
                        val updated = fetchMyReports(currentUserId)
                        trySend(updated)
                    } catch (e: Exception) {
                        logSanitizedError("SupabaseCommunityRepo", "Error refreshing my reports", e)
                    }
                }
        }

        val changeJob = launch {
            changeFlow.collect { _ ->
                refreshTrigger.trySend(Unit)
            }
        }

        val statusJob = launch {
            channel.status.collect { status ->
                if (status == RealtimeChannel.Status.SUBSCRIBED) {
                    refreshTrigger.trySend(Unit)
                }
            }
        }

        launch {
            try {
                channel.subscribe()
            } catch (e: Exception) {
                logSanitizedError("SupabaseCommunityRepo", "Failed to subscribe to my reports realtime channel", e)
            }
        }

        awaitClose {
            refreshJob.cancel()
            changeJob.cancel()
            statusJob.cancel()
            refreshTrigger.close()
            launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun publishReport(report: CommunityReport): Result<Unit> {
        return try {
            // 1. Establish/verify anonymous user identity
            val userId = authManager.getOrCreateAuthenticatedUserId()

            // 2. Compress & validate photo format/size (<= 5 MB)
            val localImagePath = report.imagePath
                ?: return Result.failure(IllegalArgumentException("Report is missing local photograph path."))
            val imageBytes = imageCompressor.compressAndValidate(localImagePath)

            // 3. Generate unique object filename and path: {userId}/{unique_filename}.jpg
            val uniqueFilename = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val storagePath = "$userId/$uniqueFilename"

            // 4. Upload photo to 'report-photos' with upsert = false
            val bucket = supabase.storage.from("report-photos")
            bucket.upload(
                path = storagePath,
                data = imageBytes
            ) {
                upsert = false
            }

            // 5. Insert report into database
            try {
                val dto = CommunityReportDto(
                    userId = userId,
                    cityId = report.cityId,
                    cityName = report.cityName,
                    cityAdmin1 = report.cityAdmin1,
                    cityCountry = report.cityCountry,
                    displayName = report.displayName,
                    latitude = report.latitude,
                    longitude = report.longitude,
                    temperature = report.temperature,
                    condition = report.condition,
                    humidity = report.humidity,
                    windSpeed = report.windSpeed,
                    pressure = report.pressure,
                    notes = report.notes,
                    imagePath = storagePath
                )
                supabase.from("community_reports").insert(dto)
                Result.success(Unit)
            } catch (dbError: Exception) {
                // Partial failure rollback: clean up uploaded image if DB insert failed
                try {
                    bucket.delete(listOf(storagePath))
                } catch (_: Exception) {}
                Result.failure(dbError)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCommunityReport(reportId: String): Result<Unit> {
        return try {
            val currentUserId = authManager.getOrCreateAuthenticatedUserId()

            // 1. Fetch report to verify ownership and obtain storage image_path
            val existing = supabase.from("community_reports").select {
                filter {
                    eq("id", reportId)
                    eq("user_id", currentUserId)
                }
            }.decodeSingleOrNull<CommunityReportDto>()
                ?: return Result.failure(IllegalStateException("Report not found or not owned by current user."))

            // 2. Step 1 of safe deletion: Delete database record
            supabase.from("community_reports").delete {
                filter {
                    eq("id", reportId)
                    eq("user_id", currentUserId)
                }
            }

            // 3. Step 2 of safe deletion: Delete storage file
            try {
                supabase.storage.from("report-photos").delete(listOf(existing.imagePath))
            } catch (storageError: Exception) {
                // If storage delete fails, DB row is already removed from feed.
                // Log/proceed safely without breaking UI deletion state.
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDtoToDomain(dto: CommunityReportDto, currentUserId: String?): CommunityReport {
        val isLocal = currentUserId != null && dto.userId == currentUserId
        val publicUrl = supabase.storage.from("report-photos").publicUrl(dto.imagePath)

        return CommunityReport(
            id = dto.id ?: UUID.randomUUID().toString(),
            userId = dto.userId,
            cityId = dto.cityId,
            cityName = dto.cityName,
            cityAdmin1 = dto.cityAdmin1,
            cityCountry = dto.cityCountry,
            displayName = dto.displayName,
            latitude = dto.latitude,
            longitude = dto.longitude,
            temperature = dto.temperature,
            condition = dto.condition,
            humidity = dto.humidity,
            windSpeed = dto.windSpeed,
            pressure = dto.pressure,
            notes = dto.notes,
            imagePath = dto.imagePath,
            imageUrl = publicUrl,
            drawableResId = null,
            timestamp = parseIsoTimestamp(dto.createdAt),
            isLocalUser = isLocal,
            authorName = if (isLocal) "You" else "Community Member"
        )
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Instant.parse(isoString).toEpochMilli()
            } else {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format.parse(isoString.take(19))?.time ?: System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

class SafeCommunityReportsException(message: String) : Exception(message)

internal fun escapePostgrestFilterValue(pattern: String): String {
    // Escape backslashes first, then double quotes
    val escaped = pattern
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    // Double quotes ensure PostgREST parses the entire operand as a single literal,
    // preventing commas, parentheses, colons, or spaces from breaking the logic tree.
    return "\"$escaped\""
}

internal fun sanitizeErrorMessage(raw: String): String {
    return raw
        .replace(Regex("Bearer\\s+[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
        .replace(Regex("apikey=[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "apikey=[REDACTED]")
        .replace(Regex("apikey:[\\s]*[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "apikey: [REDACTED]")
        .replace(Regex("https?://[^\\s]+"), "[URL_REDACTED]")
}

private fun logSanitizedError(tag: String, message: String, e: Throwable) {
    val raw = e.message ?: e.toString()
    val sanitized = sanitizeErrorMessage(raw)
    Log.e(tag, "$message: $sanitized")
}
