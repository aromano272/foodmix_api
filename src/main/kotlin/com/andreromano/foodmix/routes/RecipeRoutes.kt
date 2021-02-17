package com.andreromano.foodmix.routes

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.RecipeId
import com.andreromano.foodmix.db.RecipeService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

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
    }
}