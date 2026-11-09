package app.beepgram.bridge

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Allocates the local part of bridged ids ([BridgeIds.make]) and persists remote<->local maps
 * and connector sessions. The default implementation is file-backed JSON; a production store
 * can swap in SQLite without changing the contract.
 */
class BridgeStore(private val dir: File) {
    private val sequences = ConcurrentHashMap<Int, AtomicLong>()
    private val remoteToLocal = ConcurrentHashMap<String, Long>()
    private val localToRemote = ConcurrentHashMap<Long, String>()

    init { dir.mkdirs() }

    private fun seq(network: Int): AtomicLong =
        sequences.getOrPut(network) { AtomicLong(loadSeq(network)) }

    fun allocate(network: NetworkType, remoteId: String): Long {
        remoteToLocal[key(network, remoteId)]?.let { return it }
        val local = seq(network.id).incrementAndGet()
        val id = BridgeIds.make(network.id, local)
        remoteToLocal[key(network, remoteId)] = id
        localToRemote[id] = remoteId
        persistSeq(network.id, local)
        return id
    }

    fun local(network: NetworkType, remoteId: String): Long? = remoteToLocal[key(network, remoteId)]
    fun remote(id: Long): String? = localToRemote[id]

    fun saveSession(network: NetworkType, session: JSONObject) {
        File(dir, "session_${network.id}.json").writeText(session.toString())
    }

    fun loadSession(network: NetworkType): JSONObject? {
        val f = File(dir, "session_${network.id}.json")
        return if (f.exists()) JSONObject(f.readText()) else null
    }

    private fun key(network: NetworkType, remoteId: String) = "${network.id}:$remoteId"
    private fun seqFile(network: Int) = File(dir, "seq_$network.txt")
    private fun loadSeq(network: Int): Long = seqFile(network).takeIf { it.exists() }?.readText()?.toLongOrNull() ?: 0L
    private fun persistSeq(network: Int, value: Long) { seqFile(network).writeText(value.toString()) }
}