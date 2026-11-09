package app.beepgram.bridge

import org.json.JSONObject
import java.io.File

/*
 * Network-neutral model that every connector produces and consumes. Connectors never see
 * Telegram TL objects; [Bridge] converts between this model and TL.
 */

data class RemoteUser(
    val id: String,
    val name: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isBot: Boolean = false,
    val phone: String? = null,
    val about: String? = null,
)

enum class ChatKind { DM, GROUP, CHANNEL }

data class RemoteChat(
    val id: String,
    val kind: ChatKind,
    val title: String? = null,
    val avatarUrl: String? = null,
    /** For [ChatKind.DM]: the other party. */
    val dmUser: RemoteUser? = null,
    /** Guild / server / community the chat belongs to, used for titles and grouping. */
    val parentName: String? = null,
    val about: String? = null,
    val memberCount: Int = 0,
    /** Telegram folder to place the dialog in: 0 = main list, 1 = archive. */
    val folderId: Int = 0,
    val readOnly: Boolean = false,
)

enum class AttachmentKind { IMAGE, VIDEO, GIF, AUDIO, VOICE, STICKER, FILE }

data class RemoteAttachment(
    val kind: AttachmentKind,
    /** Opaque reference the connector understands in [NetworkConnector.download]; usually a URL. */
    val ref: String,
    val mime: String,
    val fileName: String,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val durationSec: Double = 0.0,
    val spoiler: Boolean = false,
)

data class RemoteButton(
    val label: String,
    val url: String? = null,
    /** Opaque payload handed back to [NetworkConnector.pressButton]. */
    val callback: String? = null,
)

data class RemoteReaction(val emoji: String, val count: Int, val me: Boolean)

data class RemoteMessage(
    val chatId: String,
    val id: String,
    val sender: RemoteUser?,
    val fromMe: Boolean,
    val timestampMs: Long,
    /** Plain text; formatting is carried by [entities] ([TextFormat] helps build them). */
    val text: String = "",
    val entities: List<TextFormat.Span> = emptyList(),
    val attachments: List<RemoteAttachment> = emptyList(),
    val replyToId: String? = null,
    val editedAtMs: Long = 0,
    val buttons: List<List<RemoteButton>> = emptyList(),
    val reactions: List<RemoteReaction> = emptyList(),
    /** A system/service line ("X joined", "pinned a message") rendered as a service message. */
    val isService: Boolean = false,
    val forwardedFrom: String? = null,
    val pinned: Boolean = false,
)

/** Something the user can send: text plus optional local files. */
data class OutgoingMessage(
    val text: String,
    val entities: List<TextFormat.Span>,
    val replyToId: String?,
    val files: List<OutgoingFile> = emptyList(),
)

data class OutgoingFile(val file: File, val mime: String, val fileName: String, val kind: AttachmentKind, val spoiler: Boolean = false)

/** Discord-style application command, surfaced through Telegram's bot command UI. */
data class RemoteCommand(
    val name: String,
    val description: String,
    val appName: String,
    val options: List<RemoteCommandOption> = emptyList(),
    /** Connector-private data needed to invoke the command (ids, versions...). */
    val payload: JSONObject = JSONObject(),
)

data class RemoteCommandOption(
    val name: String,
    val description: String,
    val type: Int,
    val required: Boolean,
    val choices: List<Pair<String, String>> = emptyList(),
)

enum class ConnectorState { DISCONNECTED, CONNECTING, CONNECTED, LOGGED_OUT, ERROR }

/** Events a connector reports to the bridge. All methods are thread-safe. */
interface BridgeSink {
    fun onChat(chat: RemoteChat)
    fun onMessage(message: RemoteMessage, live: Boolean = true)
    fun onEdit(message: RemoteMessage)
    fun onDelete(chatId: String, messageId: String)
    fun onReactions(chatId: String, messageId: String, reactions: List<RemoteReaction>)
    fun onTyping(chatId: String, user: RemoteUser)
    fun onRead(chatId: String, upToMessageId: String, byMe: Boolean)
    fun onUserUpdate(user: RemoteUser)
    fun onPresence(userId: String, online: Boolean, lastSeenMs: Long = 0)
    fun onState(state: ConnectorState, error: String? = null)
    /** Persist updated credentials / session state (e.g. rotated tokens, Signal keys). */
    fun saveSession(session: JSONObject)
}
