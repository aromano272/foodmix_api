package com.andreromano.foodmix.routes

import com.andreromano.foodmix.db.UserProfileService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.userProfile(userProfileService: UserProfileService) {
    route("user_profile") {
        get {
            val userId = 1
            val user = userProfileService.get(userId) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(user)
        }
    }
}