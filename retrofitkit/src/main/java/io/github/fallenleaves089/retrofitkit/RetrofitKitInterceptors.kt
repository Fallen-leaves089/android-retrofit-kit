package io.github.fallenleaves089.retrofitkit

import android.content.Context
import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response

/**
 * Exact HTTP origin used for token injection.
 *
 * The port is the effective port, so `https://example.com` and
 * `https://example.com:443` resolve to the same origin.
 */
internal data class TokenOrigin(
    val scheme: String,
    val host: String,
    val port: Int,
) {
    companion object {
        fun fromUrl(url: HttpUrl): TokenOrigin = TokenOrigin(url.scheme, url.host, url.port)

        fun parse(raw: String): TokenOrigin {
            val url = raw.trim().toHttpUrlOrNull()
                ?: throw IllegalArgumentException("allowedTokenOrigins contains an invalid URL: $raw")
            require(url.scheme == "http" || url.scheme == "https") {
                "allowedTokenOrigins must use http or https: $raw"
            }
            require(url.username.isEmpty() && url.password.isEmpty()) {
                "allowedTokenOrigins must not contain user information: $raw"
            }
            require(url.encodedPath == "/" && url.query == null && url.fragment == null) {
                "allowedTokenOrigins must contain only scheme, host, and port: $raw"
            }
            return fromUrl(url)
        }
    }
}

internal fun resolveTokenOrigins(
    baseUrl: String,
    allowedTokenOrigins: Set<String>,
): Set<TokenOrigin> {
    val base = baseUrl.toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Invalid baseUrl: $baseUrl")
    val origins = linkedSetOf(TokenOrigin.fromUrl(base))
    allowedTokenOrigins.forEach { origins.add(TokenOrigin.parse(it)) }
    return origins
}

/**
 * 鉴权拦截器：在请求发出前自动注入 Authorization 头。
 *
 * Token 为 null 或空白时保持原请求不变，避免生成无效的 Bearer 头。
 */
internal class AuthHeaderInterceptor(
    private val tokenProvider: (() -> String?)?,
    private val allowedTokenOrigins: Set<TokenOrigin>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }
        if (TokenOrigin.fromUrl(originalRequest.url) !in allowedTokenOrigins) {
            return chain.proceed(originalRequest)
        }
        val token = tokenProvider?.invoke()
        val newRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(newRequest)
    }
}

/**
 * 响应拦截器：全局检测 401，并触发 onTokenExpired 回调。
 */
internal class UnauthorizedInterceptor(
    private val context: Context,
    private val onTokenExpired: ((Context) -> Unit)?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            onTokenExpired?.invoke(context)
        }
        return response
    }
}
