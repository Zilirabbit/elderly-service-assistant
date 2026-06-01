package com.example.eldercareapp.core.i18n

import java.util.Locale

object AppLanguageResolver {
    enum class DisplayLanguage(val apiCode: String) {
        ZH_CN("zh-CN"),
        ZH_HK("zh-HK"),
        EN("en")
    }

    enum class SpeechLanguagePreference(val apiCode: String) {
        AUTO("auto"),
        ZH_CN("zh-CN"),
        YUE("yue"),
        EN("en")
    }

    fun resolveDisplayLanguage(locale: Locale): DisplayLanguage {
        val language = locale.language.lowercase(Locale.ROOT)
        val country = locale.country.uppercase(Locale.ROOT)
        val script = locale.script

        return when {
            language == "zh" && (
                country == "HK" ||
                    country == "MO" ||
                    country == "TW" ||
                    script.equals("Hant", ignoreCase = true)
                ) -> DisplayLanguage.ZH_HK

            language == "zh" -> DisplayLanguage.ZH_CN
            language == "en" -> DisplayLanguage.EN
            else -> DisplayLanguage.ZH_CN
        }
    }

    fun resolveSpeechLanguage(
        preference: SpeechLanguagePreference,
        displayLanguage: DisplayLanguage
    ): String {
        return when (preference) {
            SpeechLanguagePreference.AUTO -> when (displayLanguage) {
                DisplayLanguage.EN -> SpeechLanguagePreference.EN.apiCode
                DisplayLanguage.ZH_CN,
                DisplayLanguage.ZH_HK -> SpeechLanguagePreference.ZH_CN.apiCode
            }
            SpeechLanguagePreference.ZH_CN -> SpeechLanguagePreference.ZH_CN.apiCode
            SpeechLanguagePreference.YUE -> SpeechLanguagePreference.YUE.apiCode
            SpeechLanguagePreference.EN -> SpeechLanguagePreference.EN.apiCode
        }
    }
}
