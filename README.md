# android-retrofit-kit

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-Fallen-leaves089%2Fandroid--retrofit--kit-lightgrey?logo=github)](https://github.com/Fallen-leaves089/android-retrofit-kit)

Android Retrofit 工具包 | 加密 Token | 价格精度保护 | 动态 URL

MIT License. Copyright (c) 2024 Fallen-leaves089.

---

## 功能

- **Token 自动注入**：每次请求自动拼接 `Authorization: Bearer <token>` 头
- **401 全局拦截**：收到 401 时自动触发 `onTokenExpired` 回调
- **BigDecimal 精度保护**：`PriceTypeAdapter` 防止 Gson 将金额字段解析为 double 导致精度丢失
- **加密 Token 存储**：`TokenManager` 基于 AndroidX EncryptedSharedPreferences，密钥托管到 Android Keystore
- **可自定义错误消息**：`NetworkErrorDecoder` 接口，用户实现自定义错误提示
- **Cookie 自动管理**：内置 `SimpleCookieJar`
- **日志拦截器**：可开关的 OkHttp 日志输出

---

## 依赖坐标

本库尚未发布到 Maven Central，可 clone 后作为本地 module 引入：

```gradle
// settings.gradle.kts
include(":retrofitkit")
project(":retrofitkit").projectDir = File("path/to/android-retrofit-kit/retrofitkit")

// app/build.gradle.kts
dependencies {
    implementation(project(":retrofitkit"))
}
```

---

## 快速开始

### 1. 初始化（Application.onCreate）

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化 Token 加密存储
        TokenManager.init(this)
        
        // 2. 初始化网络客户端
        RetrofitKit.init(
            context = this,
            baseUrl = "https://api.example.com/",
            tokenProvider = { TokenManager.getToken() },
            onTokenExpired = { context ->
                // 跳转登录页
                TokenManager.clearToken()
                context.startActivity(Intent(context, LoginActivity::class.java))
            },
            errorDecoder = object : NetworkErrorDecoder {
                override fun getErrorMessage(
                    context: Context,
                    response: Response?,
                    isNetworkAvailable: Boolean
                ): String {
                    return when (response?.code()) {
                        500 -> "服务器错误，请稍后重试"
                        404 -> "请求的资源不存在"
                        403 -> "没有访问权限"
                        else -> if (!isNetworkAvailable) "网络不可用" else "请求失败"
                    }
                }
            },
            enableLogging = BuildConfig.DEBUG
        )
    }
}
```

### 2. 定义 API 接口

```kotlin
interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("/api/user/profile")
    suspend fun getProfile(): Response<UserProfile>
}
```

### 3. 发起网络请求

```kotlin
// 创建 Service 实例
val apiService = RetrofitKit.create(ApiService::class.java)

// 在 ViewModel 中使用
viewModelScope.launch(Dispatchers.IO) {
    try {
        val response = apiService.getProfile()
        if (response.isSuccessful) {
            // 处理成功
        } else {
            val errorMsg = RetrofitKit.getNetworkErrorMessage(
                context, response.raw(), isNetworkAvailable
            )
            // 显示错误
        }
    } catch (e: Exception) {
        val errorMsg = RetrofitKit.getNetworkErrorMessage(
            context, null, isNetworkAvailable
        )
    }
}
```

### 4. 登录后保存 Token

```kotlin
// 登录成功后
val token = loginResponse.data.token
TokenManager.saveToken(token)
TokenManager.saveUserId(userId)
```

---

## 配置项

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `baseUrl` | String | 必填 | API 根地址 |
| `tokenProvider` | `(() -> String?)?` | null | Token 提供者回调 |
| `onTokenExpired` | `((Context) -> Unit)?` | null | 401 时的回调 |
| `errorDecoder` | `NetworkErrorDecoder?` | null | 错误消息解码器 |
| `connectTimeout` | Long | 15 | 连接超时（秒） |
| `readTimeout` | Long | 15 | 读取超时（秒） |
| `enableLogging` | Boolean | false | 是否启用 OkHttp 日志 |

---

## 核心 API

### RetrofitKit

| 方法 | 说明 |
|------|------|
| `init(...)` | 初始化网络客户端 |
| `reinitialize(...)` | 重新初始化（切换 baseUrl 等） |
| `create(Class<T>)` | 创建 API Service 实例 |
| `getNetworkErrorMessage(...)` | 根据 Response 获取错误消息 |
| `getWebSocketUrl(baseUrl, path)` | 获取 WebSocket URL |

### TokenManager

| 方法 | 说明 |
|------|------|
| `init(context)` | 初始化加密存储 |
| `saveToken(token)` | 保存 JWT Token |
| `getToken()` | 读取 Token |
| `saveRefreshToken(token)` | 保存 Refresh Token |
| `getRefreshToken()` | 读取 Refresh Token |
| `saveUserId(userId)` | 保存用户 ID |
| `getUserId()` | 读取用户 ID |
| `clearToken()` | 清除所有 Token |
| `isLoggedIn()` | 是否已登录 |

### NetworkErrorDecoder（接口）

```kotlin
interface NetworkErrorDecoder {
    fun getErrorMessage(
        context: Context,
        response: Response?,
        isNetworkAvailable: Boolean
    ): String
}
```

### PriceTypeAdapter

Gson 的 `TypeAdapter<BigDecimal>`，自动注册到 `RetrofitKit` 内部。确保 JSON 中的金额字段以字符串精度解析，不会出现 `19.99` 变成 `19.989999` 的问题。

---

## 依赖版本

| 库 | 版本 |
|----|------|
| Retrofit | 2.9.0 |
| OkHttp | 4.12.0 |
| Gson | 2.10.1 |
| AndroidX Security | 1.1.0-alpha06 |
| minSdk | 24 |
| targetSdk | 34 |
| compileSdk | 34 |

---

## 架构说明

```
android-retrofit-kit
├── RetrofitKit.kt          -- 主入口：初始化 + API 创建 + 错误处理 + WebSocket URL
├── PriceTypeAdapter.kt     -- Gson TypeAdapter：BigDecimal 精度保护
├── TokenManager.kt         -- 加密 Token 存储（EncryptedSharedPreferences）
└── NetworkErrorDecoder.kt  -- 接口：自定义错误消息解码
```

---

## GitHub About 建议

`Android Retrofit 工具包 | 加密 Token | 价格精度保护 | 动态 URL`
