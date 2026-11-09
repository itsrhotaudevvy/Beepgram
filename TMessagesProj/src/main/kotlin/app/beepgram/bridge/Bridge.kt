package app.beepgram.bridge

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the connectors and the [BridgeStore], and acts as the [BridgeSink] they report into.
 * It assigns bridged ids and forwards neutral events to a [Listener]; the Telegram layer
 * implements [Listener] and performs the actual TL / MessagesStorage mapping.
 *
 * This is a framework skeleton: live connectors are developed in Reefgram. The TL conversion
 * and dialog injection into Telegram's MessagesController are intentionally left to the
 * integrating layer so this module stays network- and TL-agnostic.
 */
class Bridge(dir: File, private val listener: Listener) : BridgeSink {
    private val store = BridgeStore(dir)
    private val connectors = ConcurrentHashMap<Int, NetworkConnector>()

    interface Listener {
        fun onChat(network: NetworkType, chat: RemoteChat, localId: Long)
        fun onMessage(network: NetworkType, message: RemoteMessage, localChatId: Long, live: Boolean)
        fun onEdit(network: NetworkType, message: RemoteMessage, localChatId: Long)
        fun onDelete(network: NetworkType, localChatId: Long, messageId: String)
        fun onState(network: NetworkType, state: ConnectorState, error: String?)
    }

    fun register(connector: NetworkConnector) {
        connectors[connector.network.id] = connector
        connector.attach(sinkFor(connector.network))
    }

    fun connector(network: NetworkType): NetworkConnector? = connectors[network.id]

    fun startAll() {
        for (c in connectors.values) c.connect(store.loadSession(c.network))
    }

    fun stopAll() {
        for (c in connectors.values) c.disconnect()
    }

    fun send(network: NetworkType, chatId: String, message: OutgoingMessage): String? =
        connectors[network.id]?.send(chatId, message)

    override fun onChat(chat: RemoteChat) {
        // Network is resolved by the reporting connector via the sink wrapper below.
    }

    override fun onMessage(message: RemoteMessage, live: Boolean) {}
    override fun onEdit(message: RemoteMessage) {}
    override fun onDelete(chatId: String, messageId: String) {}
    override fun onReactions(chatId: String, messageId: String, reactions: List<RemoteReaction>) {}
    override fun onTyping(chatId: String, user: RemoteUser) {}
    override fun onRead(chatId: String, upToMessageId: String, byMe: Boolean) {}
    override fun onUserUpdate(user: RemoteUser) {}
    override fun onPresence(userId: String, online: Boolean, lastSeenMs: Long) {}
    override fun onState(state: ConnectorState, error: String?) {}
    override fun saveSession(session: JSONObject) {}

    /** Wrap the sink so events carry their originating network without threading it everywhere. */
    fun sinkFor(network: NetworkType): BridgeSink = object : BridgeSink by this {
        override fun onChat(chat: RemoteChat) {
            listener.onChat(network, chat, store.allocate(network, chat.id))
        }

        override fun onMessage(message: RemoteMessage, live: Boolean) {
            val localChatId = store.local(network, message.chatId) ?: store.allocate(network, message.chatId)
            listener.onMessage(network, message, localChatId, live)
        }

        override fun onEdit(message: RemoteMessage) {
            val localChatId = store.local(network, message.chatId) ?: return
            listener.onEdit(network, message, localChatId)
        }

        override fun onDelete(chatId: String, messageId: String) {
            val localChatId = store.local(network, chatId) ?: return
            listener.onDelete(network, localChatId, messageId)
        }

        override fun onState(state: ConnectorState, error: String?) {
            listener.onState(network, state, error)
        }

        override fun saveSession(session: JSONObject) {
            store.saveSession(network, session)
        }
    }

    /** Connectors attach to a network-aware sink at registration time. */
    fun store(): BridgeStore = store
}