package io.github.fallenleaves089.retrofitkit

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * RetrofitKit — Retrofit + OkHttp 一站式网络客户端。
 *
 * 核心功能：
 * - 自动添加 Authorization Token 头
 * - 全局 401 检测 + onTokenExpired 回调
 * - 可自定义的错误消息（NetworkErrorDecoder）
 * - Gson 默认 BigDecimal 精度保护（PriceTypeAdapter）
 * - Cookie 自动管理（SimpleCookieJar，仅内存）
 * - 可配置超时 + 日志拦截器
 *
 * ## 初始化示例：
 * ```
 * RetrofitKit.init(
 *     context = applicationContext,
 *     baseUrl = "https://api.example.com/",
 *     tokenProvider = { TokenManager.getToken() },
 *     onTokenExpired = { context -> /* 跳转登录页 */ },
 *     errorDecoder = DefaultNetworkErrorDecoder(),
 *     enableLogging = BuildConfig.DEBUG
 * )
 *
 * // 获取 API Service
 * val apiService = RetrofitKit.create(ApiService::class.java)
 * ```
 */
object RetrofitKit {

    @Volatile
    private var retrofit: Retrofit? = null
    private var onTokenExpired: ((Context) -> Unit)? = null
    private var errorDecoder: NetworkErrorDecoder? = null

    /**
     * 初始化 RetrofitKit。必须在调用 [create] 之前完成初始化。
     *
     * @param context        Application Context（用于 CookieJar 等工作）
     * @param baseUrl        后端 API 根地址
     * @param tokenProvider  Token 提供者（每次请求前回调），返回 null 表示无 Token
     * @param onTokenExpired Token 过期回调（收到 401 时触发）
     * @param errorDecoder   网络错误消息解码器
     * @param connectTimeout 连接超时（秒），默认 15
     * @param readTimeout    读取超时（秒），默认 15
     * @param enableLogging  是否启用 OkHttp 日志拦截器（建议仅 Debug 开启）
     */
    @Synchronized
    fun init(
        context: Context,
        baseUrl: String,
        tokenProvider: (() -> String?)? = null,
        onTokenExpired: ((Context) -> Unit)? = null,
        errorDecoder: NetworkErrorDecoder? = null,
        connectTimeout: Long = 15,
        readTimeout: Long = 15,
        enableLogging: Boolean = false,
    ) {
        this.onTokenExpired = onTokenExpired
        this.errorDecoder = errorDecoder

        val okHttpBuilder = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .cookieJar(SimpleCookieJar())

        // 鉴权拦截器：自动注入 Authorization 头
        okHttpBuilder.addInterceptor(AuthHeaderInterceptor(tokenProvider))

        // 响应拦截器：检测 401
        okHttpBuilder.addInterceptor(UnauthorizedInterceptor(context, onTokenExpired))

        // 日志拦截器
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
            }
            okHttpBuilder.addInterceptor(loggingInterceptor)
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(BigDecimal::class.java, PriceTypeAdapter())
            .create()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 创建 API Service 接口的代理实例。
     * 必须先调用 [init] 初始化。
     *
     * @param serviceClass API 接口 Class
     * @return Retrofit 创建的 Service 实例
     */
    fun <T> create(serviceClass: Class<T>): T {
        val r = retrofit ?: throw IllegalStateException(
            "RetrofitKit 尚未初始化，请先调用 RetrofitKit.init(...)"
        )
        return r.create(serviceClass)
    }

    /**
     * 重新初始化 RetrofitKit（动态切换 baseUrl 等参数时使用）。
     */
    @Synchronized
    fun reinitialize(
        context: Context,
        baseUrl: String,
        tokenProvider: (() -> String?)? = null,
        onTokenExpired: ((Context) -> Unit)? = null,
        errorDecoder: NetworkErrorDecoder? = null,
        connectTimeout: Long = 15,
        readTimeout: Long = 15,
        enableLogging: Boolean = false,
    ) {
        retrofit = null
        init(context, baseUrl, tokenProvider, onTokenExpired, errorDecoder,
            connectTimeout, readTimeout, enableLogging)
    }

    /**
     * 根据 Response 获取用户可读的错误消息。
     *
     * @param context Android Context
     * @param response OkHttp Response（可选）
     * @param isNetworkAvailable 当前是否有网络
     * @return 错误消息字符串
     */
    fun getNetworkErrorMessage(
        context: Context,
        response: Response?,
        isNetworkAvailable: Boolean
    ): String {
        return errorDecoder?.getErrorMessage(context, response, isNetworkAvailable)
            ?: "网络请求失败，请稍后重试"
    }

    /**
     * 获取 WebSocket URL（将 http 协议转 ws）。
     *
     * @param baseUrl 当前 API 的 baseUrl
     * @param path    WebSocket 路径（如 "/ws"）
     * @return 完整的 WebSocket URL
     */
    fun getWebSocketUrl(baseUrl: String, path: String): String {
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid baseUrl: $baseUrl")
        val queryIndex = path.indexOf('?')
        val pathOnly = if (queryIndex >= 0) path.substring(0, queryIndex) else path
        val query = if (queryIndex >= 0) path.substring(queryIndex + 1) else null
        val basePath = httpUrl.encodedPath.trimEnd('/')
        val normalizedPath = if (pathOnly.startsWith("/")) pathOnly else "/$pathOnly"
        val target = httpUrl.newBuilder()
            .encodedPath(basePath + normalizedPath)
            .query(query)
            .fragment(null)
            .build()
            .toString()
        return if (httpUrl.isHttps) {
            "wss" + target.removePrefix("https")
        } else {
            "ws" + target.removePrefix("http")
        }
    }

    // ── 内部工具 ──

    /**
     * In-memory, thread-safe cookie jar.
     *
     * Cookies are not persisted across process restarts. Expiry, path, domain, and Secure
     * attributes are evaluated while the process is alive.
     */
    internal class SimpleCookieJar : CookieJar {
        private val cookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            synchronized(this) {
                for (cookie in cookies) {
                    this.cookies.removeAll {
                        it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path
                    }
                    if (cookie.expiresAt > System.currentTimeMillis()) {
                        this.cookies.add(cookie)
                    }
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(this) {
                val now = System.currentTimeMillis()
                cookies.removeAll { it.expiresAt <= now }
                return cookies.filter { it.matches(url) }
            }
        }
    }
}
