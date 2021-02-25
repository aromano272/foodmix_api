package com.andreromano.foodmix.db

import com.andreromano.foodmix.IngredientType
import com.andreromano.foodmix.models.Ingredient
import com.andreromano.foodmix.models.Ingredients
import com.andreromano.foodmix.models.toImageUrl
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class IngredientService {

    suspend fun get(searchString: String? = null): List<Ingredient> = newSuspendedTransaction {
        if (searchString.isNullOrEmpty())
            Ingredients
                .selectAll()
                .map { it.toIngredient() }
        else
            Ingredients
                .select { Ingredients.name.lowerCase() like searchString.toLowerCase() }
                .map { it.toIngredient() }
    }

    suspend fun getTypes(): List<IngredientType> = newSuspendedTransaction {
        Ingredients
            .slice(Ingredients.type)
            .selectAll()
            .distinct()
            .map { it[Ingredients.type] }
    }

    private fun ResultRow.toIngredient(): Ingredient = Ingredient(
        id = this[Ingredients.id].value,
        name = this[Ingredients.name],
        imageUrl = this[Ingredients.imageId]?.value?.toImageUrl(),
        type = this[Ingredients.type]
    )

}


