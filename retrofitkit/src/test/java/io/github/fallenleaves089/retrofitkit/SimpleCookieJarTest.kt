package io.github.fallenleaves089.retrofitkit

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleCookieJarTest {

    private val appUrl = "https://example.com/app".toHttpUrl()

    @Test
    fun sendsCookieOnlyToMatchingPath() {
        val jar = RetrofitKit.SimpleCookieJar()
        jar.saveFromResponse(appUrl, listOf(cookie("session", "abc", "/app")))

        assertEquals(1, jar.loadForRequest("https://example.com/app/user".toHttpUrl()).size)
        assertTrue(jar.loadForRequest("https://example.com/other".toHttpUrl()).isEmpty())
    }

    @Test
    fun replacesCookieWithSameIdentity() {
        val jar = RetrofitKit.SimpleCookieJar()
        jar.saveFromResponse(appUrl, listOf(cookie("session", "old", "/")))
        jar.saveFromResponse(appUrl, listOf(cookie("session", "new", "/")))

        val loaded = jar.loadForRequest(appUrl)
        assertEquals(1, loaded.size)
        assertEquals("new", loaded[0].value)
    }

    @Test
    fun dropsExpiredCookie() {
        val jar = RetrofitKit.SimpleCookieJar()
        val expired = cookie("old", "1", "/", expiresAt = System.currentTimeMillis() - 1_000)
        jar.saveFromResponse(appUrl, listOf(expired))

        assertTrue(jar.loadForRequest(appUrl).isEmpty())
    }

    @Test
    fun doesNotSendSecureCookieOverPlainHttp() {
        val jar = RetrofitKit.SimpleCookieJar()
        val secure = Cookie.Builder()
            .name("session")
            .value("abc")
            .domain("example.com")
            .path("/")
            .secure()
            .build()
        jar.saveFromResponse(appUrl, listOf(secure))

        assertTrue(jar.loadForRequest("http://example.com/".toHttpUrl()).isEmpty())
        assertEquals(1, jar.loadForRequest("https://example.com/".toHttpUrl()).size)
    }

    private fun cookie(
        name: String,
        value: String,
        path: String,
        expiresAt: Long = System.currentTimeMillis() + 60_000
    ): Cookie {
        return Cookie.Builder()
            .name(name)
            .value(value)
            .domain("example.com")
            .path(path)
            .expiresAt(expiresAt)
            .build()
    }
}
