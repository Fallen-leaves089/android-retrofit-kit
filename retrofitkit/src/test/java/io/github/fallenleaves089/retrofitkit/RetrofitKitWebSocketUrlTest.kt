package io.github.fallenleaves089.retrofitkit

import org.junit.Assert.assertEquals
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
}
