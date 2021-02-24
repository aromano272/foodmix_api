package com.andreromano.foodmix.models

import kotlinx.serialization.Serializable

@Serializable
enum class RecipeOrderBy {
    RELEVANCE,
    RATING,
    DURATION
}