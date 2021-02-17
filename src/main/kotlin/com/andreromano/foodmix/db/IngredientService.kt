package com.andreromano.foodmix.db

import com.andreromano.foodmix.models.Ingredient
import com.andreromano.foodmix.models.Ingredients
import com.andreromano.foodmix.models.toImageUrl
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class IngredientService {

    suspend fun get(): List<Ingredient> = newSuspendedTransaction {
        Ingredients.selectAll().map { it.toIngredient() }
    }

    private fun ResultRow.toIngredient(): Ingredient = Ingredient(
        id = this[Ingredients.id].value,
        name = this[Ingredients.name],
        imageUrl = this[Ingredients.imageId]?.value?.toImageUrl(),
        type = this[Ingredients.type]
    )

}


