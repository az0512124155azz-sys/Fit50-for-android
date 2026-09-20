package com.fit50.app

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TranslationManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("fit50_i18n", Context.MODE_PRIVATE)
    private val translators = ConcurrentHashMap<String, Translator>()

    data class Language(
        val code: String,
        val nativeName: String,
        val rtl: Boolean = false
    )

    companion object {
        val supported = listOf(
            Language("en", "English"),
            Language("he", "עברית", true),
            Language("ar", "العربية", true),
            Language("fr", "Français"),
            Language("es", "Español"),
            Language("de", "Deutsch"),
            Language("it", "Italiano"),
            Language("pt", "Português"),
            Language("ru", "Русский"),
            Language("tr", "Türkçe"),
            Language("zh", "中文"),
            Language("ja", "日本語"),
            Language("ko", "한국어"),
            Language("hi", "हिन्दी"),
            Language("pl", "Polski"),
            Language("nl", "Nederlands")
        )

        private val supportedCodes = supported.map { it.code }.toSet()

        fun normalizeLanguage(code: String?): String {
            val raw = code.orEmpty().lowercase(Locale.ROOT)
            val base = raw.substringBefore('-').substringBefore('_')
            val normalized = when (base) {
                "iw" -> "he"
                "in" -> "id"
                "ji" -> "yi"
                else -> base
            }
            return if (normalized in supportedCodes) normalized else "en"
        }
    }

    fun getDeviceLanguage(): String =
        normalizeLanguage(Locale.getDefault().toLanguageTag())

    fun getPreferredLanguage(): String =
        prefs.getString("preferred_language", "auto") ?: "auto"

    fun getEffectiveLanguage(): String {
        val preferred = getPreferredLanguage()
        return if (preferred == "auto") getDeviceLanguage() else normalizeLanguage(preferred)
    }

    fun setPreferredLanguage(code: String) {
        val value = if (code == "auto") "auto" else normalizeLanguage(code)
        prefs.edit().putString("preferred_language", value).apply()
    }

    fun supportedLanguagesJson(): String {
        val array = JSONArray()
        array.put(
            JSONObject()
                .put("code", "auto")
                .put("name", "Auto / Device language")
                .put("rtl", false)
        )
        supported.forEach { lang ->
            array.put(
                JSONObject()
                    .put("code", lang.code)
                    .put("name", lang.nativeName)
                    .put("rtl", lang.rtl)
            )
        }
        return array.toString()
    }

    fun translateTexts(
        targetLanguage: String,
        textsJson: String,
        done: (Boolean, String, String) -> Unit
    ) {
        val target = normalizeLanguage(targetLanguage)

        val input = runCatching { JSONArray(textsJson) }.getOrElse {
            done(false, target, "{}")
            return
        }

        val requested = mutableListOf<String>()
        for (i in 0 until input.length()) {
            val value = input.optString(i).trim()
            if (value.isNotEmpty() && value !in requested) {
                requested += value
            }
        }

        if (requested.isEmpty()) {
            done(true, target, "{}")
            return
        }

        if (target == "he") {
            val out = JSONObject()
            requested.forEach { out.put(it, it) }
            done(true, target, out.toString())
            return
        }

        val mlTarget = mlKitLanguage(target)
        if (mlTarget == null) {
            done(false, target, "{}")
            return
        }

        val cache = loadCache(target)
        val result = JSONObject()
        val missing = mutableListOf<String>()

        requested.forEach { original ->
            val cached = cache.optString(original, "")
            if (cached.isNotBlank()) {
                result.put(original, cached)
            } else {
                missing += original
            }
        }

        if (missing.isEmpty()) {
            done(true, target, result.toString())
            return
        }

        val translator = translators.getOrPut(target) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.HEBREW)
                .setTargetLanguage(mlTarget)
                .build()
            Translation.getClient(options)
        }

        val conditions = DownloadConditions.Builder().build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translateSequential(
                    translator = translator,
                    target = target,
                    texts = missing,
                    index = 0,
                    result = result,
                    cache = cache,
                    done = done
                )
            }
            .addOnFailureListener {
                done(false, target, result.toString())
            }
    }

    private fun translateSequential(
        translator: Translator,
        target: String,
        texts: List<String>,
        index: Int,
        result: JSONObject,
        cache: JSONObject,
        done: (Boolean, String, String) -> Unit
    ) {
        if (index >= texts.size) {
            saveCache(target, cache)
            done(true, target, result.toString())
            return
        }

        val original = texts[index]

        translator.translate(original)
            .addOnSuccessListener { translated ->
                val value = translated.trim().ifBlank { original }
                result.put(original, value)
                cache.put(original, value)
                translateSequential(
                    translator,
                    target,
                    texts,
                    index + 1,
                    result,
                    cache,
                    done
                )
            }
            .addOnFailureListener {
                result.put(original, original)
                translateSequential(
                    translator,
                    target,
                    texts,
                    index + 1,
                    result,
                    cache,
                    done
                )
            }
    }

    private fun loadCache(language: String): JSONObject =
        runCatching {
            JSONObject(prefs.getString("translation_cache_$language", "{}") ?: "{}")
        }.getOrDefault(JSONObject())

    private fun saveCache(language: String, cache: JSONObject) {
        prefs.edit()
            .putString("translation_cache_$language", cache.toString())
            .apply()
    }

    private fun mlKitLanguage(code: String): String? = when (code) {
        "en" -> TranslateLanguage.ENGLISH
        "ar" -> TranslateLanguage.ARABIC
        "fr" -> TranslateLanguage.FRENCH
        "es" -> TranslateLanguage.SPANISH
        "de" -> TranslateLanguage.GERMAN
        "it" -> TranslateLanguage.ITALIAN
        "pt" -> TranslateLanguage.PORTUGUESE
        "ru" -> TranslateLanguage.RUSSIAN
        "tr" -> TranslateLanguage.TURKISH
        "zh" -> TranslateLanguage.CHINESE
        "ja" -> TranslateLanguage.JAPANESE
        "ko" -> TranslateLanguage.KOREAN
        "hi" -> TranslateLanguage.HINDI
        "pl" -> TranslateLanguage.POLISH
        "nl" -> TranslateLanguage.DUTCH
        else -> null
    }
}
