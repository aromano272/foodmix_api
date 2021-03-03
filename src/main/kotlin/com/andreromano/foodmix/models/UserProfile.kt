package com.andreromano.foodmix.models

import com.andreromano.foodmix.ImageUrl
import com.andreromano.foodmix.UserId
import kotlinx.serialization.Serializable


@Serializable
data class UserProfile(
    val id: UserId,
    val username: String,
    val description: String,
    val avatarUrl: ImageUrl?,
    val backgroundUrl: ImageUrl?,

    val totalRecipesCount: Int,
    val totalCookbooksCount: Int,
    val myRecipesCount: Int,
    val myCookbooksCount: Int,
    val shoppingListCount: Int,
)