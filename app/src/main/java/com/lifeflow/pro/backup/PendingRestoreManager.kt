package com.lifeflow.pro.backup

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

object PendingRestoreManager {
    private const val PENDING_RESTORE_FILE = "pending_restore.lfpbak"
    private val header = "LFPBK1".toByteArray()

    fun stageRestore(context: Context, rawBackupBytes: ByteArray) {
        val target = File(context.filesDir, PENDING_RESTORE_FILE)
        target.writeBytes(rawBackupBytes)
    }

    fun applyIfPending(context: Context, dbName: String): Boolean {
        val staged = File(context.filesDir, PENDING_RESTORE_FILE)
        if (!staged.exists()) return false

        val entries = readEntries(staged.readBytes())
        val dbBytes = entries["database/$dbName"] ?: return false
        val walBytes = entries["database/$dbName-wal"]
        val shmBytes = entries["database/$dbName-shm"]

        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.writeBytes(dbBytes)
        writeSidecar(dbFile, "-wal", walBytes)
        writeSidecar(dbFile, "-shm", shmBytes)
        staged.delete()
        return true
    }

    private fun readEntries(rawBytes: ByteArray): Map<String, ByteArray> {
        val zipBytes = if (rawBytes.size >= header.size && rawBytes.copyOfRange(0, header.size).contentEquals(header)) {
            rawBytes.copyOfRange(header.size, rawBytes.size)
        } else {
            rawBytes
        }
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun writeSidecar(dbFile: File, suffix: String, bytes: ByteArray?) {
        val file = File(dbFile.absolutePath + suffix)
        if (bytes == null) {
            if (file.exists()) file.delete()
        } else {
            file.writeBytes(bytes)
        }
    }
}
