package com.parmtech.mvg

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only diagnostics. No model execution, no POST, no billing lookups, and no
 * changes to the user's Cloud project. All requests are OAuth bearer GETs.
 *
 * A PASS for project visibility + Vertex API enablement does NOT guarantee
 * Veo model entitlement, IAM prediction permission, quotas or free-trial credit.
 */
data class CloudReadinessLine(
    val status: String,
    val title: String,
    val explanation: String
)

data class CloudReadinessReport(
    val project: CloudReadinessLine,
    val vertexApi: CloudReadinessLine,
    val projectId: String,
    val checkedAt: Long = System.currentTimeMillis()
) {
    val summary: String get() =
        if (project.status == "PASS" && vertexApi.status == "PASS")
            "Basic Cloud setup looks reachable. Veo access, billing credits and quota remain untested."
        else "Resolve the checks below before attempting any billable Veo video generation."
}

internal data class DiagnosticHttpReply(val status: Int, val body: String)

/** Transport injected for safe JVM tests so no real Google API is contacted in CI. */
internal fun interface DiagnosticReadOnlyTransport {
    fun get(url: String, token: String): DiagnosticHttpReply
}

object CloudReadiness {
    const val PROJECT_API = "https://cloudresourcemanager.googleapis.com/v1/projects/"
    const val SERVICE_API = "https://serviceusage.googleapis.com/v1/projects/"

    private val idPattern = Regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$")
    private val numberPattern = Regex("^[0-9]{1,20}$")

    private fun line(status: String, title: String, explanation: String) =
        CloudReadinessLine(status, title, explanation)

    private fun httpStatusMessage(code: Int, task: String): CloudReadinessLine =
        when (code) {
            401 -> line("BLOCKED", task, "Google authorization expired or was rejected. Use Continue with Google again.")
            403 -> line("UNKNOWN", task, "Access denied or API access unavailable. Check IAM permissions; cannot determine setup state.")
            404 -> line("BLOCKED", task, "Project or service not found. Check the Cloud Project ID and enabled APIs.")
            429 -> line("UNKNOWN", task, "Cloud API request rate-limited. Retry this read-only check later.")
            in 500..599 -> line("UNKNOWN", task, "Google Cloud service is temporarily unavailable (HTTP $code).")
            else -> line("UNKNOWN", task, "Could not verify Cloud setup (HTTP $code).")
        }

    /**
     * Calls:
     *  GET Cloud Resource Manager projects.get  (resourcemanager.projects.get IAM)
     *  GET Service Usage services.get            (serviceusage.services.get IAM)
     * Requires a short-lived Google cloud-platform OAuth token, not an API key.
     * Does not inspect billing balances, submit Veo jobs, or list private assets.
     */
    internal fun checkWithTransport(
        config: CloudConfig,
        token: String,
        transport: DiagnosticReadOnlyTransport
    ): CloudReadinessReport {
        val id = config.projectId.trim()
        if (!idPattern.matches(id)) {
            return CloudReadinessReport(
                line("BLOCKED", "Cloud Project ID", "Enter a valid project ID (6–30 lowercase letters, digits or hyphens)."),
                line("UNKNOWN", "Vertex AI API", "Project ID must be corrected first."),
                id
            )
        }
        if (config.authMode != "GOOGLE" || token.isBlank()) {
            return CloudReadinessReport(
                line("BLOCKED", "Google Login", "Select Google Login and tap Continue with Google to authorize this APK."),
                line("UNKNOWN", "Vertex AI API", "Google authorization is required for a read-only check."),
                id
            )
        }
        if (config.region != "us-central1") {
            return CloudReadinessReport(
                line("BLOCKED", "Video region", "Choose us-central1 for the currently configured Veo integration."),
                line("UNKNOWN", "Vertex AI API", "Correct the video region before checking."),
                id
            )
        }
        val project = try { transport.get(PROJECT_API + id, token) }
            catch (_: Exception) {
                return CloudReadinessReport(
                    line("UNKNOWN", "Cloud Project", "Network unavailable or secure connection failed. Check internet access."),
                    line("UNKNOWN", "Vertex AI API", "Project visibility must be checked first."), id
                )
            }
        if (project.status != 200) {
            return CloudReadinessReport(
                httpStatusMessage(project.status, "Cloud Project"),
                line("UNKNOWN", "Vertex AI API", "Project check did not succeed, so API state was not queried."),
                id
            )
        }
        val document = try { JSONObject(project.body) }
            catch (_: Exception) {
                return CloudReadinessReport(
                    line("UNKNOWN", "Cloud Project", "Unexpected project response from Google."),
                    line("UNKNOWN", "Vertex AI API", "Could not read a project number."), id
                )
            }
        val confirmedId = document.optString("projectId")
        val number = document.optString("projectNumber")
        if (confirmedId != id || !numberPattern.matches(number)) {
            return CloudReadinessReport(
                line("UNKNOWN", "Cloud Project", "Project identity or numeric identifier could not be verified."),
                line("UNKNOWN", "Vertex AI API", "Could not safely check service state."), id
            )
        }
        if (document.optString("lifecycleState") != "ACTIVE") {
            return CloudReadinessReport(
                line("BLOCKED", "Cloud Project", "The project is not ACTIVE. Check its lifecycle state in Cloud Console."),
                line("UNKNOWN", "Vertex AI API", "Project must be active."), id
            )
        }
        val projectLine = line("PASS", "Cloud Project",
            "Found active project: " + document.optString("name").take(60).ifBlank { id } + ".")
        val service = try { transport.get(SERVICE_API + number + "/services/aiplatform.googleapis.com", token) }
            catch (_: Exception) {
                return CloudReadinessReport(projectLine,
                    line("UNKNOWN", "Vertex AI API", "Could not reach Service Usage. Project access is still verified."), id)
            }
        if (service.status != 200) {
            return CloudReadinessReport(projectLine,
                httpStatusMessage(service.status, "Vertex AI API"), id)
        }
        val state = try { JSONObject(service.body).optString("state") }
            catch (_: Exception) { "" }
        val serviceLine = when (state) {
            "ENABLED" -> line("PASS", "Vertex AI API", "Vertex AI API is enabled for this project.")
            "DISABLED" -> line("BLOCKED", "Vertex AI API",
                "Enable aiplatform.googleapis.com in Cloud Console (API Library).")
            else -> line("UNKNOWN", "Vertex AI API", "The service state could not be interpreted.")
        }
        return CloudReadinessReport(projectLine, serviceLine, id)
    }

    fun check(config: CloudConfig, token: String): CloudReadinessReport =
        checkWithTransport(config, token, DiagnosticReadOnlyTransport(::safeGet))

    private fun safeGet(url: String, token: String): DiagnosticHttpReply {
        require(url.startsWith(PROJECT_API) || url.startsWith(SERVICE_API)) {
            "Only known read-only Google Cloud API endpoints are allowed"
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false // never forward OAuth credentials on redirects
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.useCaches = false
            val code = connection.responseCode
            // Don't surface raw API error responses that could contain account details.
            if (code != 200) return DiagnosticHttpReply(code, "")
            val body = connection.inputStream.use { stream ->
                val bytes = stream.readNBytes(8 * 1024 + 1)
                require(bytes.size <= 8 * 1024) { "Cloud project status response is too large" }
                String(bytes, Charsets.UTF_8)
            }
            return DiagnosticHttpReply(code, body)
        } finally {
            connection.disconnect()
        }
    }
}
