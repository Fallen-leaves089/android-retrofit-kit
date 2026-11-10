package io.github.fallenleaves089.retrofitkit

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
            .addInterceptor(AuthHeaderInterceptor({ "test-token" }, setOf(TokenOrigin.fromUrl(server.url("/")))))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertEquals("Bearer test-token", it.request.header("Authorization"))
        }
    }

    @Test
    fun skipsHeaderWhenTokenIsNull() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor({ null }, setOf(TokenOrigin.fromUrl(server.url("/")))))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }

    @Test
    fun skipsHeaderWhenTokenIsBlank() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor({ "  " }, setOf(TokenOrigin.fromUrl(server.url("/")))))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }

    @Test
    fun injectsTokenForAnExplicitlyAllowedOrigin() {
        val extraServer = MockWebServer()
        extraServer.start()
        try {
            extraServer.enqueue(MockResponse().setResponseCode(200))
            val origins = setOf(
                TokenOrigin.fromUrl(server.url("/")),
                TokenOrigin.fromUrl(extraServer.url("/")),
            )
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthHeaderInterceptor({ "test-token" }, origins))
                .build()

            client.newCall(Request.Builder().url(extraServer.url("/")).build()).execute().use {
                assertEquals("Bearer test-token", it.request.header("Authorization"))
            }
        } finally {
            extraServer.shutdown()
        }
    }

    @Test
    fun skipsTokenForDifferentHostWithoutReadingTheProvider() {
        server.enqueue(MockResponse().setResponseCode(200))
        var tokenCalls = 0
        val origins = setOf(TokenOrigin("http", "api.example.com", server.port))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor({ tokenCalls++; "test-token" }, origins))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
        assertEquals(0, tokenCalls)
    }

    @Test
    fun skipsTokenForDifferentPort() {
        server.enqueue(MockResponse().setResponseCode(200))
        val origins = setOf(TokenOrigin("http", server.url("/").host, server.port + 1))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor({ "test-token" }, origins))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }

    @Test
    fun skipsTokenForDifferentScheme() {
        server.enqueue(MockResponse().setResponseCode(200))
        val origins = setOf(TokenOrigin("https", server.url("/").host, server.port))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor({ "test-token" }, origins))
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertNull(it.request.header("Authorization"))
        }
    }

    @Test
    fun doesNotOverrideAnExplicitAuthorizationHeader() {
        server.enqueue(MockResponse().setResponseCode(200))
        var tokenCalls = 0
        val client = OkHttpClient.Builder()
            .addInterceptor(
                AuthHeaderInterceptor(
                    { tokenCalls++; "test-token" },
                    setOf(TokenOrigin.fromUrl(server.url("/"))),
                )
            )
            .build()

        val request = Request.Builder()
            .url(server.url("/"))
            .header("Authorization", "Custom credential")
            .build()
        client.newCall(request).execute().use {
            assertEquals("Custom credential", it.request.header("Authorization"))
        }
        assertEquals(0, tokenCalls)
    }

    @Test
    fun tokenOriginRejectsPathQueryFragmentAndUserInfo() {
        listOf(
            "https://api.example.com/v1",
            "https://api.example.com/?a=1",
            "https://api.example.com/#fragment",
            "https://user:pass@api.example.com",
        ).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) {
                TokenOrigin.parse(value)
            }
        }
    }

    @Test
    fun tokenOriginNormalizesTheDefaultHttpsPort() {
        assertEquals(
            TokenOrigin("https", "api.example.com", 443),
            TokenOrigin.parse("https://api.example.com"),
        )
    }

    @Test
    fun resolveTokenOriginsAlwaysIncludesTheBaseOrigin() {
        val origins = resolveTokenOrigins(
            "https://api.example.com/v1/",
            setOf("https://uploads.example.com:8443"),
        )

        assertEquals(
            setOf(
                TokenOrigin("https", "api.example.com", 443),
                TokenOrigin("https", "uploads.example.com", 8443),
            ),
            origins,
        )
    }
}
