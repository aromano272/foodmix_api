package com.andreromano.foodmix.db

import com.andreromano.foodmix.UserId
import com.andreromano.foodmix.models.Recipes
import com.andreromano.foodmix.models.UserProfile
import com.andreromano.foodmix.models.Users
import com.andreromano.foodmix.models.toImageUrl
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserProfileService {

    suspend fun get(id: UserId): UserProfile? = newSuspendedTransaction {
        val totalRecipesCount = 0 // TODO
        val totalCookbooksCount = 0 // TODO
        val myRecipesCount = Recipes.userId.count().alias("myRecipesCount")
        val myCookbooksCount = 0 // TODO
        val shoppingListCount = 0 // TODO

        val myRecipesCountSubQuery =
            Recipes
                .slice(Recipes.userId, myRecipesCount)
                .select { Recipes.userId eq id }
                .groupBy(Recipes.userId)
                .alias("myRecipesCountSubQuery")

        Users
            .leftJoin(myRecipesCountSubQuery, { Users.id }, { myRecipesCountSubQuery[Recipes.userId] })
            .slice(Users.columns + myRecipesCount.aliasOnlyExpression())
            .select { Users.id eq id }
            .singleOrNull()
            ?.toUserProfile()
    }

    private fun ResultRow.toUserProfile(): UserProfile {
        val myRecipesCount = Recipes.userId.count().alias("myRecipesCount")

        return UserProfile(
            id = this[Users.id].value,
            username = this[Users.name],
            description = "TODO",
            avatarUrl = this[Users.avatarImageId]?.value?.toImageUrl(),
            backgroundUrl = this[Users.backgroundImageId]?.value?.toImageUrl(),
            totalRecipesCount = 0,
            totalCookbooksCount = 0,
            myRecipesCount = this.getOrNull(myRecipesCount.aliasOnlyExpression())?.toInt() ?: 0,
            myCookbooksCount = 0,
            shoppingListCount = 0,
        )
    }


}