package app.beepgram.bridge

/**
 * Formatting spans over [RemoteMessage.text], mirroring Telegram message entities so the
 * bridge can map them 1:1 onto TL `MessageEntity` without re-parsing markdown.
 * Offsets/lengths are in UTF-16 code units, exactly like Telegram.
 */
object TextFormat {
    enum class Type {
        BOLD, ITALIC, UNDERLINE, STRIKE, SPOILER, CODE, PRE, TEXT_URL, MENTION, HASHTAG,
        BOT_COMMAND, URL, EMAIL, PHONE, BLOCKQUOTE
    }

    data class Span(
        val type: Type,
        val offset: Int,
        val length: Int,
        /** TEXT_URL target, PRE language, MENTION user id, etc. */
        val extra: String? = null,
    )

    fun bold(offset: Int, length: Int) = Span(Type.BOLD, offset, length)
    fun italic(offset: Int, length: Int) = Span(Type.ITALIC, offset, length)
    fun code(offset: Int, length: Int) = Span(Type.CODE, offset, length)
    fun pre(offset: Int, length: Int, language: String? = null) = Span(Type.PRE, offset, length, language)
    fun link(offset: Int, length: Int, url: String) = Span(Type.TEXT_URL, offset, length, url)
    fun spoiler(offset: Int, length: Int) = Span(Type.SPOILER, offset, length)

    /** Wrap a whole string in a single span. */
    fun whole(text: String, type: Type, extra: String? = null) = Span(type, 0, text.length, extra)
}