package com.andreromano.foodmix.routes

import com.andreromano.foodmix.db.CategoryService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.categories(categoryService: CategoryService) {
    route("categories") {
        get("/") {
            call.respond(categoryService.getAll())
        }
    }
}