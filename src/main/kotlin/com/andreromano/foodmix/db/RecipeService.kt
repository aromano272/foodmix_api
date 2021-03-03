package com.andreromano.foodmix.db

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.IngredientId
import com.andreromano.foodmix.RecipeId
import com.andreromano.foodmix.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RecipeService {

    suspend fun insert(recipe: InsertRecipe): Recipe {
        val recipeId = newSuspendedTransaction {
            val imageId = recipe.image?.let { image ->
                Images.insertAndGetId {
                    it[Images.image] = ExposedBlob(image)
                }
            }

            val recipeId = Recipes.insertAndGetId {
                it[userId] = 1 // TODO
                it[title] = recipe.title
                it[description] = recipe.description
                it[Recipes.imageId] = imageId
                it[calories] = recipe.calories
                it[servings] = recipe.servings
                it[cookingTime] = recipe.cookingTime
            }

            RecipeCategories.batchInsert(recipe.categories) { categoryId ->
                this[RecipeCategories.recipeId] = recipeId
                this[RecipeCategories.categoryId] = categoryId
            }

            RecipeIngredients.batchInsert(recipe.ingredients) { ingredientId ->
                this[RecipeIngredients.recipeId] = recipeId
                this[RecipeIngredients.ingredientId] = ingredientId
            }

            Directions.batchInsert(recipe.directions) { direction ->
                this[Directions.recipeId] = recipeId
                this[Directions.title] = direction.title
                this[Directions.description] = direction.description

                val imageId = direction.image?.let { image ->
                    Images.insertAndGetId {
                        it[Images.image] = ExposedBlob(image)
                    }
                }

                this[Directions.imageId] = imageId
            }.map { it[Directions.id] }

            recipeId
        }
        return newSuspendedTransaction {
            getByRecipe(recipeId.value)!!
        }
    }

    suspend fun getAll(): List<Recipe> {
        val categoriesByRecipeId: Map<RecipeId, List<Category>> = getCategories()
        val ingredientsByRecipeId: Map<RecipeId, List<Ingredient>> = getIngredients()
        val directionsByRecipeId: Map<RecipeId, List<Direction>> = getDirections()

        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .selectAll()
                .map {
                    it.toRecipe(
                        categoriesByRecipeId[it[Recipes.id].value].orEmpty(),
                        ingredientsByRecipeId[it[Recipes.id].value].orEmpty(),
                        directionsByRecipeId[it[Recipes.id].value].orEmpty(),
                    )
                }
        }
    }

    suspend fun getByRecipe(id: RecipeId): Recipe? {
        val categories = getCategoriesByRecipe(id)
        val ingredients = getIngredientsByRecipe(id)
        val directions = getDirectionsByRecipes(listOf(id))[id].orEmpty()

        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .select { Recipes.id eq id }
                .singleOrNull()
                ?.toRecipe(categories, ingredients, directions)
        }
    }

    suspend fun getAllContainingCategory(id: CategoryId): List<Recipe> {
        val categoriesByRecipeId: Map<RecipeId, List<Category>> = getCategoriesContainingCategory(id)
        val ingredientsByRecipeId: Map<RecipeId, List<Ingredient>> = getIngredientsByRecipes(categoriesByRecipeId.keys.toList())
        val directionsByRecipeId: Map<RecipeId, List<Direction>> = getDirectionsByRecipes(categoriesByRecipeId.keys.toList())

        // TODO: Maybe this all should be within a single transaction
        return newSuspendedTransaction {
            buildRecipesFieldSet()
                .select { Recipes.id inList categoriesByRecipeId.keys }
                .map {
                    it.toRecipe(
                        categoriesByRecipeId[it[Recipes.id].value].orEmpty(),
                        ingredientsByRecipeId[it[Recipes.id].value].orEmpty(),
                        directionsByRecipeId[it[Recipes.id].value].orEmpty(),
                    )
                }
        }
    }

    suspend fun getAllContainingAllIngredients(ids: List<IngredientId>, orderBy: RecipeOrderBy): List<Recipe> {
        val ingredientsByRecipeId: Map<RecipeId, List<Ingredient>> = getIngredientsInRecipesContainingAllIngredients(ids)
        val categoriesByRecipeId: Map<RecipeId, List<Category>> = getCategoriesByRecipes(ingredientsByRecipeId.keys.toList())
        val directionsByRecipeId: Map<RecipeId, List<Direction>> = getDirectionsByRecipes(ingredientsByRecipeId.keys.toList())

        return newSuspendedTransaction {
            val query = buildRecipesFieldSet()
                .select { Recipes.id inList ingredientsByRecipeId.keys }

            val orderedQuery = when (orderBy) {
                RecipeOrderBy.RELEVANCE -> query.orderBy(Ratings.rating.count().alias("ratingCount"), SortOrder.DESC)
                RecipeOrderBy.RATING -> query.orderBy(Ratings.rating.castTo<Float>(FloatColumnType()).avg(2).alias("ratingAvg"), SortOrder.DESC)
                RecipeOrderBy.DURATION -> query.orderBy(Recipes.cookingTime)
            }

            orderedQuery.map {
                it.toRecipe(
                    categoriesByRecipeId[it[Recipes.id].value].orEmpty(),
                    ingredientsByRecipeId[it[Recipes.id].value].orEmpty(),
                    directionsByRecipeId[it[Recipes.id].value].orEmpty(),
                )
            }
        }
    }

    private suspend fun getIngredients(): Map<RecipeId, List<Ingredient>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Ingredients
                .innerJoin(RecipeIngredients)
                .selectAll()
                .map { it[RecipeIngredients.recipeId].value to it.toIngredient() }
        }.fold(mapOf()) { acc, (recipeId, ingredient) ->
            val newIngredients = (acc[recipeId] ?: emptyList()) + ingredient
            acc + (recipeId to newIngredients)
        }
    }

    private suspend fun getIngredientsInRecipesContainingAllIngredients(ids: List<IngredientId>): Map<RecipeId, List<Ingredient>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            val recipeIdsContainingAllIngredientsIds: List<IngredientId> = RecipeIngredients
                .slice(RecipeIngredients.recipeId)
                .select { RecipeIngredients.ingredientId inList ids }
                .groupBy(RecipeIngredients.recipeId)
                .having { RecipeIngredients.ingredientId.count() eq ids.size.toLong() }
                .map { it[RecipeIngredients.recipeId].value }

            Ingredients
                .innerJoin(RecipeIngredients)
                .select { RecipeIngredients.recipeId inList recipeIdsContainingAllIngredientsIds }
                .map { it[RecipeIngredients.recipeId].value to it.toIngredient() }
        }.fold(mapOf()) { acc, (recipeId, ingredient) ->
            val newIngredients = (acc[recipeId] ?: emptyList()) + ingredient
            acc + (recipeId to newIngredients)
        }
    }

    private suspend fun getIngredientsByRecipes(ids: List<RecipeId>): Map<RecipeId, List<Ingredient>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Ingredients
                .innerJoin(RecipeIngredients)
                .select { RecipeIngredients.recipeId inList ids }
                .map { it[RecipeIngredients.recipeId].value to it.toIngredient() }
        }.fold(mapOf()) { acc, (recipeId, ingredient) ->
            val newIngredients = (acc[recipeId] ?: emptyList()) + ingredient
            acc + (recipeId to newIngredients)
        }
    }

    private suspend fun getIngredientsByRecipe(id: RecipeId): List<Ingredient> = newSuspendedTransaction {
        Ingredients
            .innerJoin(RecipeIngredients)
            .select { RecipeIngredients.recipeId eq id }
            .map { it.toIngredient() }
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

    private suspend fun getCategoriesByRecipes(ids: List<RecipeId>): Map<RecipeId, List<Category>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Categories
                .innerJoin(RecipeCategories)
                .select { RecipeCategories.recipeId inList ids }
                .map { it[RecipeCategories.recipeId].value to it.toCategory() }
        }.fold(mapOf()) { acc, (recipeId, category) ->
            val newCategories = (acc[recipeId] ?: emptyList()) + category
            acc + (recipeId to newCategories)
        }
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

    private suspend fun getDirections(): Map<RecipeId, List<Direction>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Directions
                .selectAll()
                .map { it[Directions.recipeId].value to it.toDirection() }
        }.fold(mapOf()) { acc, (recipeId, ingredient) ->
            val newIngredients = (acc[recipeId] ?: emptyList()) + ingredient
            acc + (recipeId to newIngredients)
        }
    }

    private suspend fun getDirectionsByRecipes(ids: List<RecipeId>): Map<RecipeId, List<Direction>> = withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            Directions
                .select { Directions.recipeId inList ids }
                .map { it[Directions.recipeId].value to it.toDirection() }
        }.fold(mapOf()) { acc, (recipeId, ingredient) ->
            val newIngredients = (acc[recipeId] ?: emptyList()) + ingredient
            acc + (recipeId to newIngredients)
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
        categories: List<Category>,
        ingredients: List<Ingredient>,
        directions: List<Direction>
    ): Recipe {
        val ratingAvg = Ratings.rating.castTo<Float>(FloatColumnType()).avg(2).alias("ratingAvg")
        val ratingCount = Ratings.rating.count().alias("ratingCount")

        return Recipe(
            id = this[Recipes.id].value,
            author = User(
                id = this[Users.id].value,
                name = this[Users.name],
                avatarUrl = this[Users.avatarImageId]?.value?.toImageUrl(),
                backgroundUrl = this[Users.backgroundImageId]?.value?.toImageUrl(),
            ),
            title = this[Recipes.title],
            description = this[Recipes.description],
            isFavorite = true,
            imageUrl = this[Recipes.imageId]?.value?.toImageUrl(),
            rating = this.getOrNull(ratingAvg.aliasOnlyExpression())?.toFloat() ?: 0f,
            ratingsCount = this.getOrNull(ratingCount.aliasOnlyExpression())?.toInt() ?: 0,
            calories = this[Recipes.calories],
            servings = this[Recipes.servings],
            cookingTime = this[Recipes.cookingTime],
            categories = categories,
            ingredients = ingredients,
            directions = directions,
            reviews = emptyList()
        )
    }

    private fun ResultRow.toCategory(): Category = Category(
        id = this[RecipeCategories.categoryId].value,
        name = this[Categories.name],
        imageUrl = this[Categories.imageId].value.toImageUrl(),
    )

    private fun ResultRow.toIngredient(): Ingredient = Ingredient(
        id = this[RecipeIngredients.ingredientId].value,
        name = this[Ingredients.name],
        imageUrl = this[Ingredients.imageId]?.value?.toImageUrl(),
        type = this[Ingredients.type],
    )

    private fun ResultRow.toDirection(): Direction = Direction(
        id = this[Directions.id].value,
        title = this[Directions.title],
        description = this[Directions.description],
        imageUrl = this[Directions.imageId]?.value?.toImageUrl(),
    )

}