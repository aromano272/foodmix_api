package com.andreromano.foodmix.models

import com.andreromano.foodmix.ImageUrl
import com.andreromano.foodmix.IngredientId
import com.andreromano.foodmix.IngredientType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert

object Ingredients : IntIdTable() {

    val name = varchar("name", 255)
    val imageId = reference("imageId", Images).nullable()
    val type: Column<IngredientType> = varchar("type", 255)

    fun initializeTable() {
        val json = javaClass.classLoader.getResource("database/ingredients.json")?.readText() ?: return
        val ingredientsMap: Map<String, List<JsonIngredient>> = Json.decodeFromString(json)

        ingredientsMap.forEach { (ingredientType, ingredients) ->
            ingredients.forEach { ingredient ->
                Ingredients.insert {
                    it[name] = ingredient.name
                    it[type] = ingredientType
                }
            }
        }
    }

    @Serializable
    private class JsonIngredient(val name: String)
}

@Serializable
data class Ingredient(
    val id: IngredientId,
    val name: String,
    val imageUrl: ImageUrl?,
    val type: IngredientType
)