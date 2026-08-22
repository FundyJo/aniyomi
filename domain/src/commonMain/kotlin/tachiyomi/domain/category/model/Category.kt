package tachiyomi.domain.category.model

import tachiyomi.domain.serialization.DomainSerializable

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
    val hidden: Boolean,
) : DomainSerializable {

    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID

    companion object {
        const val UNCATEGORIZED_ID = 0L
    }
}
