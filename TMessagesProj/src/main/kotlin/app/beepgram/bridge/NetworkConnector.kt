package app.beepgram.bridge

import org.json.JSONObject
import java.io.File

/**
 * A connector owns one network's protocol session and translates between that network and the
 * neutral [Remote] model. It never touches Telegram TL objects; [Bridge] does the TL mapping.
 *
 * Implementations report everything through the [BridgeSink] they are attached to and must be
 * safe to call from any thread. Long work (network I/O, downloads) happens on the connector's
 * own dispatcher, never on the caller's thread.
 */
interface NetworkConnector {
    val network: NetworkType
    val state: ConnectorState

    fun attach(sink: BridgeSink)

    /** Restore a persisted session (null = fresh login flow). */
    fun connect(session: JSONObject?)
    fun disconnect()

    /** Send and return the remote message id. */
    fun send(chatId: String, message: OutgoingMessage): String
    fun edit(chatId: String, messageId: String, message: OutgoingMessage)
    fun delete(chatId: String, messageId: String)
    fun react(chatId: String, messageId: String, emoji: String)
    fun markRead(chatId: String, upToMessageId: String)
    fun sendTyping(chatId: String)

    /** Resolve an attachment ref to a local file the Telegram layer can upload/read. */
    fun download(attachment: RemoteAttachment): File

    fun pressButton(chatId: String, messageId: String, button: RemoteButton)

    /** Application / slash commands available in a chat (Discord-style). Empty if unsupported. */
    fun commands(chatId: String): List<RemoteCommand> = emptyList()
    fun invokeCommand(chatId: String, command: RemoteCommand, args: Map<String, String>) {}

    /** Begin an interactive login; the sink receives prompts/state. */
    fun beginAuth()
    fun submitAuth(input: String)
}