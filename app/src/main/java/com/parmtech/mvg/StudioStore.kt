package com.parmtech.mvg

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

data class FilmProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
data class FilmClip(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val prompt: String,
    val modelId: String,
    val duration: Int,
    val ratio: String,
    val resolution: String,
    val status: String = "Draft",
    val operation: String = "",
    val remoteUri: String = "",
    val localPath: String = "",
    val error: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
data class FilmMovie(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val filePath: String,
    val sceneCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
data class CharacterPack(
    val id: String = UUID.randomUUID().toString(),
    val nickname: String,
    val photoPath: String,
    val createdAt: Long = System.currentTimeMillis()
)
data class DirectorLine(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val role: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
data class CloudConfig(
    val projectId: String = "",
    val region: String = "us-central1",
    val outputBucket: String = "",
    val authMode: String = "GOOGLE"
)
data class VideoOptions(
    val model: String = "veo-3.1-fast-generate-001",
    val duration: Int = 8,
    val ratio: String = "16:9",
    val resolution: String = "720p",
    val audio: Boolean = true,
    val firstFramePath: String = ""
)

private fun FilmProject.asJson() = JSONObject().put("id", id).put("title", title).put("createdAt", createdAt)
private fun FilmClip.asJson() = JSONObject().put("id", id).put("projectId", projectId)
    .put("title", title).put("prompt", prompt).put("modelId", modelId).put("duration", duration)
    .put("ratio", ratio).put("resolution", resolution).put("status", status).put("operation", operation)
    .put("remoteUri", remoteUri).put("localPath", localPath).put("error", error).put("createdAt", createdAt)
private fun FilmMovie.asJson() = JSONObject().put("id", id)
    .put("projectId", projectId).put("filePath", filePath)
    .put("sceneCount", sceneCount).put("createdAt", createdAt)
private fun CharacterPack.asJson() = JSONObject().put("id", id).put("nickname", nickname).put("photoPath", photoPath).put("createdAt", createdAt)
private fun DirectorLine.asJson() = JSONObject().put("id", id).put("projectId", projectId)
    .put("role", role).put("text", text).put("createdAt", createdAt)

/** Local-first v0.1 data. An additive Room migration is planned for v0.2. */
class StudioStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("mvg_studio_v1", Context.MODE_PRIVATE)
    val projects = mutableStateListOf<FilmProject>()
    val clips = mutableStateListOf<FilmClip>()
    val movies = mutableStateListOf<FilmMovie>()
    var movieExportRunning by mutableStateOf(false)
    var movieExportProgress by mutableStateOf(0)
    var movieExportError by mutableStateOf("")
    val avatars = mutableStateListOf<CharacterPack>()
    val messages = mutableStateListOf<DirectorLine>()
    var config by mutableStateOf(CloudConfig())
        private set
    /** Ephemeral on-screen feedback; never contains access tokens. */
    var googleAuthorizationStatus by mutableStateOf("")
    var credentialRevision by mutableStateOf(0)
        private set
    val vault = TokenVault(context)
    val apiKeyVault = TokenVault(context, "api_key")

    fun activeCredential(): String =
        if (config.authMode == "API_KEY") apiKeyVault.read()
        else if (isGoogleAuthorizationFresh()) vault.read() else ""

    // Google Identity Services gives a short-lived access token, not a permanent login.
    // A 45-minute local freshness bound is deliberately shorter than typical token lifetime.
    // After expiry the owner taps Continue with Google again; no hidden paid call or refresh.
    fun isGoogleAuthorizationFresh(): Boolean {
        val issued = prefs.getLong("google_auth_issued_ms", 0L)
        val age = System.currentTimeMillis() - issued
        return issued > 0L && age in 0L..(45L * 60L * 1000L) &&
            vault.read().isNotBlank()
    }

    fun saveGoogleAuthorization(token: String) {
        require(token.isNotBlank()) { "Google authorization returned an empty token" }
        vault.save(token)
        check(prefs.edit().putLong("google_auth_issued_ms", System.currentTimeMillis()).commit()) {
            "Google authorization state not saved"
        }
        credentialsChanged()
    }

    fun clearGoogleAuthorization() {
        vault.clear()
        prefs.edit().remove("google_auth_issued_ms").apply()
        credentialsChanged()
    }

    fun credentialsChanged() { credentialRevision++ }

    companion object { private val CLIPS_LOCK = Any() }
    private var clipSnapshot: String = ""
    init { load(); clipSnapshot = prefs.getString("clips", "[]") ?: "[]" }
    private fun array(key: String) = try { JSONArray(prefs.getString(key, "[]") ?: "[]") }
        catch (_: Exception) { JSONArray() }

    private fun load() {
        val p = array("projects")
        for (i in 0 until p.length()) {
            val o = p.optJSONObject(i) ?: continue
            projects.add(FilmProject(o.optString("id"), o.optString("title"), o.optLong("createdAt")))
        }
        val c = array("clips")
        for (i in 0 until c.length()) {
            val o = c.optJSONObject(i) ?: continue
            clips.add(FilmClip(
                id = o.optString("id"), projectId = o.optString("projectId"), title = o.optString("title"),
                prompt = o.optString("prompt"), modelId = o.optString("modelId"), duration = o.optInt("duration", 8),
                ratio = o.optString("ratio", "16:9"), resolution = o.optString("resolution", "720p"),
                status = o.optString("status", "Draft"), operation = o.optString("operation"),
                remoteUri = o.optString("remoteUri"), localPath = o.optString("localPath"),
                error = o.optString("error"), createdAt = o.optLong("createdAt")
            ))
        }
        val savedMovies = array("movies")
        for (i in 0 until savedMovies.length()) {
            val item = savedMovies.optJSONObject(i) ?: continue
            movies.add(FilmMovie(
                id = item.optString("id"), projectId = item.optString("projectId"),
                filePath = item.optString("filePath"),
                sceneCount = item.optInt("sceneCount"),
                createdAt = item.optLong("createdAt")
            ))
        }
        val a = array("avatars")
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            avatars.add(CharacterPack(o.optString("id"), o.optString("nickname"), o.optString("photoPath"), o.optLong("createdAt")))
        }
        val m = array("messages")
        for (i in 0 until m.length()) {
            val o = m.optJSONObject(i) ?: continue
            messages.add(DirectorLine(o.optString("id"), o.optString("projectId"), o.optString("role"), o.optString("text"), o.optLong("createdAt")))
        }
        config = CloudConfig(
            prefs.getString("cloudProject", "") ?: "",
            prefs.getString("region", "us-central1") ?: "us-central1",
            prefs.getString("bucket", "") ?: "",
            prefs.getString("authMode", "GOOGLE") ?: "GOOGLE"
        )
    }

    // Different instances (Activity and Worker) may persist different record types.
    // Write only changed keys to avoid overwriting another component's newer data.

    @Synchronized
    private fun save(vararg changed: String) {
        fun <T> json(list: List<T>, toJson: (T) -> JSONObject): String {
            val result = JSONArray()
            list.forEach { result.put(toJson(it)) }
            return result.toString()
        }
        val edit = prefs.edit()
        for (key in changed) when (key) {
            "projects" -> edit.putString("projects", json(projects) { it.asJson() })
            "clips" -> {
                val encoded = json(clips) { it.asJson() }
                edit.putString("clips", encoded)
                clipSnapshot = encoded
            }
            "avatars" -> edit.putString("avatars", json(avatars) { it.asJson() })
            "movies" -> edit.putString("movies", json(movies) { it.asJson() })
            "messages" -> edit.putString("messages", json(messages) { it.asJson() })
            "config" -> edit.putString("cloudProject", config.projectId)
                .putString("region", config.region).putString("bucket", config.outputBucket)
                .putString("authMode", config.authMode)
        }
        check(edit.commit()) { "Could not save project data" }
    }

    @Synchronized
    fun refreshClipsFromDisk() {
        val json = prefs.getString("clips", "[]") ?: "[]"
        if (clipSnapshot == json) return
        clipSnapshot = json
        val latest = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        val parsed = mutableListOf<FilmClip>()
        for (i in 0 until latest.length()) {
            val o = latest.optJSONObject(i) ?: continue
            parsed.add(FilmClip(
                id = o.optString("id"), projectId = o.optString("projectId"), title = o.optString("title"),
                prompt = o.optString("prompt"), modelId = o.optString("modelId"), duration = o.optInt("duration", 8),
                ratio = o.optString("ratio", "16:9"), resolution = o.optString("resolution", "720p"),
                status = o.optString("status", "Draft"), operation = o.optString("operation"),
                remoteUri = o.optString("remoteUri"), localPath = o.optString("localPath"),
                error = o.optString("error"), createdAt = o.optLong("createdAt")
            ))
        }
        clips.clear()
        clips.addAll(parsed)
    }

    fun getClipById(id: String): FilmClip? {
        refreshClipsFromDisk()
        return clips.firstOrNull { it.id == id }
    }

    fun updateConfig(value: CloudConfig) { config = value; save("config") }
    fun getDraft(projectId: String): String = prefs.getString("draft_" + projectId, "") ?: ""
    fun saveDraft(projectId: String, draft: String) {
        // Small text autosave; never include credentials or media in this field.
        prefs.edit().putString("draft_" + projectId, draft.take(5000)).apply()
    }
    fun createProject(title: String): FilmProject =
        FilmProject(title = title.trim()).also { projects.add(0, it); save("projects") }
    fun removeProject(id: String) {
        refreshClipsFromDisk()
        projects.removeAll { it.id == id }
        clips.removeAll { it.projectId == id }
        messages.removeAll { it.projectId == id }
        movies.filter { it.projectId == id }.forEach { deleteMovieFile(it.filePath) }
        movies.removeAll { it.projectId == id }
        prefs.edit().remove("draft_" + id).apply()
        save("projects", "clips", "messages", "movies")
    }
    fun addClip(clip: FilmClip) {
        synchronized(CLIPS_LOCK) {
            refreshClipsFromDisk()
            clips.add(0, clip)
            save("clips")
        }
    }
    fun updateClip(clip: FilmClip) {
        synchronized(CLIPS_LOCK) {
            refreshClipsFromDisk()
            val i = clips.indexOfFirst { it.id == clip.id }
            if (i >= 0) { clips[i] = clip; save("clips") }
        }
    }
    fun moveClip(projectId: String, clipId: String, up: Boolean) {
        refreshClipsFromDisk()
        val items = clips.filter { it.projectId == projectId }
        val idx = items.indexOfFirst { it.id == clipId }
        val other = if (up) idx - 1 else idx + 1
        if (idx < 0 || other !in items.indices) return
        val left = clips.indexOfFirst { it.id == items[idx].id }
        val right = clips.indexOfFirst { it.id == items[other].id }
        if (left < 0 || right < 0) return
        val tmp = clips[left]
        clips[left] = clips[right]
        clips[right] = tmp
        save("clips")
    }
    fun addAvatar(avatar: CharacterPack) { avatars.add(0, avatar); save("avatars") }
    fun removeAvatar(id: String) {
        val a = avatars.firstOrNull { it.id == id } ?: return
        avatars.remove(a)
        File(a.photoPath).takeIf { it.isFile }?.delete()
        save("avatars")
    }
    fun addLine(line: DirectorLine) { messages.add(line); save("messages") }

    fun addMovie(movie: FilmMovie) {
        check(movies.none { it.id == movie.id }) { "Movie already saved" }
        require(isPrivateMovieFile(movie.filePath)) { "Invalid export location" }
        movies.add(0, movie)
        save("movies")
    }

    fun removeMovie(id: String) {
        val found = movies.firstOrNull { it.id == id } ?: return
        if (deleteMovieFile(found.filePath)) {
            movies.remove(found)
            save("movies")
        }
    }

    private fun isPrivateMovieFile(filePath: String): Boolean {
        val expectedFolder = File(context.filesDir, "media/exports").canonicalFile
        val candidate = File(filePath).canonicalFile
        return candidate.parentFile == expectedFolder && candidate.extension == "mp4"
    }

    private fun deleteMovieFile(filePath: String): Boolean {
        if (!isPrivateMovieFile(filePath)) return false
        val file = File(filePath)
        return !file.exists() || file.delete()
    }

    fun importImage(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        require(mime == "image/jpeg" || mime == "image/png") {
            "For Vertex references, select a JPEG or PNG photo"
        }
        val extension = if (mime == "image/png") ".png" else ".jpg"
        val dest = File(File(context.filesDir, "media"), "avatar_" + UUID.randomUUID().toString() + extension)
        dest.parentFile?.mkdirs()
        try {
            context.contentResolver.openInputStream(uri).use { source ->
                requireNotNull(source) { "Could not read selected photo" }
                dest.outputStream().use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 20L * 1024 * 1024) { "Photo exceeds 20 MB" }
                        target.write(buffer, 0, n)
                    }
                }
            }
        } catch (e: Exception) { dest.delete(); throw e }
        require(dest.length() > 0) { "Empty image" }
        return dest.absolutePath
    }
    fun videoOutputFile(id: String) =
        File(File(context.filesDir, "media"), "clip_" + id + ".mp4").also { it.parentFile?.mkdirs() }
}

/** A local Keystore-protected bearer token; no service-account JSON inside the APK. */
class TokenVault(context: Context, storageSuffix: String = "") {
    private val prefs = context.getSharedPreferences(
        if (storageSuffix.isBlank()) "mvg_token_store" else "mvg_token_store_" + storageSuffix,
        Context.MODE_PRIVATE
    )
    private val alias = if (storageSuffix.isBlank()) "mvg_bearer_v1" else "mvg_secret_" + storageSuffix + "_v1"
    private val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun secretKey(): java.security.Key {
        if (!store.containsAlias(alias)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generator.generateKey()
        }
        return requireNotNull(store.getKey(alias, null))
    }
    fun save(token: String) {
        if (token.isBlank()) { clear(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val data = cipher.iv + cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("encrypted", Base64.encodeToString(data, Base64.NO_WRAP)).apply()
    }
    fun read(): String {
        val saved = prefs.getString("encrypted", null) ?: return ""
        return try {
            val bytes = Base64.decode(saved, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }
    fun clear() { prefs.edit().remove("encrypted").apply() }
}
