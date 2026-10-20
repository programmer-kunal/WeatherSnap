package com.example.weathersnap.di

import android.app.Application
import androidx.room.Room
import com.example.weathersnap.BuildConfig
import com.example.weathersnap.data.local.WeatherDatabase
import com.example.weathersnap.data.local.dao.ReportDao
import com.example.weathersnap.data.remote.OpenMeteoApi
import com.example.weathersnap.data.remote.supabase.SharedPreferencesSessionManager
import com.example.weathersnap.data.remote.supabase.SupabaseAuthManager
import com.example.weathersnap.data.repository.SupabaseCommunityReportRepositoryImpl
import com.example.weathersnap.data.repository.WeatherRepositoryImpl
import com.example.weathersnap.data.util.ImageCompressor
import com.example.weathersnap.domain.repository.CommunityReportRepository
import com.example.weathersnap.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOpenMeteoApi(): OpenMeteoApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/") 
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherDatabase(app: Application): WeatherDatabase {
        return Room.databaseBuilder(
            app,
            WeatherDatabase::class.java,
            "weather_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideReportDao(db: WeatherDatabase): ReportDao {
        return db.reportDao
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(api: OpenMeteoApi, dao: ReportDao): WeatherRepository {
        return WeatherRepositoryImpl(api, dao)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(app: Application): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                sessionManager = SharedPreferencesSessionManager(app)
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideCommunityReportRepository(
        supabase: SupabaseClient,
        authManager: SupabaseAuthManager,
        imageCompressor: ImageCompressor,
        app: Application
    ): CommunityReportRepository {
        return SupabaseCommunityReportRepositoryImpl(
            supabase = supabase,
            authManager = authManager,
            imageCompressor = imageCompressor,
            context = app
        )
    }
}
