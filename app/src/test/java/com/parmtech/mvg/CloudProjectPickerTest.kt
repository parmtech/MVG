package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class CloudProjectPickerTest {
    private val token = "fake-oauth-no-real-network"

    @Test fun listsOnlyActiveProjectsFromGoogleReadOnlyEndpoint() {
        var calls = 0
        val result = CloudProjectPicker.discoverWithTransport(token) { url, credential ->
            calls++
            assertEquals(token, credential)
            assertTrue(url.startsWith("https://cloudresourcemanager.googleapis.com/v1/projects?pageSize=100"))
            DiagnosticHttpReply(200, """{"projects":[
                {"projectId":"movie-cloud-123","name":"Film Studio","lifecycleState":"ACTIVE"},
                {"projectId":"other-cloud-123","name":"Old","lifecycleState":"DELETE_REQUESTED"},
                {"projectId":"../unsafe","name":"Invalid","lifecycleState":"ACTIVE"},
                {"projectId":"second-movie-2","name":"Second Studio","lifecycleState":"ACTIVE"}]}""")
        }
        assertEquals(1, calls)
        assertEquals(2, result.projects.size)
        assertEquals("movie-cloud-123", result.projects.first().projectId)
        assertFalse(result.truncated)
    }

    @Test fun followsNextPageTokenWithBoundedUrlEncodedToken() {
        val urls = mutableListOf<String>()
        val result = CloudProjectPicker.discoverWithTransport(token) { url, _ ->
            urls.add(url)
            when(urls.size) {
                1 -> DiagnosticHttpReply(200, """{"nextPageToken":"second page&","projects":[{"projectId":"project-one-12","name":"One","lifecycleState":"ACTIVE"}]}""")
                2 -> DiagnosticHttpReply(200, """{"projects":[{"projectId":"project-two-12","name":"Two","lifecycleState":"ACTIVE"}]}""")
                else -> throw AssertionError("Too many requests")
            }
        }
        assertEquals(2, urls.size)
        assertTrue(urls[1].contains("pageToken=second+page%26"))
        assertEquals(2, result.projects.size)
    }

    @Test fun noOAuthNeverCallsCloud() {
        val result = CloudProjectPicker.discoverWithTransport("") { _, _ ->
            throw AssertionError("Do not list without Google authorization")
        }
        assertTrue(result.projects.isEmpty())
        assertTrue(result.message.contains("Connect"))
    }

    @Test fun permissionDeniedOffersManualFallbackWithoutLeakingPrivateError() {
        val result = CloudProjectPicker.discoverWithTransport(token) { _, _ ->
            DiagnosticHttpReply(403, """{"error":"private-account-info"}""")
        }
        assertTrue(result.projects.isEmpty())
        assertTrue(result.message.contains("manually"))
        assertFalse(result.message.contains("private-account"))
    }

    @Test fun repeatedPaginationTokenCannotRunForever() {
        var calls = 0
        val result = CloudProjectPicker.discoverWithTransport(token) { _, _ ->
            calls++
            DiagnosticHttpReply(200, """{"nextPageToken":"repeat","projects":[]}""")
        }
        assertEquals(2, calls)
        assertTrue(result.truncated)
    }

    @Test fun networkFailuresStillPermitManualProjectId() {
        val result = CloudProjectPicker.discoverWithTransport(token) { _, _ ->
            throw java.io.IOException("secret network error")
        }
        assertTrue(result.message.contains("manually"))
        assertFalse(result.message.contains("secret network error"))
    }
}
