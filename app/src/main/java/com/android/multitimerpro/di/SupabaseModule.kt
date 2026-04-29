package com.android.multitimerpro.di

import android.util.Log
import com.android.multitimerpro.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = Constants.SUPABASE_URL,
            supabaseKey = Constants.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }

            // Configuramos el serializador para que NO ignore los valores por defecto
            // Esto asegura que dark_mode = false se envíe realmente a Supabase
            val jsonConfig = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            install(Auth)
            install(Postgrest) {
                serializer = KotlinXSerializer(jsonConfig)
            }
            install(Storage)
        }.also { client ->
            client.auth.sessionStatus.onEach { status ->
                Log.d("MT_SUPABASE", "AUTH_STATUS_CHANGE: $status")
            }.launchIn(CoroutineScope(Dispatchers.IO))
        }
    }
}
