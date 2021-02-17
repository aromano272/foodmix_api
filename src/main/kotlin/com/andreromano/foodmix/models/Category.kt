package com.andreromano.foodmix.models

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.ImageUrl
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object Categories : IntIdTable() {
    val name = varchar("name", 255)
    val imageId = reference("imageId", Images).nullable()

    fun initializeTable() {
        val categoryNames = listOf(
            "Easy dishes",
            "Breakfast",
            "Dinner",
            "Salads",
            "Dessert",
            "Soups",
            "Lunch"
        )
        categoryNames.forEach { categoryName ->
            Categories.insert {
                it[name] = categoryName
            }
        }
    }
}

@Serializable
data class Category(
    val id: CategoryId,
    val name: String,
    val imageUrl: ImageUrl?
)
