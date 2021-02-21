package com.andreromano.foodmix.models

import com.andreromano.foodmix.DirectionId
import com.andreromano.foodmix.ImageId
import com.andreromano.foodmix.ImageUrl
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable


object Directions : IntIdTable() {
    val recipeId = reference("recipeId", Recipes)

    val title = varchar("title", 255)
    val description = varchar("description", 255).nullable()
    val imageId = reference("imageId", Images).nullable()
}

data class InsertDirection(
    val title: String,
    val description: String,
    val image: ByteArray?
)

@Serializable
data class Direction(
    val id: DirectionId,
    val title: String,
    val description: String,
    val imageUrl: ImageUrl?
)
