package com.example.gitalearning

import com.aipoweredgita.app.network.RetryInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RetryInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWithRetry(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .build()
    }

    @Test
    fun smoke() {
        assertTrue(true)
    }

    @Test
    fun retriesOn5xx() {
        // First request fails with 500, second succeeds
        server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = clientWithRetry()
        val req = Request.Builder().url(server.url("/test")).build()
        val resp = client.newCall(req).execute()

        assertTrue(resp.isSuccessful)
        assertEquals(200, resp.code)
        assertEquals(2, server.requestCount) // Verify retry happened
    }

    @Test
    fun doesNotRetryOn4xx() {
        // 404 should not be retried
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val client = clientWithRetry()
        val req = Request.Builder().url(server.url("/test")).build()

        try {
            client.newCall(req).execute()
            assertTrue("Expected IOException to be thrown", false)
        } catch (e: IOException) {
            // This is expected - 4xx errors throw IOException
            assertTrue(e.message?.contains("404") == true)
            assertEquals(1, server.requestCount) // Verify no retry
        }
    }

    @Test
    fun successfulRequestDoesNotRetry() {
        // 200 response should work on first try
        server.enqueue(MockResponse().setResponseCode(200).setBody("success"))

        val client = clientWithRetry()
        val req = Request.Builder().url(server.url("/test")).build()
        val resp = client.newCall(req).execute()

        assertTrue(resp.isSuccessful)
        assertEquals(200, resp.code)
        assertEquals(1, server.requestCount) // No retry needed
    }
}
