package com.andreromano.foodmix.db

import com.andreromano.foodmix.models.Categories
import com.andreromano.foodmix.models.Category
import com.andreromano.foodmix.models.toImageUrl
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CategoryService {

    suspend fun get(searchQuery: String? = null): List<Category> = newSuspendedTransaction {
        if (searchQuery.isNullOrEmpty())
            Categories
                .selectAll()
                .map { it.toCategory() }
        else
            Categories
                .select { Categories.name.lowerCase() like searchQuery.toLowerCase() }
                .map { it.toCategory() }
    }

    private fun ResultRow.toCategory() = Category(
        id = this[Categories.id].value,
        name = this[Categories.name],
        imageUrl = this[Categories.imageId]?.value?.toImageUrl(),
    )

}