package com.andreromano.foodmix.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


object Ratings : Table() {
    val recipeId = reference("recipeId", Recipes)
    val userId = reference("userId", Users)

    val rating = integer("rating")

    override val primaryKey: PrimaryKey = PrimaryKey(recipeId, userId)

    fun initializeTable() {
        val ratingsRecipe1 = listOf(4, 3, 3, 5)
        val ratingsRecipe2 = listOf(1, 1, 2)

        ratingsRecipe1.forEachIndexed { index, rating ->
            Ratings.insert {
                it[recipeId] = 1
                it[userId] = index + 1
                it[Ratings.rating] = rating
            }
        }

        ratingsRecipe2.forEachIndexed { index, rating ->
            Ratings.insert {
                it[recipeId] = 2
                it[userId] = index + 1
                it[Ratings.rating] = rating
            }
        }
    }

}
