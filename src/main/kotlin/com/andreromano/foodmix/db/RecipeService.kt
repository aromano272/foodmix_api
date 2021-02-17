package com.andreromano.foodmix.db

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.RecipeId
import com.andreromano.foodmix.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RecipeService {

    suspend fun getAll(): List<Recipe> {
        val categoriesByRecipeId: Map<RecipeId, List<Category>> = getCategories()


        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .selectAll()
                .map { it.toRecipe(categoriesByRecipeId[it[Recipes.id].value].orEmpty()) }
        }
    }

    suspend fun getByRecipe(id: RecipeId): Recipe? {
        val categories = getCategoriesByRecipe(id)

        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .select { Recipes.id eq id }
                .singleOrNull()
                ?.toRecipe(categories)
        }
    }

    suspend fun getAllContainingCategory(id: CategoryId): List<Recipe> {
        val categoriesByRecipeId: Map<RecipeId, List<Category>> = getCategoriesContainingCategory(id)

        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .selectAll()
                .map { it.toRecipe(categoriesByRecipeId[it[Recipes.id].value].orEmpty()) }
        }
    }

    private suspend fun getCategories(): Map<RecipeId, List<Category>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Categories
                .innerJoin(RecipeCategories)
                .selectAll()
                .map { it[RecipeCategories.recipeId].value to it.toCategory() }
        }.fold(mapOf()) { acc, (recipeId, category) ->
            val newCategories = (acc[recipeId] ?: emptyList()) + category
            acc + (recipeId to newCategories)
        }
    }

    private suspend fun getCategoriesByRecipe(id: RecipeId): List<Category> = newSuspendedTransaction {
        Categories
            .innerJoin(RecipeCategories)
            .select { RecipeCategories.recipeId eq id }
            .map { it.toCategory() }
    }

    private suspend fun getCategoriesContainingCategory(id: CategoryId): Map<RecipeId, List<Category>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            val recipeIdsContainingCategory = RecipeCategories
                .slice(RecipeCategories.recipeId)
                .select { RecipeCategories.categoryId eq id }

            Categories
                .innerJoin(RecipeCategories)
                .select { RecipeCategories.recipeId inSubQuery recipeIdsContainingCategory }
                .map { it[RecipeCategories.recipeId].value to it.toCategory() }
        }.fold(mapOf()) { acc, (recipeId, category) ->
            val newCategories = (acc[recipeId] ?: emptyList()) + category
            acc + (recipeId to newCategories)
        }
    }

    private fun buildRecipesFieldSet(): FieldSet {
        val ratingAvg = Ratings.rating.castTo<Float>(FloatColumnType()).avg(2).alias("ratingAvg")
        val ratingCount = Ratings.rating.count().alias("ratingCount")

        val ratingRecipeId = Ratings.recipeId
        val ratingSubQuery = Ratings
            .slice(ratingRecipeId, ratingAvg, ratingCount)
            .selectAll()
            .groupBy(Ratings.recipeId)
            .alias("ratingsSubQuery")

        return Recipes
            .innerJoin(Users)
            // Nested queries: https://github.com/JetBrains/Exposed/issues/248
            .leftJoin(ratingSubQuery, { Recipes.id }, { ratingSubQuery[ratingRecipeId] })
            // Aliasing causes columns to be lost, https://github.com/JetBrains/Exposed/issues/850
            .slice(Recipes.columns + Users.columns + ratingSubQuery.columns + ratingAvg.aliasOnlyExpression() + ratingCount.aliasOnlyExpression())
    }

    private fun ResultRow.toRecipe(
        categories: List<Category>
    ): Recipe {
        val ratingAvg = Ratings.rating.castTo<Float>(FloatColumnType()).avg(2).alias("ratingAvg")
        val ratingCount = Ratings.rating.count().alias("ratingCount")

        return Recipe(
            id = this[Recipes.id].value,
            author = User(
                id = this[Users.id].value,
                name = this[Users.name]
            ),
            title = this[Recipes.title],
            description = this[Recipes.description],
            imageUrl = this[Recipes.imageId]?.value?.toImageUrl(),
            rating = this.getOrNull(ratingAvg.aliasOnlyExpression())?.toFloat() ?: 0f,
            ratingsCount = this.getOrNull(ratingCount.aliasOnlyExpression())?.toInt() ?: 0,
            calories = this[Recipes.calories],
            servings = this[Recipes.servings],
            duration = this[Recipes.duration],
            categories = categories
        )
    }

    private fun ResultRow.toCategory(): Category = Category(
        id = this[RecipeCategories.categoryId].value,
        name = this[Categories.name],
        imageUrl = this[Categories.imageId]?.value?.toImageUrl(),
    )

}