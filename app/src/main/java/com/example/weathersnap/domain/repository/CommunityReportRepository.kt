package com.example.weathersnap.domain.repository

import com.example.weathersnap.domain.model.CommunityReport
import kotlinx.coroutines.flow.Flow

interface CommunityReportRepository {
    fun getCommunityReports(cityQuery: String? = null, cityId: Int? = null): Flow<List<CommunityReport>>
    fun getMyReports(): Flow<List<CommunityReport>>
    suspend fun publishReport(report: CommunityReport): Result<Unit>
    suspend fun deleteCommunityReport(reportId: String): Result<Unit>
}
