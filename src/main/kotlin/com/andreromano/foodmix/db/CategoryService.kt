package com.andreromano.foodmix.db

import com.andreromano.foodmix.models.Categories
import com.andreromano.foodmix.models.Category
import com.andreromano.foodmix.models.toImageUrl
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CategoryService {

    suspend fun getAll(): List<Category> = newSuspendedTransaction {
        Categories.selectAll().map { it.toCategory() }
    }

    private fun ResultRow.toCategory() = Category(
        id = this[Categories.id].value,
        name = this[Categories.name],
        imageUrl = this[Categories.imageId]?.value?.toImageUrl(),
    )

}