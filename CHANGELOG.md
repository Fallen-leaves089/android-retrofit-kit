# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `SECURITY.md` security policy
- `CODE_OF_CONDUCT.md` contributor covenant
- CI runs unit tests alongside `assembleDebug`
- Local unit tests for `PriceTypeAdapter`, WebSocket URL conversion, token injection, 401 interception, and `TokenManager`
- JitPack publishing instructions and `1.0.0` release example

### Changed

- Extracted authorization and 401 interceptors into `RetrofitKitInterceptors.kt` for testability
- Blank tokens no longer produce an `Authorization: Bearer   ` header

## [1.0.0] - 2024

### Added

- Retrofit + OkHttp wrapper with automatic Bearer token injection
- 401 global interceptor with `onTokenExpired` callback
- `PriceTypeAdapter` to preserve BigDecimal price precision
- Encrypted token storage via AndroidX EncryptedSharedPreferences + Keystore
- `NetworkErrorDecoder` interface for custom error messages
- Built-in `SimpleCookieJar` and toggleable OkHttp logging
