package com.andreromano.foodmix.models

import com.andreromano.foodmix.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert


object Recipes : IntIdTable() {
    val userId = reference("userId", Users)
    val title = varchar("title", 255)
    val description = varchar("description", 255)
    val imageId = reference("imageId", Images).nullable()
    val calories = integer("calories")
    val servings = integer("servings")
    val cookingTime: Column<Minutes> = integer("cookingTime")

    fun initializeTable() {
        val titles = listOf("Steak with potatoes", "Spaghetti Bolognese", "Pizza margarita")

        titles.forEachIndexed { i, title ->
            val index = i + 1
            Recipes.insert {
                it[userId] = index
                it[Recipes.title] = title
                it[description] = "Some description"
                it[imageId] = null
                it[calories] = 100
                it[servings] = 2
                it[cookingTime] = 30
            }

            RecipeCategories.insert {
                it[recipeId] = index
                it[categoryId] = index
            }

            RecipeCategories.insert {
                it[recipeId] = index
                it[categoryId] = (index * 2) + 1
            }

            RecipeIngredients.batchInsert(1 * index..4 * index) {
                this[RecipeIngredients.recipeId] = index
                this[RecipeIngredients.ingredientId] = it
            }

            Directions.batchInsert(1..4) {
                this[Directions.recipeId] = index
                this[Directions.title] = "dir title $it"
                this[Directions.description] = "dir description $it"
                this[Directions.imageId] = null
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

data class InsertRecipe(
    val userId: UserId,
    val title: String,
    val description: String,
    val image: ByteArray?,
    val categories: List<CategoryId>,
    val cookingTime: Minutes,
    val calories: Int,
    val servings: Int,
    val ingredients: List<IngredientId>,
    val directions: List<InsertDirection>
)

@Serializable
data class Recipe(
    val id: RecipeId,
    val author: User,
    val title: String,
    val description: String,
    val isFavorite: Boolean,
    val imageUrl: String?,
    val rating: Float,
    val ratingsCount: Int,
    val calories: Int,
    val servings: Int,
    val cookingTime: Minutes,
    val categories: List<Category>,
    val ingredients: List<Ingredient>,
    val directions: List<Direction>,
    val reviews: List<Review>
)
