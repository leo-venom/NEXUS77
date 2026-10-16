package com.leo.nexus77

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Páginas salvas em `Downloads/nexus77`.
 *
 * API 29+ usa MediaStore (sem permissão, arquivos do próprio app).
 * API < 29 grava direto na pasta pública (exige WRITE/READ_EXTERNAL_STORAGE).
 */
object Store {

    const val FOLDER = "nexus77"
    private const val MIME = "text/html"
    private const val EXT = ".html"

    data class Item(val name: String, val size: Long, val modified: Long)

    /** Na API antiga o app precisa pedir permissão de armazenamento. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 29 && context.checkSelfPermission(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED

    /** Normaliza o nome digitado pelo usuário num arquivo `.html` seguro. */
    fun fileName(raw: String): String {
        var s = raw.trim()
        if (s.lowercase(Locale.US).endsWith(EXT)) s = s.substring(0, s.length - EXT.length)
        s = s.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]+"), "-").trim().trim('.')
        if (s.isEmpty()) s = "pagina"
        if (s.length > 80) s = s.substring(0, 80)
        return s + EXT
    }

    /** Linha secundária do card: tamanho + data. */
    fun meta(item: Item): String =
        String.format(Locale.US, "%s · %s", sizeLabel(item.size), dateLabel(item.modified))

    private fun sizeLabel(bytes: Long): String = when {
        bytes >= 1048576 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun dateLabel(ms: Long): String =
        if (ms <= 0) "" else SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(ms))

    /** Grava o HTML (sobrescreve o mesmo nome). Retorna o nome final. */
    fun save(context: Context, name: String, html: String): String {
        val bytes = html.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= 29) saveMedia(context, name, bytes) else saveLegacy(name, bytes)
        return name
    }

    fun list(context: Context): List<Item> =
        if (Build.VERSION.SDK_INT >= 29) listMedia(context) else listLegacy()

    fun read(context: Context, name: String): String? =
        if (Build.VERSION.SDK_INT >= 29) readMedia(context, name) else readLegacy(name)

    /** Remove um arquivo salvo. true se algo foi removido. */
    fun delete(context: Context, name: String): Boolean =
        if (Build.VERSION.SDK_INT >= 29) deleteMedia(context, name) else deleteLegacy(name)

    // ---------- API 29+ (MediaStore) ----------

    private fun relativePath(): String = Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER + "/"

    private fun saveMedia(context: Context, name: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        // sobrescreve: apaga versões anteriores com o mesmo nome na pasta
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf(relativePath(), name),
            null
        )?.use { c ->
            val ids = ArrayList<Long>()
            while (c.moveToNext()) ids.add(c.getLong(0))
            ids.forEach { resolver.delete(ContentUris.withAppendedId(collection, it), null, null) }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, MIME)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath())
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore recusou o arquivo")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IOException("não foi possível abrir o arquivo para escrita")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun listMedia(context: Context): List<Item> {
        val out = ArrayList<Item>()
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            ),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/%", "%$EXT"),
            "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        )?.use { c ->
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
            while (c.moveToNext()) {
                out.add(Item(c.getString(nameIdx), c.getLong(sizeIdx), c.getLong(dateIdx) * 1000L))
            }
        }
        return out
    }

    private fun readMedia(context: Context, name: String): String? {
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf("${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/%", name),
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
                return context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
            }
        }
        return null
    }

    // ---------- API < 29 (arquivo direto) ----------

    @Suppress("DEPRECATION")
    private fun legacyDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)

    private fun saveLegacy(name: String, bytes: ByteArray) {
        val dir = legacyDir()
        if (!dir.exists() && !dir.mkdirs()) throw IOException("não foi possível criar a pasta")
        File(dir, name).writeBytes(bytes)
    }

    private fun listLegacy(): List<Item> {
        val files = legacyDir().listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.endsWith(EXT, ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .map { Item(it.name, it.length(), it.lastModified()) }
    }

    private fun readLegacy(name: String): String? {
        val f = File(legacyDir(), name)
        return if (f.isFile) f.readText() else null
    }

    private fun deleteMedia(context: Context, name: String): Boolean {
        var removed = false
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.DISPLAY_NAME}=?",
            arrayOf("${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/%", name),
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
                if (context.contentResolver.delete(uri, null, null) > 0) removed = true
            }
        }
        return removed
    }

    private fun deleteLegacy(name: String): Boolean {
        val f = File(legacyDir(), name)
        return f.isFile && f.delete()
    }
}
