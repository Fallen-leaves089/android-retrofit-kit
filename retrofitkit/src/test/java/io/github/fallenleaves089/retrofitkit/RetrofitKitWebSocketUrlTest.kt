package io.github.fallenleaves089.retrofitkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RetrofitKitWebSocketUrlTest {

    @Test
    fun convertsHttpToWs() {
        assertEquals(
            "ws://example.com/ws",
            RetrofitKit.getWebSocketUrl("http://example.com", "/ws")
        )
    }

    @Test
    fun convertsHttpsToWss() {
        assertEquals(
            "wss://example.com/ws",
            RetrofitKit.getWebSocketUrl("https://example.com", "/ws")
        )
    }

    @Test
    fun removesTrailingSlashFromHost() {
        assertEquals(
            "wss://example.com/ws",
            RetrofitKit.getWebSocketUrl("https://example.com/", "/ws")
        )
    }

    @Test
    fun addsMissingLeadingSlashToPath() {
        assertEquals(
            "ws://example.com/ws",
            RetrofitKit.getWebSocketUrl("http://example.com", "ws")
        )
    }

    @Test
    fun preservesPortAndBasePath() {
        assertEquals(
            "wss://example.com:8443/api/ws",
            RetrofitKit.getWebSocketUrl("https://example.com:8443/api/", "/ws")
        )
    }

    @Test
    fun movesQueryStringToQueryComponent() {
        assertEquals(
            "wss://example.com/api/ws?token=abc",
            RetrofitKit.getWebSocketUrl("https://example.com/api", "/ws?token=abc")
        )
    }

    @Test
    fun rejectsInvalidBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            RetrofitKit.getWebSocketUrl("not a url", "/ws")
        }
    }
}
