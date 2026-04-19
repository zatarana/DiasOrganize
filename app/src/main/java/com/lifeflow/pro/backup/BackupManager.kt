package com.lifeflow.pro.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lifeflow.pro.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {
    private val dbName = "lifeflow_pro.db"
    private val header = "LFPBK1".toByteArray()

    suspend fun exportBackup(targetUri: Uri): BackupPreview {
        checkpointDatabase()
        val preview = currentPreview(sourceName = DocumentFile.fromSingleUri(context, targetUri)?.name)
        val dbFile = context.getDatabasePath(dbName)
        val dbBytes = dbFile.readBytes()
        val walBytes = sidecarBytes("-wal")
        val shmBytes = sidecarBytes("-shm")

        val payload = ByteArrayOutputStream()
        payload.write(header)
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadataJson(preview).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("database/$dbName"))
            zip.write(dbBytes)
            zip.closeEntry()

            walBytes?.let {
                zip.putNextEntry(ZipEntry("database/$dbName-wal"))
                zip.write(it)
                zip.closeEntry()
            }
            shmBytes?.let {
                zip.putNextEntry(ZipEntry("database/$dbName-shm"))
                zip.write(it)
                zip.closeEntry()
            }
        }

        context.contentResolver.openOutputStream(targetUri, "w")!!.use { output ->
            output.write(payload.toByteArray())
        }
        return preview
    }

    suspend fun previewBackup(sourceUri: Uri): BackupPreview {
        val entries = readEntries(sourceUri)
        val metadata = entries["metadata.json"]?.decodeToString() ?: error("Backup sem metadados")
        val preview = parseMetadata(metadata, DocumentFile.fromSingleUri(context, sourceUri)?.name)
        validateChecksum(entries, preview.checksum)
        return preview
    }

    suspend fun restoreBackup(sourceUri: Uri): BackupPreview {
        val rawBytes = context.contentResolver.openInputStream(sourceUri)!!.use { input -> input.readBytes() }
        val entries = readEntries(rawBytes)
        val metadata = entries["metadata.json"]?.decodeToString() ?: error("Backup sem metadados")
        val preview = parseMetadata(metadata, DocumentFile.fromSingleUri(context, sourceUri)?.name)
        validateChecksum(entries, preview.checksum)
        PendingRestoreManager.stageRestore(context, rawBytes)
        return preview
    }

    suspend fun exportAutomaticBackupToTree(treeUri: Uri): BackupPreview {
        val parent = DocumentFile.fromTreeUri(context, treeUri) ?: error("Pasta de backup inválida")
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.US)
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val file = parent.createFile("application/octet-stream", "lifeflow_auto_$timestamp.lfpbak")
            ?: error("Não foi possível criar o arquivo de backup automático")
        return exportBackup(file.uri)
    }

    suspend fun currentPreview(sourceName: String? = null): BackupPreview {
        checkpointDatabase()
        val taskCount = database.taskDao().getAllOnce().size
        val transactionCount = database.transactionDao().getAll().size
        val debtCount = database.debtDao().getAll().size
        val dbBytes = context.getDatabasePath(dbName).takeIf(File::exists)?.readBytes() ?: ByteArray(0)
        return BackupPreview(
            createdAtEpochMillis = System.currentTimeMillis(),
            schemaVersion = 1,
            taskCount = taskCount,
            transactionCount = transactionCount,
            debtCount = debtCount,
            checksum = sha256(dbBytes),
            sourceName = sourceName,
        )
    }

    private fun metadataJson(preview: BackupPreview): String = JSONObject().apply {
        put("createdAtEpochMillis", preview.createdAtEpochMillis)
        put("schemaVersion", preview.schemaVersion)
        put("taskCount", preview.taskCount)
        put("transactionCount", preview.transactionCount)
        put("debtCount", preview.debtCount)
        put("checksum", preview.checksum)
    }.toString()

    private fun parseMetadata(json: String, sourceName: String?): BackupPreview {
        val objectJson = JSONObject(json)
        return BackupPreview(
            createdAtEpochMillis = objectJson.getLong("createdAtEpochMillis"),
            schemaVersion = objectJson.getInt("schemaVersion"),
            taskCount = objectJson.getInt("taskCount"),
            transactionCount = objectJson.getInt("transactionCount"),
            debtCount = objectJson.getInt("debtCount"),
            checksum = objectJson.getString("checksum"),
            sourceName = sourceName,
        )
    }

    private suspend fun checkpointDatabase() {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        }
    }

    private fun sidecarBytes(suffix: String): ByteArray? {
        val file = File(context.getDatabasePath(dbName).absolutePath + suffix)
        return if (file.exists()) file.readBytes() else null
    }

    private fun readEntries(sourceUri: Uri): Map<String, ByteArray> {
        val rawBytes = context.contentResolver.openInputStream(sourceUri)!!.use { input -> input.readBytes() }
        return readEntries(rawBytes)
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

    private fun validateChecksum(entries: Map<String, ByteArray>, expectedChecksum: String) {
        val dbBytes = entries["database/$dbName"] ?: error("Arquivo principal do banco não encontrado no backup")
        val actual = sha256(dbBytes)
        check(actual == expectedChecksum) { "Checksum inválido. Esperado $expectedChecksum e obtido $actual" }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { "%02x".format(it) }
}
