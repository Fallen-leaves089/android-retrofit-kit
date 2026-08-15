package io.github.fallenleaves089.retrofitkit

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 鉴权拦截器：在请求发出前自动注入 Authorization 头。
 *
 * Token 为 null 或空白时保持原请求不变，避免生成无效的 Bearer 头。
 */
internal class AuthHeaderInterceptor(
    private val tokenProvider: (() -> String?)?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
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
