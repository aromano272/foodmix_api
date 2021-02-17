package com.andreromano.foodmix.models

import com.andreromano.foodmix.Millis
import com.andreromano.foodmix.ReviewId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime


object Reviews : IntIdTable() {
    val recipeId = reference("recipeId", Recipes)
    val userId = reference("userId", Users)

    val comment = varchar("comment", 255)
    val createdDate = datetime("timestamp")

    init {
        index(true, recipeId, userId)
    }
}

@Serializable
data class Review(
    val id: ReviewId,
    val user: User,
    val comment: String,
    val timestamp: Millis
)
