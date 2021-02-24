package com.andreromano.foodmix.routes

import com.andreromano.foodmix.db.CategoryService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.categories(categoryService: CategoryService) {
    route("categories") {
        get("/") {
            call.respond(categoryService.get())
        }

        get("/{searchQuery}") {
            val searchQuery = call.parameters["searchQuery"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            call.respond(categoryService.get(searchQuery))
        }
    }
}