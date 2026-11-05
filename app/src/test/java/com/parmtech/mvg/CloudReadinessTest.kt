package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudReadinessTest {
    private val cloud = CloudConfig(
        projectId = "movie-project-123",
        region = "us-central1",
        authMode = "GOOGLE"
    )
    private val token = "only-injected-test-token"

    @Test fun successfulReadOnlyChecksNeedExactlyTwoGetRequests() {
        val visited = mutableListOf<String>()
        val report = CloudReadiness.checkWithTransport(cloud, token) { url, bearer ->
            assertEquals(token, bearer)
            visited.add(url)
            when (visited.size) {
                1 -> DiagnosticHttpReply(200,
                    """{"projectId":"movie-project-123","projectNumber":"456789123","name":"Films","lifecycleState":"ACTIVE"}""")
                2 -> DiagnosticHttpReply(200,
                    """{"name":"projects/456789123/services/aiplatform.googleapis.com","state":"ENABLED"}""")
                else -> throw AssertionError("Unexpected third API request")
            }
        }
        assertEquals(2, visited.size)
        assertEquals(CloudReadiness.PROJECT_API + "movie-project-123", visited[0])
        assertEquals(CloudReadiness.SERVICE_API +
            "456789123/services/aiplatform.googleapis.com", visited[1])
        assertTrue(visited.all { it.startsWith("https://") })
        assertEquals("PASS", report.project.status)
        assertEquals("PASS", report.vertexApi.status)
        assertTrue(report.summary.contains("remain untested"))
    }

    @Test fun disabledVertexServiceIsNotReportedReady() {
        val report = CloudReadiness.checkWithTransport(cloud, token) { url, _ ->
            if (url.startsWith(CloudReadiness.PROJECT_API)) DiagnosticHttpReply(200,
                """{"projectId":"movie-project-123","projectNumber":"123456","lifecycleState":"ACTIVE"}""")
            else DiagnosticHttpReply(200, """{"state":"DISABLED"}""")
        }
        assertEquals("PASS", report.project.status)
        assertEquals("BLOCKED", report.vertexApi.status)
        assertTrue(report.vertexApi.explanation.contains("Enable"))
    }

    @Test fun iam403IsUnknownNotAssumedDisabled() {
        var calls = 0
        val report = CloudReadiness.checkWithTransport(cloud, token) { _, _ ->
            calls++
            DiagnosticHttpReply(403, """{"error":{"message":"not exposed"}}""")
        }
        assertEquals(1, calls)
        assertEquals("UNKNOWN", report.project.status)
        assertEquals("UNKNOWN", report.vertexApi.status)
        assertFalse(report.project.explanation.contains("not exposed"))
    }

    @Test fun expiredLoginAndApiKeyModeNeverUseNetwork() {
        for ((mode, key) in listOf("GOOGLE" to "", "API_KEY" to "some-api-key")) {
            val report = CloudReadiness.checkWithTransport(cloud.copy(authMode = mode), key) {
                _, _ -> throw AssertionError("No Google read-only call allowed without Google OAuth token")
            }
            assertEquals("BLOCKED", report.project.status)
        }
    }

    @Test fun rejectNonCanonicalProjectIdsBeforeNetwork() {
        val invalid = listOf("../secrets", "https://evil.example", "A-B", "a", "abcde",
            "a".repeat(31), "a-b-", "my_project", "test/path")
        for (id in invalid) {
            val report = CloudReadiness.checkWithTransport(cloud.copy(projectId = id), token) {
                _, _ -> throw AssertionError("Invalid project ID must never make HTTP call")
            }
            assertEquals("BLOCKED", report.project.status)
        }
    }

    @Test fun unrelatedProjectNumberIsNeverTrusted() {
        var calls = 0
        val report = CloudReadiness.checkWithTransport(cloud, token) { _, _ ->
            calls++
            DiagnosticHttpReply(200,
                """{"projectId":"wrong-project","projectNumber":"not-a-number","lifecycleState":"ACTIVE"}""")
        }
        assertEquals(1, calls)
        assertEquals("UNKNOWN", report.project.status)
        assertEquals("UNKNOWN", report.vertexApi.status)
    }

    @Test fun regionMisconfigurationIsVisibleWithoutCloudCalls() {
        val report = CloudReadiness.checkWithTransport(
            cloud.copy(region = "europe-west1"), token
        ) { _, _ -> throw AssertionError("Don't call Cloud with unsupported regional config") }
        assertEquals("BLOCKED", report.project.status)
        assertTrue(report.project.explanation.contains("us-central1"))
    }

    @Test fun serverFailuresDoNotRevealPrivateDetails() {
        for (status in listOf(401, 404, 429, 502)) {
            val report = CloudReadiness.checkWithTransport(cloud, token) { _, _ ->
                DiagnosticHttpReply(status,
                    """{"error":{"message":"Sensitive owner account information"}}""")
            }
            assertFalse(report.project.explanation.contains("Sensitive"))
            assertTrue(report.vertexApi.status == "UNKNOWN")
        }
    }
}
