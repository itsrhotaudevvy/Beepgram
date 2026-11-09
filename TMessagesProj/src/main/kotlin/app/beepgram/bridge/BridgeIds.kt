package app.beepgram.bridge

/**
 * Bridged peers live in a reserved id range so the Telegram layer can tell them apart from
 * real Telegram users and channels without any lookup.
 *
 *   bit 56          : bridge marker (BASE)
 *   bits 48..55     : network id ([NetworkType.id])
 *   bits 0..47      : local sequence number allocated by [BridgeStore]
 *
 * Real Telegram ids are far below 2^56, and the range stays clear of the encrypted-chat
 * (bit 62) and folder (bit 61) dialog id markers used by [org.telegram.messenger.DialogObject].
 *
 * DM portals are represented as Telegram users, everything with more than one remote member as
 * a megagroup channel, so a bridged dialog id is either `+userId` or `-channelId`.
 */
object BridgeIds {
    const val BASE = 1L shl 56
    private const val NET_SHIFT = 48
    private const val LOCAL_MASK = (1L shl NET_SHIFT) - 1

    /**
     * Messages in bridged DMs share Telegram's account-global id space for non-channel messages,
     * so they get ids from a range Telegram itself will not reach.
     */
    const val DM_MESSAGE_ID_BASE = 1_500_000_000

    @JvmStatic
    fun make(network: Int, local: Long): Long = BASE or (network.toLong() shl NET_SHIFT) or (local and LOCAL_MASK)

    /** True for a bridged user id, channel id or dialog id (either sign). */
    @JvmStatic
    fun isBridged(id: Long): Boolean {
        val a = if (id < 0) -id else id
        return a >= BASE && a < (BASE shl 1)
    }

    @JvmStatic
    fun isBridgedDialog(dialogId: Long): Boolean = isBridged(dialogId)

    @JvmStatic
    fun isBridgedDmMessageId(id: Int): Boolean = id >= DM_MESSAGE_ID_BASE

    @JvmStatic
    fun network(id: Long): Int {
        val a = if (id < 0) -id else id
        return ((a ushr NET_SHIFT) and 0xFF).toInt()
    }
}

enum class NetworkType(val id: Int, val title: String, val color: Int, val supportsSlashCommands: Boolean = false) {
    SMS(1, "SMS", 0xFF4CAF50.toInt()),
    DISCORD(2, "Discord", 0xFF5865F2.toInt(), supportsSlashCommands = true),
    FLUXER(3, "Fluxer", 0xFF4641D9.toInt()),
    WHATSAPP(4, "WhatsApp", 0xFF25D366.toInt()),
    INSTAGRAM(5, "Instagram", 0xFFE1306C.toInt()),
    MESSENGER(6, "Messenger", 0xFF0084FF.toInt()),
    TWITTER(7, "X (Twitter)", 0xFF000000.toInt());

    companion object {
        @JvmStatic
        fun byId(id: Int): NetworkType? = entries.firstOrNull { it.id == id }
    }
}
