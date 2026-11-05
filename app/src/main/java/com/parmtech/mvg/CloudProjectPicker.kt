package com.parmtech.mvg

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Display-only project data. Tokens never appear in result objects or UI. */
data class GoogleCloudProjectChoice(val projectId: String, val name: String)

/** Errors are intentionally generic: no private Google API response bodies are exposed. */
data class GoogleProjectDiscovery(
    val projects: List<GoogleCloudProjectChoice>,
    val message: String,
    val truncated: Boolean = false
)

internal fun interface GoogleProjectReadOnlyTransport {
    fun get(url: String, token: String): DiagnosticHttpReply
}

/** Uses only GET projects.list for projects already visible to the signed-in Google principal. */
object CloudProjectPicker {
    private const val BASE = "https://cloudresourcemanager.googleapis.com/v1/projects"
    private const val MAX_PAGES = 4
    private const val PAGE_SIZE = 100
    private const val MAX_PROJECTS = 200
    private const val RESPONSE_LIMIT = 128 * 1024
    private val PROJECT_ID = Regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$")

    internal fun discoverWithTransport(
        token: String,
        transport: GoogleProjectReadOnlyTransport
    ): GoogleProjectDiscovery {
        if (token.isBlank()) {
            return GoogleProjectDiscovery(emptyList(), "Connect with Google first.")
        }
        val found = linkedMapOf<String, GoogleCloudProjectChoice>()
        val seenPageTokens = hashSetOf<String>()
        var pageToken = ""
        var reachedLimit = false
        for (page in 0 until MAX_PAGES) {
            val url = BASE + "?pageSize=$PAGE_SIZE" +
                if (pageToken.isEmpty()) "" else
                    "&pageToken=" + URLEncoder.encode(pageToken, "UTF-8")
            val reply = try { transport.get(url, token) }
                catch (_: Exception) {
                    return GoogleProjectDiscovery(
                        found.values.toList(),
                        "Project list could not load. Check your connection or enter a Project ID manually.")
                }
            if (reply.status != 200) {
                val message = when (reply.status) {
                    401 -> "Google authorization expired. Tap Continue with Google."
                    403 -> "Google cannot list these projects with this account. Use a permitted account or enter Project ID manually."
                    429 -> "Google Cloud is rate-limiting requests. Try again later or enter Project ID."
                    else -> "Google Cloud project list unavailable (HTTP " +
                        reply.status + "). Enter Project ID manually."
                }
                return GoogleProjectDiscovery(found.values.toList(), message)
            }
            val json = try { JSONObject(reply.body) }
                catch (_: Exception) {
                    return GoogleProjectDiscovery(found.values.toList(),
                        "Google Cloud sent an unexpected project list. Enter Project ID manually.")
                }
            val items = json.optJSONArray("projects")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val entry = items.optJSONObject(i) ?: continue
                    val id = entry.optString("projectId")
                    if (entry.optString("lifecycleState") != "ACTIVE" ||
                        !PROJECT_ID.matches(id)) continue
                    if (!found.containsKey(id)) {
                        found[id] = GoogleCloudProjectChoice(id, entry.optString("name")
                            .take(60).ifBlank { id })
                    }
                    if (found.size >= MAX_PROJECTS) {
                        reachedLimit = true
                        break
                    }
                }
            }
            if (reachedLimit) break
            pageToken = json.optString("nextPageToken")
            if (pageToken.isBlank()) break
            // A malicious or buggy provider must not make us loop indefinitely.
            if (pageToken.length > 1024 || !seenPageTokens.add(pageToken)) {
                reachedLimit = true
                break
            }
            if (page == MAX_PAGES - 1) reachedLimit = true
        }
        val sorted = found.values.sortedWith(
            compareBy<GoogleCloudProjectChoice> { it.name.lowercase() }
                .thenBy { it.projectId }
        )
        return GoogleProjectDiscovery(
            sorted,
            if (sorted.isEmpty())
                "No active Cloud projects visible. Create one in Google Cloud, switch Google account, or enter Project ID manually."
            else if (reachedLimit)
                "Some projects are shown. If yours is missing, enter its Project ID manually."
            else "Choose your Cloud project below.",
            reachedLimit
        )
    }

    fun discover(token: String): GoogleProjectDiscovery = discoverWithTransport(
        token, GoogleProjectReadOnlyTransport(::safeGet))

    private fun safeGet(url: String, token: String): DiagnosticHttpReply {
        require(url.startsWith("$BASE?pageSize=$PAGE_SIZE") &&
            URL(url).host == "cloudresourcemanager.googleapis.com") {
            "Project discovery is restricted to the Google Cloud Resource Manager"
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status != 200) return DiagnosticHttpReply(status, "")
            val data = connection.inputStream.use { input ->
                val sink = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    require(sink.size() + read <= RESPONSE_LIMIT) {
                        "Unexpected oversized Cloud project list"
                    }
                    sink.write(buffer, 0, read)
                }
                sink.toString("UTF-8")
            }
            return DiagnosticHttpReply(status, data)
        } finally {
            connection.disconnect()
        }
    }
}
