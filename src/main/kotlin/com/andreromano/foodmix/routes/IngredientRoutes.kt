package com.andreromano.foodmix.routes

import com.andreromano.foodmix.db.IngredientService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.ingredients(ingredientService: IngredientService) {
    route("ingredients") {
        get {
            call.respond(ingredientService.get())
        }

        get("/types") {
            call.respond(ingredientService.getTypes())
        }

        get("/{searchQuery}") {
            val searchQuery = call.parameters["searchQuery"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            call.respond(ingredientService.get(searchQuery))
        }
    }
}