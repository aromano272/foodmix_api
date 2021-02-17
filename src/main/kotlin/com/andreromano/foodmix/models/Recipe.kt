package com.andreromano.foodmix.models

import com.andreromano.foodmix.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


object Recipes : IntIdTable() {
    val userId = reference("userId", Users)
    val title = varchar("title", 255)
    val description = varchar("description", 255)
    val imageId = reference("imageId", Images).nullable()
    val calories = integer("calories")
    val servings = integer("servings")
    val duration: Column<Minutes> = integer("duration")

    fun initializeTable() {
        val titles = listOf("Steak with potatoes", "Spaghetti Bolognese", "Pizza margarita")

        titles.forEachIndexed { index, title ->
            Recipes.insert {
                it[userId] = index + 1
                it[Recipes.title] = title
                it[description] = "Some description"
                it[imageId] = null
                it[calories] = 100
                it[servings] = 2
                it[duration] = 30
            }

            RecipeCategories.insert {
                it[recipeId] = index + 1
                it[categoryId] = index + 1
            }

            RecipeCategories.insert {
                it[recipeId] = index + 1
                it[categoryId] = ((index + 1) * 2) + 1
            }
        }
    }

}

object RecipeCategories : Table() {
    val recipeId = reference("recipeId", Recipes)
    val categoryId = reference("categoryId", Categories) // TODO: Should this be nullable?

    override val primaryKey: PrimaryKey = PrimaryKey(recipeId, categoryId)
}

object RecipeIngredients : Table() {
    val recipeId = reference("recipeId", Recipes)
    val ingredientId = reference("ingredientId", Ingredients)

    override val primaryKey: PrimaryKey = PrimaryKey(recipeId, ingredientId)
}

@Serializable
data class Recipe(
    val id: RecipeId,
    val author: User,
    val title: String,
    val description: String,
//    val isFavorite: Boolean,
    val imageUrl: String?,
    val rating: Float,
    val ratingsCount: Int,
    val calories: Int,
    val servings: Int,
    val duration: Minutes,
    val categories: List<Category>,
//    val ingredients: List<Ingredient>,
//    val directions: List<Direction>,
//    val reviews: List<Review>
)
