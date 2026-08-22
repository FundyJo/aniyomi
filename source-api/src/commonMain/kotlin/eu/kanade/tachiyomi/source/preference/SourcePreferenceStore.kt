package eu.kanade.tachiyomi.source.preference

interface SourcePreferenceStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)

    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
