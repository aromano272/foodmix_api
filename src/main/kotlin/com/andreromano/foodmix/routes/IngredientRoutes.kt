package com.andreromano.foodmix.routes

import com.andreromano.foodmix.db.IngredientService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.ingredients(ingredientService: IngredientService) {
    route("ingredients") {
        get {
            call.respond(ingredientService.get())
        }
    }
}