package com.andreromano.foodmix.routes

import com.andreromano.foodmix.ImageUrl
import com.andreromano.foodmix.db.ImageService
import com.andreromano.foodmix.models.toImageUrl
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.images(imageService: ImageService) {
    route("images") {
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val image = imageService.get(id)

            if (image == null) call.respond(HttpStatusCode.NotFound)
            else call.respondBytes(image.value)
        }

        post {
            val multipart = call.receiveMultipart()
            val imageIds = mutableListOf<ImageUrl>()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            val input = part.streamProvider().readAllBytes()
                            val id = imageService.insert(input)
                            imageIds.add(id.value.toImageUrl())
                        }
                    }
                }

                part.dispose()
            }
            call.respondText(imageIds.joinToString())
        }
    }
}