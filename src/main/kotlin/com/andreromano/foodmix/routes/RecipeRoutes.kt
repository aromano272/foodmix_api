package com.andreromano.foodmix.routes

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.IngredientId
import com.andreromano.foodmix.RecipeId
import com.andreromano.foodmix.db.RecipeService
import com.andreromano.foodmix.extensions.*
import com.andreromano.foodmix.models.InsertDirection
import com.andreromano.foodmix.models.InsertRecipe
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.lang.Exception
import java.util.*

fun Route.recipes(recipeService: RecipeService) {
    route("recipes") {

        get() {
            call.respond(recipeService.getAll())
        }

        get("/{id}") {
            val id: RecipeId = call.parameters["id"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val recipe = recipeService.getByRecipe(id) ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(recipe)
        }

        get("/category/{id}") {
            val id: CategoryId = call.parameters["id"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)

            call.respond(recipeService.getAllContainingCategory(id))
        }

        post {
            val parts = call.receiveOrNull(MultiPartData::class)?.readAllParts() ?: return@post call.respond(HttpStatusCode.BadRequest)

            val title = parts.findFormPart("title") ?: return@post call.respond(HttpStatusCode.BadRequest, "title is missing")
            val description = parts.findFormPart("description") ?: return@post call.respond(HttpStatusCode.BadRequest, "description is missing")
            val image = parts.findFilePart("image")
            val categoriesParam: List<IngredientId?> = parts.getFormPartArrayStartingWith("category").values.map { it.toIntOrNull() }
            val categories =
                if (categoriesParam.contains(null)) return@post call.respond(HttpStatusCode.BadRequest, "category ids must be numbers and not null")
                else categoriesParam.filterNotNull()

            val cookingTime = parts.findFormPart("cookingTime")?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "cookingTime must be number and not null")
            val servingsCount = parts.findFormPart("servingsCount")?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "servingsCount must be number and not null")
            val calories = parts.findFormPart("calories")?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "calories must be number and not null")
            val ingredientsParam: List<IngredientId?> = parts.getFormPartArrayStartingWith("ingredient").values.map { it.toIntOrNull() }
            val ingredients =
                if (ingredientsParam.contains(null)) return@post call.respond(HttpStatusCode.BadRequest, "ingredient ids must be numbers and not null")
                else ingredientsParam.filterNotNull()

            val directionTitles = parts.getFormPartArrayStartingWith("direction.title")
            val directionDescriptions = parts.getFormPartArrayStartingWith("direction.description")
            val directionImages = parts.getFilePartArrayStartingWith("direction.image")

            if (directionTitles.keys != directionDescriptions.keys)
                return@post call.respond(HttpStatusCode.BadRequest, "Direction titles and description keys don't match")

            if (!directionTitles.keys.containsAll(directionImages.keys))
                return@post call.respond(HttpStatusCode.BadRequest, "There are direction images missing a title")

            val directions: List<InsertDirection> = try {
                directionTitles.map { (index, title) ->
                    InsertDirection(
                        title = title,
                        description = directionDescriptions[index]!!,
                        image = directionImages[index]
                    )
                }
            } catch (ex: Exception) {
                call.respond(HttpStatusCode.BadRequest, "directions not correctly formatted")
                return@post
            }

            val insertRecipe = InsertRecipe(
                userId = 1, // TODO
                title = title,
                description = description,
                image = image,
                categories = categories,
                cookingTime = cookingTime,
                calories = calories,
                servings = servingsCount,
                ingredients = ingredients,
                directions = directions,
            )
            val recipe = recipeService.insert(insertRecipe)
            call.respond(recipe)
        }

    }
}