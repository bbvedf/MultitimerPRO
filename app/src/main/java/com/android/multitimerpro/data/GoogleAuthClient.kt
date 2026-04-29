package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthClient @Inject constructor(
    private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val googleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Este es tu Web Client ID (debe estar en Google Cloud Console para que Supabase lo valide)
            .requestIdToken("345643159897-jq82kf0jb4f5j3jo0h9n47bor6tkr4fh.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            val idToken = account.idToken ?: throw Exception("Google ID Token is null")

            // Login nativo en Supabase usando el ID Token de Google
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }

            Result.success(account.email ?: "Google User")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
