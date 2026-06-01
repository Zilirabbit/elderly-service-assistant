DISPLAY_LANGUAGES = {"zh-CN", "zh-HK", "en"}
SPEECH_LANGUAGES = {"auto", "zh-CN", "zh-HK", "yue", "en"}


def normalize_display_language(language: str | None) -> str:
    if language in DISPLAY_LANGUAGES:
        return language
    return "zh-CN"


def normalize_speech_language(tts_language: str | None, display_language: str = "zh-CN") -> str:
    display = normalize_display_language(display_language)
    if tts_language not in SPEECH_LANGUAGES or tts_language in {None, "auto"}:
        if display == "en":
            return "en"
        return "zh-CN"
    if tts_language == "zh-HK":
        return "zh-CN"
    return tts_language


def display_language_instruction(language: str) -> str:
    normalized = normalize_display_language(language)
    if normalized == "zh-HK":
        return (
            "請用繁體中文回答。表達清楚、自然、適合中老年使用者理解。"
            "不要改變政策事實。不要改寫成粵語口語，除非使用者明確要求。"
        )
    if normalized == "en":
        return (
            "Please answer in clear and simple English. Keep the policy facts unchanged. "
            "Use short sentences suitable for older users."
        )
    return "请用简体中文回答。表达清楚、口语化、适合中老年用户理解。不要改变政策事实。"


def tts_language_instruction(speech_language: str, display_language: str = "zh-CN") -> str:
    speech = normalize_speech_language(speech_language, display_language)
    if speech == "yue":
        return "请输出适合粤语朗读的文本，但不要改变政策事实。"
    if speech == "en":
        return "Please output clear spoken English. Keep the policy facts unchanged."
    return "请输出适合普通话朗读的中文文本。句子要短，不要改变政策事实。"
