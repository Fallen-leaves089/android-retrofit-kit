package io.github.fallenleaves089.retrofitkit

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthHeaderInterceptorTest {

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

    @Test
    fun injectsBearerTokenWhenTokenExists() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { "test-token" })
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertEquals("Bearer test-token", it.request.header("Authorization"))
        }
    }

    @Test
    fun skipsHeaderWhenTokenIsNull() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { null })
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }

    @Test
    fun skipsHeaderWhenTokenIsBlank() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { "  " })
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }
}
