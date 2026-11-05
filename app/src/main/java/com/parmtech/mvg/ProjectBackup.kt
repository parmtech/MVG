package com.parmtech.mvg

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Owner-triggered, portable, add-only v1 project archive.
 * WARNING: This ZIP is not encrypted. Contains prompts, personal photos and videos.
 * OAuth tokens, API keys and local Android Keystore material are never included.
 *
 * The caller must run create/read on an IO executor, not the Compose main thread.
 * Read carefully validates all entries before persisting any StudioStore metadata.
 */
data class BackupAsset(val entryName: String, val source: File)
data class BackupSnapshot(val data: JSONObject, val assets: List<BackupAsset>)
data class BackupSummary(val projects: Int, val clips: Int, val movies: Int, val avatars: Int,
                         val files: Int, val bytes: Long)
data class BackupImport(
    val projects: List<FilmProject>,
    val clips: List<FilmClip>,
    val movies: List<FilmMovie>,
    val avatars: List<CharacterPack>,
    val messages: List<DirectorLine>,
    val drafts: Map<String, String>,
    val createdFiles: List<File>,
    val summary: BackupSummary
) {
    fun discardCreatedFiles() {
        createdFiles.forEach { file -> try { file.delete() } catch (_: Exception) {} }
    }
}

object ProjectBackup {
    const val MIME_TYPE = "application/zip"
    private const val SCHEMA = 1
    private const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024
    private const val MAX_ARCHIVE_FILES = 1000
    private const val MAX_ONE_FILE = 512L * 1024 * 1024
    private const val MAX_TOTAL_MEDIA = 2L * 1024 * 1024 * 1024
    private const val MAX_PROJECTS = 200
    private const val MAX_CLIPS = 1000
    private const val MAX_MOVIES = 300
    private const val MAX_AVATARS = 200
    private const val MAX_MESSAGES = 20000

    private fun sha256() = MessageDigest.getInstance("SHA-256")
    private fun hex(bytes: ByteArray) = bytes.joinToString("") {
        (it.toInt() and 255).toString(16).padStart(2, '0')
    }
    private fun entryIsValid(name: String): Boolean {
        val suffix = "[0-9a-fA-F-]{36}"
        return Regex("^media/(clips|movies)/" + suffix + "\\.mp4$").matches(name) ||
            Regex("^media/avatars/" + suffix + "\\.(jpg|png)$").matches(name)
    }

    fun create(context: Context, destination: Uri, snapshot: BackupSnapshot): BackupSummary {
        require(snapshot.assets.size <= MAX_ARCHIVE_FILES) { "Too many media files for portable backup" }
        val names = snapshot.assets.map { it.entryName }
        require(names.distinct().size == names.size && names.all(::entryIsValid)) {
            "Invalid or duplicate media paths in backup"
        }
        val files = JSONArray()
        var total = 0L
        val stream = requireNotNull(context.contentResolver.openOutputStream(destination, "w")) {
            "Cannot write selected backup destination"
        }
        ZipOutputStream(stream.buffered()).use { zip ->
            for (asset in snapshot.assets) {
                val src = asset.source
                require(src.isFile) { "Missing media: " + src.name }
                require(src.length() in 1..MAX_ONE_FILE) {
                    "Media is too large for portable backup: " + src.name
                }
                require(total + src.length() <= MAX_TOTAL_MEDIA) {
                    "Backup exceeds 2 GB media size limit"
                }
                val digest = sha256()
                var bytes = 0L
                zip.putNextEntry(ZipEntry(asset.entryName))
                src.inputStream().buffered().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        bytes += read
                        require(bytes <= MAX_ONE_FILE && total + bytes <= MAX_TOTAL_MEDIA) {
                            "Backup media grew beyond safe limits"
                        }
                        digest.update(buffer, 0, read)
                        zip.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
                total += bytes
                files.put(JSONObject().put("name", asset.entryName)
                    .put("size", bytes).put("sha256", hex(digest.digest())))
            }
            val manifest = JSONObject(snapshot.data.toString())
                .put("format", "MVG_PORTABLE_PROJECT_BACKUP")
                .put("schemaVersion", SCHEMA)
                .put("createdAt", System.currentTimeMillis())
                .put("files", files)
            val json = manifest.toString().toByteArray(Charsets.UTF_8)
            require(json.size <= MAX_MANIFEST_BYTES) { "Backup manifest too large" }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json)
            zip.closeEntry()
            zip.finish()
        }
        return BackupSummary(
            snapshot.data.optJSONArray("projects")?.length() ?: 0,
            snapshot.data.optJSONArray("clips")?.length() ?: 0,
            snapshot.data.optJSONArray("movies")?.length() ?: 0,
            snapshot.data.optJSONArray("avatars")?.length() ?: 0,
            snapshot.assets.size, total
        )
    }

    /**
     * Full validation, copy into new private file names, then return a prepared additive
     * import for StudioStore.commitImport on main thread. Never overwrite existing data.
     */
    fun read(context: Context, source: Uri): BackupImport {
        val staging = File(context.cacheDir, "backup_stage_" + UUID.randomUUID())
        require(staging.mkdirs()) { "Could not prepare temporary backup storage" }
        val created = mutableListOf<File>()
        try {
            val extracted = linkedMapOf<String, Pair<File, Pair<Long, String>>>()
            var manifestBytes: ByteArray? = null
            var total = 0L
            val seen = hashSetOf<String>()
            val input = requireNotNull(context.contentResolver.openInputStream(source)) {
                "Cannot read selected backup"
            }
            ZipInputStream(input.buffered()).use { zip ->
                var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count++
                    require(count <= MAX_ARCHIVE_FILES + 1) { "Too many ZIP entries" }
                    require(!entry.isDirectory && seen.add(entry.name)) {
                        "Duplicate or directory ZIP entry"
                    }
                    if (entry.name == "manifest.json") {
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(16384)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            require(out.size() + n <= MAX_MANIFEST_BYTES) {
                                "Oversized backup manifest"
                            }
                            out.write(buffer, 0, n)
                        }
                        manifestBytes = out.toByteArray()
                    } else {
                        require(entryIsValid(entry.name)) { "Unsafe or unexpected ZIP entry" }
                        val dest = File(staging, "part_" + count)
                        val digest = sha256()
                        var bytes = 0L
                        FileOutputStream(dest).use { sink ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                bytes += n
                                total += n
                                require(bytes <= MAX_ONE_FILE && total <= MAX_TOTAL_MEDIA) {
                                    "ZIP media exceeds maximum safe extraction size"
                                }
                                digest.update(buffer, 0, n)
                                sink.write(buffer, 0, n)
                            }
                        }
                        extracted[entry.name] = Pair(dest, Pair(bytes, hex(digest.digest())))
                    }
                    zip.closeEntry()
                }
            }
            val manifest = JSONObject(String(requireNotNull(manifestBytes) {
                "Not an MVG project backup: manifest missing"
            }, Charsets.UTF_8))
            require(manifest.optString("format") == "MVG_PORTABLE_PROJECT_BACKUP" &&
                manifest.optInt("schemaVersion") == SCHEMA) {
                "Unsupported MVG backup format or schema"
            }
            val fileList = requiredArray(manifest, "files", MAX_ARCHIVE_FILES)
            val expected = hashSetOf<String>()
            for (i in 0 until fileList.length()) {
                val info = requireNotNull(fileList.optJSONObject(i)) { "Invalid file record" }
                val name = info.optString("name")
                require(entryIsValid(name) && expected.add(name)) { "Unsafe media manifest entry" }
                val found = requireNotNull(extracted[name]) { "Backup media is missing" }
                require(found.second.first == info.optLong("size") &&
                    found.second.second.equals(info.optString("sha256"), ignoreCase = true)) {
                    "Backup media failed size/integrity verification"
                }
            }
            require(extracted.keys == expected) { "Unexpected media not referenced by backup" }
            val projectJson = requiredArray(manifest, "projects", MAX_PROJECTS)
            val clipJson = requiredArray(manifest, "clips", MAX_CLIPS)
            val movieJson = requiredArray(manifest, "movies", MAX_MOVIES)
            val avatarJson = requiredArray(manifest, "avatars", MAX_AVATARS)
            val messageJson = requiredArray(manifest, "messages", MAX_MESSAGES)
            val draftJson = requireNotNull(manifest.optJSONObject("drafts")) {
                "Missing project drafts"
            }
            val projectIds = hashMapOf<String, String>()
            val importedProjects = mutableListOf<FilmProject>()
            for (i in 0 until projectJson.length()) {
                val record = requireNotNull(projectJson.optJSONObject(i)) { "Invalid project" }
                val oldId = record.optString("id")
                require(oldId.isNotBlank() && !projectIds.containsKey(oldId)) {
                    "Duplicate or missing project ID"
                }
                val newId = UUID.randomUUID().toString()
                projectIds[oldId] = newId
                importedProjects += FilmProject(
                    id = newId,
                    title = (record.optString("title").take(120).ifBlank { "Restored project" }) +
                        " (restored)",
                    createdAt = record.optLong("createdAt", System.currentTimeMillis())
                )
            }
            val copiedNames = hashSetOf<String>()
            fun restoreMedia(entryName: String, folder: String, baseName: String): String {
                if (entryName.isBlank()) return ""
                require(entryName in expected && copiedNames.add(entryName)) {
                    "Media item is missing or referenced twice"
                }
                val extension = entryName.substringAfterLast('.')
                val outFolder = File(context.filesDir, folder)
                require(outFolder.mkdirs() || outFolder.isDirectory) {
                    "Could not create destination media folder"
                }
                val out = File(outFolder, baseName + "." + extension)
                require(!out.exists()) { "Generated destination already exists" }
                // Track the target BEFORE copy so a partially written file is cleaned
                // if the storage provider fails during an otherwise valid import.
                created += out
                requireNotNull(extracted[entryName]).first.copyTo(out, overwrite = false)
                return out.absolutePath
            }
            val importedClips = mutableListOf<FilmClip>()
            for (i in 0 until clipJson.length()) {
                val record = requireNotNull(clipJson.optJSONObject(i)) { "Invalid clip" }
                val project = requireNotNull(projectIds[record.optString("projectId")]) {
                    "Orphan clip project ID"
                }
                val newId = UUID.randomUUID().toString()
                val media = record.optString("backupMedia")
                val path = restoreMedia(media, "media", "clip_" + newId)
                val operation = record.optString("operation").take(500)
                val remote = record.optString("remoteUri").take(2048)
                val oldStatus = record.optString("status")
                val state = if (path.isNotBlank()) "Complete"
                    else if (operation.isNotBlank() || remote.isNotBlank()) "Check again"
                    else if (oldStatus == "Failed") "Failed" else "Needs review"
                importedClips += FilmClip(
                    id = newId, projectId = project,
                    title = record.optString("title").take(150),
                    prompt = record.optString("prompt").take(5000),
                    modelId = record.optString("modelId").take(120),
                    duration = record.optInt("duration", 8),
                    ratio = record.optString("ratio", "16:9").take(10),
                    resolution = record.optString("resolution", "720p").take(10),
                    status = state, operation = operation,
                    remoteUri = remote, localPath = path,
                    error = if (path.isBlank()) "Restored metadata; reconnect Google Cloud to recover any cloud output"
                        else "",
                    createdAt = record.optLong("createdAt", System.currentTimeMillis())
                )
            }
            val importedMovies = mutableListOf<FilmMovie>()
            for (i in 0 until movieJson.length()) {
                val record = requireNotNull(movieJson.optJSONObject(i)) { "Invalid movie" }
                val project = requireNotNull(projectIds[record.optString("projectId")]) {
                    "Orphan movie project ID"
                }
                val newId = UUID.randomUUID().toString()
                val path = restoreMedia(record.optString("backupMedia"),
                    "media/exports", "MVG_restored_" + newId)
                if (path.isNotBlank()) {
                    importedMovies += FilmMovie(
                        id = newId, projectId = project, filePath = path,
                        sceneCount = record.optInt("sceneCount").coerceIn(0, 1000),
                        createdAt = record.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
            val importedAvatars = mutableListOf<CharacterPack>()
            for (i in 0 until avatarJson.length()) {
                val record = requireNotNull(avatarJson.optJSONObject(i)) { "Invalid avatar" }
                val newId = UUID.randomUUID().toString()
                val path = restoreMedia(record.optString("backupMedia"),
                    "media", "avatar_" + newId)
                if (path.isNotBlank()) {
                    importedAvatars += CharacterPack(
                        id = newId,
                        nickname = record.optString("nickname").take(80).ifBlank { "Restored photo" },
                        photoPath = path,
                        createdAt = record.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            }
            require(copiedNames == expected) { "Backup manifest has unreferenced media" }
            val importedMessages = mutableListOf<DirectorLine>()
            for (i in 0 until messageJson.length()) {
                val record = requireNotNull(messageJson.optJSONObject(i)) { "Invalid message" }
                val project = requireNotNull(projectIds[record.optString("projectId")]) {
                    "Orphan AI Director message"
                }
                importedMessages += DirectorLine(
                    id = UUID.randomUUID().toString(), projectId = project,
                    role = record.optString("role").take(40),
                    text = record.optString("text").take(12000),
                    createdAt = record.optLong("createdAt", System.currentTimeMillis())
                )
            }
            val drafts = hashMapOf<String, String>()
            for ((oldId, newId) in projectIds) {
                drafts[newId] = draftJson.optString(oldId).take(5000)
            }
            return BackupImport(
                projects = importedProjects, clips = importedClips, movies = importedMovies,
                avatars = importedAvatars, messages = importedMessages, drafts = drafts,
                createdFiles = created.toList(),
                summary = BackupSummary(importedProjects.size, importedClips.size,
                    importedMovies.size, importedAvatars.size, extracted.size, total)
            )
        } catch (e: Exception) {
            created.forEach { try { it.delete() } catch (_: Exception) {} }
            throw e
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun requiredArray(data: JSONObject, name: String, maximum: Int): JSONArray {
        val array = requireNotNull(data.optJSONArray(name)) { "Missing archive section: " + name }
        require(array.length() <= maximum) { "Backup contains too many " + name }
        return array
    }
}
