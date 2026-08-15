# android-retrofit-kit

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-fallen-leaves089%2Fandroid--retrofit--kit-lightgrey?logo=github)](https://github.com/fallen-leaves089/android-retrofit-kit)
[![Build](https://img.shields.io/github/actions/workflow/status/fallen-leaves089/android-retrofit-kit/ci.yml?branch=main&logo=github)](https://github.com/fallen-leaves089/android-retrofit-kit/actions)

Android Retrofit toolkit | Encrypted token storage | BigDecimal precision protection | Dynamic URLs

MIT License. Copyright (c) 2024 fallen-leaves089.

[中文说明](README.zh-CN.md)

---

## Features

- **Automatic token injection**: adds `Authorization: Bearer <token>` to every request.
- **Global 401 interception**: triggers the `onTokenExpired` callback when a 401 response is received.
- **BigDecimal precision protection**: `PriceTypeAdapter` prevents Gson from parsing monetary fields as `double` and losing precision.
- **Encrypted token storage**: `TokenManager` uses AndroidX EncryptedSharedPreferences and delegates key management to the Android Keystore.
- **Customizable error messages**: the `NetworkErrorDecoder` interface lets you provide your own error message handling.
- **Automatic cookie management**: includes `SimpleCookieJar`.
- **Logging interceptor**: optional OkHttp logging output.

---

## Dependency coordinates

### Option 1: JitPack (available after tagging the repository)

Add the JitPack repository to `dependencyResolutionManagement` in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}
```

Add the dependency in the module-level `build.gradle.kts` (the version matches the Git tag):

```kotlin
implementation("com.github.fallen-leaves089:android-retrofit-kit:1.0.0")
```

> The first JitPack build runs remotely and usually takes 1-2 minutes.

### Option 2: local module (recommended before publishing)

This library has not been published to Maven Central yet. Clone it and include it as a local module:

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

## Quick start

### 1. Initialize in `Application.onCreate`

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize encrypted token storage
        TokenManager.init(this)
        
        // 2. Initialize the network client
        RetrofitKit.init(
            context = this,
            baseUrl = "https://api.example.com/",
            tokenProvider = { TokenManager.getToken() },
            onTokenExpired = { context ->
                // Navigate to the login screen
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
                        500 -> "Server error, please try again later"
                        404 -> "The requested resource does not exist"
                        403 -> "Access denied"
                        else -> if (!isNetworkAvailable) "Network unavailable" else "Request failed"
                    }
                }
            },
            enableLogging = BuildConfig.DEBUG
        )
    }
}
```

### 2. Define an API interface

```kotlin
interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("/api/user/profile")
    suspend fun getProfile(): Response<UserProfile>
}
```

### 3. Make a network request

```kotlin
// Create a service instance
val apiService = RetrofitKit.create(ApiService::class.java)

// Use it from a ViewModel
viewModelScope.launch(Dispatchers.IO) {
    try {
        val response = apiService.getProfile()
        if (response.isSuccessful) {
            // Handle success
        } else {
            val errorMsg = RetrofitKit.getNetworkErrorMessage(
                context, response.raw(), isNetworkAvailable
            )
            // Show the error
        }
    } catch (e: Exception) {
        val errorMsg = RetrofitKit.getNetworkErrorMessage(
            context, null, isNetworkAvailable
        )
    }
}
```

### 4. Save the token after login

```kotlin
// After a successful login
val token = loginResponse.data.token
TokenManager.saveToken(token)
TokenManager.saveUserId(userId)
```

---

## Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `baseUrl` | String | required | API base URL |
| `tokenProvider` | `(() -> String?)?` | null | Token provider callback |
| `onTokenExpired` | `((Context) -> Unit)?` | null | Callback invoked on 401 |
| `errorDecoder` | `NetworkErrorDecoder?` | null | Error message decoder |
| `connectTimeout` | Long | 15 | Connection timeout in seconds |
| `readTimeout` | Long | 15 | Read timeout in seconds |
| `enableLogging` | Boolean | false | Whether to enable OkHttp logging |

---

## Core API

### RetrofitKit

| Method | Description |
|--------|-------------|
| `init(...)` | Initializes the network client |
| `reinitialize(...)` | Reinitializes the client, for example when switching `baseUrl` |
| `create(Class<T>)` | Creates an API service instance |
| `getNetworkErrorMessage(...)` | Gets an error message from a response |
| `getWebSocketUrl(baseUrl, path)` | Gets a WebSocket URL |

### TokenManager

| Method | Description |
|--------|-------------|
| `init(context)` | Initializes encrypted storage |
| `saveToken(token)` | Saves a JWT token |
| `getToken()` | Reads the token |
| `saveRefreshToken(token)` | Saves a refresh token |
| `getRefreshToken()` | Reads the refresh token |
| `saveUserId(userId)` | Saves the user ID |
| `getUserId()` | Reads the user ID |
| `clearToken()` | Clears all tokens |
| `isLoggedIn()` | Returns whether the user is logged in |

### NetworkErrorDecoder (interface)

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

A Gson `TypeAdapter<BigDecimal>` that is automatically registered inside `RetrofitKit`. It ensures monetary fields in JSON are parsed as string-precision decimals, avoiding issues such as `19.99` becoming `19.989999`.

---

## Dependency versions

| Library | Version |
|---------|---------|
| Retrofit | 2.9.0 |
| OkHttp | 4.12.0 |
| Gson | 2.14.0 |
| AndroidX Security | 1.1.0 |
| minSdk | 24 |
| targetSdk | 34 |
| compileSdk | 34 |

---

## Architecture

```
android-retrofit-kit
├── RetrofitKit.kt          -- Main entry point: initialization + API creation + error handling + WebSocket URL
├── RetrofitKitInterceptors.kt -- Authorization and 401 interceptors
├── PriceTypeAdapter.kt     -- Gson TypeAdapter: BigDecimal precision protection
├── TokenManager.kt         -- Encrypted token storage (EncryptedSharedPreferences)
└── NetworkErrorDecoder.kt  -- Interface: custom error message decoding
```

---

## Tests

```bash
./gradlew --no-daemon :retrofitkit:testDebugUnitTest
```

Covers `PriceTypeAdapter`, WebSocket URL conversion, authorization header injection, 401 handling, and `TokenManager` persistence.

---

## Releasing

JitPack builds a release automatically from a Git tag.

```bash
git tag 1.0.0
git push origin 1.0.0
```

Then use:

```text
https://jitpack.io/#fallen-leaves089/android-retrofit-kit/1.0.0
```

Maven Central publishing requires OSSRH credentials, signed artifacts, and source/javadoc jars.

---

## GitHub About

`Android Retrofit toolkit | Encrypted token storage | BigDecimal precision protection | Dynamic URLs`
