DISPLAY_LANGUAGES = {"zh-CN", "zh-HK", "en"}
SPEECH_LANGUAGES = {"auto", "zh-CN", "zh-HK", "yue", "en"}


def _normalize_language_tag(language: str | None) -> str:
    return (language or "").strip().replace("_", "-").lower()


def normalize_display_language(language: str | None) -> str:
    tag = _normalize_language_tag(language)
    if tag in {"zh-cn", "zh-hans", "zh-hans-cn", "zh-sg", "zh-hans-sg", "zh"}:
        return "zh-CN"
    if tag in {"zh-hk", "zh-mo", "zh-tw", "zh-hant", "zh-hant-hk", "zh-hant-mo", "zh-hant-tw"}:
        return "zh-HK"
    if tag == "en" or tag.startswith("en-"):
        return "en"
    return "zh-CN"


def normalize_speech_language(tts_language: str | None, display_language: str = "zh-CN") -> str:
    display = normalize_display_language(display_language)
    speech = _normalize_language_tag(tts_language)

    if speech in {"", "auto"}:
        return "en" if display == "en" else "zh-CN"
    if speech in {"zh-hk", "zh-mo", "zh-tw", "zh-hant", "zh-hant-hk", "zh-hant-mo", "zh-hant-tw"}:
        return "zh-CN"
    if speech in {"zh-cn", "zh-hans", "zh-hans-cn", "zh-sg", "zh-hans-sg", "zh"}:
        return "zh-CN"
    if speech == "yue":
        return "yue"
    if speech == "en" or speech.startswith("en-"):
        return "en"
    return "en" if display == "en" else "zh-CN"


def display_language_instruction(language: str) -> str:
    normalized = normalize_display_language(language)
    if normalized == "zh-HK":
        return (
            "請用繁體中文回答。表達清楚、自然、適合中老年使用者理解。"
            "不要改寫政策事實。不要改寫成粵語口語，除非使用者明確要求。"
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
        return "请输入适合粤语朗读的文本，但不要改变政策事实。"
    if speech == "en":
        return "Please output clear spoken English. Keep the policy facts unchanged."
    if normalize_display_language(display_language) == "zh-HK":
        return "請輸出適合普通話朗讀的繁體中文文本。句子要短，不要改變政策事實。"
    return "请输入适合普通话朗读的中文文本。句子要短，不要改变政策事实。"
