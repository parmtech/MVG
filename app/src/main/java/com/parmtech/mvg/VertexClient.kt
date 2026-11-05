package com.parmtech.mvg

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class OperationStatus(
    val done: Boolean,
    val remoteUri: String = "",
    val base64Video: String = "",
    val error: String = ""
)

/**
 * Real Vertex REST transport; needs an owner-provided, short-lived OAuth 2.0 access token.
 * No service-account key is embedded. No retry of the billable submit method.
 */
object VertexClient {
    val VIDEO_MODELS = listOf(
        "veo-3.1-generate-001",
        "veo-3.1-fast-generate-001",
        "veo-3.1-lite-generate-001"
    )
    private const val CHAT_MODEL = "gemini-2.5-flash"

    /**
     * Vertex Veo's predictLongRunning operation requires principal-based authentication.
     * Cloud standard API keys cover generateContent but not this endpoint.
     */
    fun validate(config: CloudConfig, credential: String, video: Boolean = true) {
        require(config.projectId.matches(Regex("[a-z][a-z0-9-]{4,62}"))) {
            "Enter a valid Google Cloud Project ID under Connection Settings"
        }
        require(config.region == "us-central1") {
            "Select us-central1 for the currently integrated Veo model."
        }
        require(config.authMode == "GOOGLE" || config.authMode == "API_KEY") {
            "Choose Google Login or API Key in Settings."
        }
        require(credential.isNotBlank()) {
            if (config.authMode == "GOOGLE") "Tap Continue with Google to connect."
            else "Enter an API key and save it in Settings."
        }
        if (video) require(config.authMode == "GOOGLE") {
            "Veo video generation requires Google Login; API Key mode supports Gemini AI Director only."
        }
    }

    private fun route(config: CloudConfig, model: String): String {
        require(model in VIDEO_MODELS) { "Unverified model not supported in this build" }
        val project = config.projectId
        val region = config.region
        return "https://" + region + "-aiplatform.googleapis.com/v1/projects/" +
            project + "/locations/" + region + "/publishers/google/models/" + model
    }

    private fun call(url: String, credential: String, json: JSONObject, apiKey: Boolean = false): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 90000
            connection.doOutput = true
            if (apiKey) connection.setRequestProperty("x-goog-api-key", credential)
            else connection.setRequestProperty("Authorization", "Bearer " + credential)
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val reason = try {
                    val e = JSONObject(response).optJSONObject("error")
                    e?.optString("message") ?: response.take(360)
                } catch (_: Exception) { response.take(360) }
                throw IllegalStateException("Vertex HTTP " + code + ": " + reason.take(360))
            }
            return JSONObject(response)
        } finally { connection.disconnect() }
    }

    fun startVideo(
        config: CloudConfig,
        token: String,
        prompt: String,
        options: VideoOptions
    ): String {
        validate(config, token)
        require(prompt.isNotBlank()) { "Write a prompt" }
        val issues = ModelGuard.validate(options)
        require(issues.isEmpty()) { issues.joinToString("; ") }
        val parameters = JSONObject()
            .put("durationSeconds", options.duration)
            .put("aspectRatio", options.ratio)
            .put("resolution", options.resolution)
            .put("sampleCount", 1)
            .put("generateAudio", options.audio)
        if (config.outputBucket.isNotBlank()) {
            require(config.outputBucket.startsWith("gs://")) { "Cloud Storage URI must start with gs://" }
            parameters.put("storageUri", config.outputBucket.trimEnd('/') + "/")
        }
        val instance = JSONObject().put("prompt", prompt)
        if (options.firstFramePath.isNotBlank()) {
            val file = File(options.firstFramePath)
            require(file.isFile && file.length() in 1..(20L * 1024 * 1024)) { "Invalid first-frame image" }
            val bytes = file.readBytes()
            val mime = when {
                bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
                bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
                else -> throw IllegalArgumentException("Only JPEG and PNG images supported")
            }
            val image = JSONObject().put("bytesBase64Encoded", Base64.encodeToString(bytes, Base64.NO_WRAP))
                .put("mimeType", mime)
            instance.put("image", image)
        }
        val result = call(
            route(config, options.model) + ":predictLongRunning",
            token,
            JSONObject().put("instances", JSONArray().put(instance)).put("parameters", parameters)
        )
        return result.optString("name").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Vertex accepted no operation name: cannot safely retry automatically")
    }

    fun pollVideo(config: CloudConfig, token: String, clip: FilmClip): OperationStatus {
        validate(config, token)
        require(clip.operation.isNotBlank()) { "Job has no provider operation ID" }
        val payload = JSONObject().put("operationName", clip.operation)
        val response = call(route(config, clip.modelId) + ":fetchPredictOperation", token, payload)
        if (!response.optBoolean("done", false)) return OperationStatus(done = false)
        response.optJSONObject("error")?.let {
            return OperationStatus(true, error = it.optString("message", "Provider reported an error"))
        }
        val body = response.optJSONObject("response") ?: JSONObject()
        val videos = body.optJSONArray("videos")
            ?: body.optJSONArray("generatedVideos")
            ?: body.optJSONArray("predictions")
        val first = videos?.optJSONObject(0) ?: JSONObject()
        val video = first.optJSONObject("video") ?: first
        val remote = video.optString("gcsUri", video.optString("uri", ""))
        val base64 = video.optString("bytesBase64Encoded", "")
        if (remote.isBlank() && base64.isBlank()) {
            val filtered = body.optJSONArray("raiMediaFilteredReasons")
            return OperationStatus(true, error = if (filtered != null && filtered.length() > 0)
                "Video was filtered by the provider's safety policy"
            else "Provider completed but no supported video output was returned; review job in Cloud console.")
        }
        return OperationStatus(done = true, remoteUri = remote, base64Video = base64)
    }

    fun writeVideo(token: String, job: OperationStatus, destination: File) {
        destination.parentFile?.mkdirs()
        val tmp = File(destination.parentFile, destination.name + ".partial")
        try {
            when {
                job.base64Video.isNotBlank() -> {
                    val data = Base64.decode(job.base64Video, Base64.DEFAULT)
                    require(data.size in 16..(150 * 1024 * 1024)) { "Invalid inline video size" }
                    tmp.writeBytes(data)
                }
                job.remoteUri.startsWith("gs://") -> {
                    val raw = job.remoteUri.removePrefix("gs://")
                    val index = raw.indexOf('/')
                    require(index in 1 until raw.lastIndex) { "Invalid GCS output path" }
                    val bucket = raw.substring(0, index)
                    val path = raw.substring(index + 1)
                    val encoded = URLEncoder.encode(path, "UTF-8").replace("+", "%20")
                    val endpoint = "https://storage.googleapis.com/storage/v1/b/" +
                        bucket + "/o/" + encoded + "?alt=media"
                    val con = URL(endpoint).openConnection() as HttpURLConnection
                    try {
                        // Do not forward the bearer token to a redirecting host.
                        con.instanceFollowRedirects = false
                        con.requestMethod = "GET"
                        con.connectTimeout = 20000
                        con.readTimeout = 90000
                        con.setRequestProperty("Authorization", "Bearer " + token)
                        if (con.responseCode !in 200..299) {
                            throw IllegalStateException("Cloud Storage HTTP " + con.responseCode +
                                "; check bucket permissions or download from the Cloud console")
                        }
                        con.inputStream.use { inp ->
                            FileOutputStream(tmp).use { out ->
                                val buf = ByteArray(65536)
                                var count = 0L
                                while (true) {
                                    val n = inp.read(buf)
                                    if (n < 0) break
                                    count += n
                                    require(count < 150L * 1024 * 1024) { "Clip exceeds 150MB limit" }
                                    out.write(buf, 0, n)
                                }
                            }
                        }
                    } finally { con.disconnect() }
                }
                else -> throw IllegalStateException("Output URI type unsupported")
            }
            require(VideoOutputGuard.isMp4File(tmp)) {
                "Provider returned data that is not a valid MP4 header"
            }
            // POSIX rename within the same private directory atomically replaces
            // the prior file; never delete a good previous MP4 before promotion.
            android.system.Os.rename(tmp.absolutePath, destination.absolutePath)
        } catch (e: Exception) { tmp.delete(); throw e }
    }

    fun directorPrompt(config: CloudConfig, token: String, idea: String): String {
        validate(config, token, video = false)
        require(idea.isNotBlank())
        val url = "https://" + config.region + "-aiplatform.googleapis.com/v1/projects/" +
            config.projectId + "/locations/" + config.region +
            "/publishers/google/models/" + CHAT_MODEL + ":generateContent"
        val guidance = "You are MVG AI Director. Create a precise cinematic video prompt " +
            "with camera angle, lens, lighting, action, setting, mood, outfit and continuity. " +
            "Suggest a single 4/6/8-second shot and keep characters consistent. " +
            "No claims of guaranteed identity or unsupported model capabilities. User idea: " + idea
        val req = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("role", "user").put(
                "parts", JSONArray().put(JSONObject().put("text", guidance))
            ))
        ).put("generationConfig", JSONObject().put("temperature", 0.75).put("maxOutputTokens", 1200))
        val result = call(url, token, req, apiKey = config.authMode == "API_KEY")
        val candidates = result.optJSONArray("candidates")
        val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val text = StringBuilder()
        if (parts != null) for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val value = part.optString("text", "")
            if (value.isNotBlank()) text.append(value).append("\n")
        }
        return text.toString().trim().ifBlank { "No textual response; check model access or content filtering." }
    }
}
