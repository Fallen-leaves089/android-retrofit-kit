package io.github.fallenleaves089.retrofitkit

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token 安全管理器 — 基于 AndroidX EncryptedSharedPreferences 实现 AES-256 加密存储。
 *
 * Token 存储在 Android Keystore 保护的 SharedPreferences 中，避免明文泄露风险。
 *
 * ## 使用示例：
 * ```
 * // 初始化（建议在 Application.onCreate() 中）
 * TokenManager.init(context)
 *
 * // 保存 Token
 * TokenManager.saveToken("eyJhbGciOi...")
 *
 * // 读取 Token
 * val token = TokenManager.getToken()
 *
 * // 清除 Token
 * TokenManager.clearToken()
 * ```
 */
object TokenManager {

    private const val PREFS_NAME = "retrofit_kit_token_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * 初始化 TokenManager。建议在 Application.onCreate() 中调用一次即可。
     *
     * @param context Application Context
     */
    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * 保存 JWT Token。
     */
    fun saveToken(token: String) {
        getPrefs().edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * 保存刷新 Token（如果有）。
     */
    fun saveRefreshToken(refreshToken: String) {
        getPrefs().edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    /**
     * 获取当前 JWT Token，未登录返回 null。
     */
    fun getToken(): String? {
        return getPrefs().getString(KEY_TOKEN, null)
    }

    /**
     * 获取刷新 Token，不存在返回 null。
     */
    fun getRefreshToken(): String? {
        return getPrefs().getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * 保存当前登录用户 ID。
     */
    fun saveUserId(userId: Long) {
        getPrefs().edit().putLong(KEY_USER_ID, userId).apply()
    }

    /**
     * 获取当前登录用户 ID，未登录返回 -1。
     */
    fun getUserId(): Long {
        return getPrefs().getLong(KEY_USER_ID, -1L)
    }

    /**
     * 清除所有 Token 和用户信息。
     */
    fun clearToken() {
        getPrefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }

    /**
     * 判断是否已登录（Token 非空）。
     */
    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException(
            "TokenManager 尚未初始化，请先调用 TokenManager.init(context)"
        )
    }
}
