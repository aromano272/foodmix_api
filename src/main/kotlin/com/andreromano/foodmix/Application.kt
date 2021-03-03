package com.andreromano.foodmix

import com.andreromano.foodmix.db.*
import com.andreromano.foodmix.models.*
import com.andreromano.foodmix.routes.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }

    val categoryService = CategoryService()
    val imageService = ImageService()
    val ingredientService = IngredientService()
    val recipeService = RecipeService()
    val userProfileService = UserProfileService()

    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    transaction {
        SchemaUtils.create(
            Categories,
            Images,
            Ingredients,
            Users,
            Recipes,
            Ratings,
            RecipeCategories,
            RecipeIngredients,
            Directions
        )

        Users.initializeTable()
        Categories.initializeTable()
        Ingredients.initializeTable()
        Recipes.initializeTable()
        Ratings.initializeTable()
    }

    install(Routing) {
        categories(categoryService)
        images(imageService)
        ingredients(ingredientService)
        recipes(recipeService)
        userProfile(userProfileService)
    }

}
