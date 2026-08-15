package io.github.fallenleaves089.retrofitkit

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenManagerTest {

    private lateinit var prefs: FakeSharedPreferences

    @Before
    fun setUp() {
        TokenManager.resetForTesting()
        prefs = FakeSharedPreferences()
        TokenManager.overrideSharedPreferencesForTesting(prefs)
    }

    @Test
    fun tokenDefaultsToNull() {
        assertNull(TokenManager.getToken())
        assertNull(TokenManager.getRefreshToken())
        assertEquals(-1L, TokenManager.getUserId())
    }

    @Test
    fun savesAndReadsTokens() {
        TokenManager.saveToken("access-token")
        TokenManager.saveRefreshToken("refresh-token")

        assertEquals("access-token", TokenManager.getToken())
        assertEquals("refresh-token", TokenManager.getRefreshToken())
    }

    @Test
    fun savesAndReadsUserId() {
        TokenManager.saveUserId(42L)

        assertEquals(42L, TokenManager.getUserId())
    }

    @Test
    fun isLoggedInReflectsTokenPresence() {
        assertFalse(TokenManager.isLoggedIn())

        TokenManager.saveToken("access-token")

        assertTrue(TokenManager.isLoggedIn())
    }

    @Test
    fun clearRemovesAllValues() {
        TokenManager.saveToken("access-token")
        TokenManager.saveRefreshToken("refresh-token")
        TokenManager.saveUserId(42L)

        TokenManager.clearToken()

        assertNull(TokenManager.getToken())
        assertNull(TokenManager.getRefreshToken())
        assertEquals(-1L, TokenManager.getUserId())
        assertFalse(TokenManager.isLoggedIn())
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values

        override fun getString(key: String, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String, defValue: Int): Int {
            return values[key] as? Int ?: defValue
        }

        override fun getLong(key: String, defValue: Long): Long {
            return values[key] as? Long ?: defValue
        }

        override fun getFloat(key: String, defValue: Float): Float {
            return values[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return values[key] as? Boolean ?: defValue
        }

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
            // Not needed for local unit tests.
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
            // Not needed for local unit tests.
        }
    }

    private class FakeEditor(
        private val values: MutableMap<String, Any?>
    ) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            pending[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            applyChanges()
            return true
        }

        override fun apply() {
            applyChanges()
        }

        private fun applyChanges() {
            if (clearAll) {
                values.clear()
            }
            values.putAll(pending)
            removed.forEach(values::remove)
        }
    }
}
