package com.parmtech.mvg

import org.junit.Assert.assertTrue
import org.junit.Test

/** Credential choice must never silently turn a Gemini API key into a Veo bearer token. */
class AuthModeTest {
    private val project = "mvg-test-project"

    @Test fun googleLoginIsAllowedForVeo() {
        VertexClient.validate(CloudConfig(projectId = project, authMode = "GOOGLE"),
            "nonempty-fake-bearer", video = true)
    }

    @Test fun apiKeyCanConfigureGeminiDirector() {
        VertexClient.validate(CloudConfig(projectId = project, authMode = "API_KEY"),
            "nonempty-fake-api-key", video = false)
    }

    @Test fun apiKeyDoesNotAuthorizeVeoLongRunningEndpoint() {
        val error = try {
            VertexClient.validate(CloudConfig(projectId = project, authMode = "API_KEY"),
                "nonempty-fake-api-key", video = true)
            ""
        } catch (e: IllegalArgumentException) { e.message ?: "" }
        assertTrue(error.contains("Google Login"))
    }

    @Test fun missingCredentialsShowModeSpecificHint() {
        val noKey = try {
            VertexClient.validate(CloudConfig(projectId = project, authMode = "API_KEY"),
                "", video = false)
            ""
        } catch (e: IllegalArgumentException) { e.message ?: "" }
        val noLogin = try {
            VertexClient.validate(CloudConfig(projectId = project, authMode = "GOOGLE"),
                "", video = true)
            ""
        } catch (e: IllegalArgumentException) { e.message ?: "" }
        assertTrue(noKey.contains("API key"))
        assertTrue(noLogin.contains("Google"))
    }
}
