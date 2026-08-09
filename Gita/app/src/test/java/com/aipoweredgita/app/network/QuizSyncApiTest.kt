package com.aipoweredgita.app.network

import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QuizSyncApiTest {

    private lateinit var server: MockWebServer
    private lateinit var apiService: CoinApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(CoinApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `test recordQuizAttempt sends correct payload`() = runTest {
        // Prepare mock response
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"success\", \"id\":123}")
        )

        // Create request
        val request = QuizAttemptRequest(
            user_id = "test_user_123",
            score = 10,
            total_questions = 15,
            quiz_type = "general",
            time_spent_seconds = 120,
            coins_earned = 50,
            client_date = "2026-07-01T12:00:00Z",
            country_code = "IN"
        )

        // Make API call
        val response = apiService.recordQuizAttempt(request)

        // Verify response parsing
        assertEquals("success", response["status"])

        // Verify the request that was sent to the server
        val recordedRequest = server.takeRequest()
        assertEquals("/quiz/attempt", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        
        // Basic JSON string checking to ensure fields are serialized properly
        val bodyString = recordedRequest.body.readUtf8()
        assert(bodyString.contains("\"user_id\":\"test_user_123\""))
        assert(bodyString.contains("\"score\":10"))
        assert(bodyString.contains("\"total_questions\":15"))
        assert(bodyString.contains("\"time_spent_seconds\":120"))
    }
    
    @Test
    fun `test awardCoins sends correct payload`() = runTest {
        // Prepare mock response
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"awarded\":50, \"total_coins\":150, \"levelled_up\":false, \"new_level\":1}")
        )

        // Create request
        val request = CoinAwardRequest(
            user_id = "test_user_123",
            source = "quiz_completion",
            metadata = mapOf(
                "score" to 10,
                "accuracy" to 66.6f
            ),
            client_date = "2026-07-01T12:00:00Z",
            country_code = "IN"
        )

        // Make API call
        val response = apiService.awardCoins(request)

        // Verify response parsing
        assertEquals(50, response.awarded)
        assertEquals(150, response.total_coins)

        // Verify the request that was sent to the server
        val recordedRequest = server.takeRequest()
        assertEquals("/coins/award", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        
        val bodyString = recordedRequest.body.readUtf8()
        assert(bodyString.contains("\"user_id\":\"test_user_123\""))
        assert(bodyString.contains("\"source\":\"quiz_completion\""))
        assert(bodyString.contains("\"score\":10"))
    }
}
