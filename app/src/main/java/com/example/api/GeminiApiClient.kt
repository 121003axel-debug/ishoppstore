package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun askGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext "¡Hola! Estoy listo para ayudarte con tu compra en iShopp, pero el Administrador aún no ha configurado una clave API de Gemini válida. No te preocupes, ¡puedes seguir explorando nuestra tienda y agregar los mejores productos como la MacBook Pro M3 o los Audífonos Sony XM5 a tu carrito!"
        }

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                if (systemInstruction != null) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, Message ${response.message}")
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Error body: $errorMsg")
                    return@withContext "Lo siento, tuve un problema de conexión con el asistente de iShopp. Por favor, intenta de nuevo en unos momentos."
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No pude generar respuesta.")
                        }
                    }
                }
                return@withContext "Disculpa, no obtuve una respuesta comprensible de mi sistema. ¿Te puedo sugerir algún producto o asistir en tu compra?"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in askGemini: ", e)
            return@withContext "No se pudo conectar con el Asistente AI: ${e.localizedMessage}. ¡Te invito a que sigas disfrutando y comprando dentro de iShopp Store!"
        }
    }
}
