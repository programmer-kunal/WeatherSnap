package com.example.weathersnap.di

import com.example.weathersnap.data.remote.OpenMeteoApi
import com.example.weathersnap.data.repository.WeatherRepositoryImpl
import com.example.weathersnap.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.app.Application
import androidx.room.Room
import com.example.weathersnap.data.local.WeatherDatabase
import com.example.weathersnap.data.local.dao.ReportDao
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
}
