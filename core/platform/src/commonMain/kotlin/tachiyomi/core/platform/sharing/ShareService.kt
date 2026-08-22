package tachiyomi.core.platform.sharing

import tachiyomi.core.platform.filesystem.PlatformPath

interface ShareService {
    suspend fun shareText(text: String, title: String? = null)

    suspend fun shareFiles(paths: List<PlatformPath>, title: String? = null)
}
