package io.github.fallenleaves089.retrofitkit

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UnauthorizedInterceptorTest {

    private lateinit var server: MockWebServer
    private val context: Context = mock(Context::class.java)

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
    fun invokesCallbackOn401() {
        server.enqueue(MockResponse().setResponseCode(401))
        var callbackCount = 0
        var expiredContext: Context? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(
                UnauthorizedInterceptor(context) {
                    callbackCount++
                    expiredContext = it
                }
            )
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertEquals(401, it.code)
        }

        assertEquals(1, callbackCount)
        assertSame(context, expiredContext)
    }

    @Test
    fun doesNotInvokeCallbackOnSuccess() {
        server.enqueue(MockResponse().setResponseCode(200))
        var callbackCount = 0
        val client = OkHttpClient.Builder()
            .addInterceptor(
                UnauthorizedInterceptor(context) {
                    callbackCount++
                }
            )
            .build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().use {
            assertEquals(200, it.code)
        }

        assertEquals(0, callbackCount)
    }
}
