package com.andreromano.foodmix.models

import com.andreromano.foodmix.UserId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


object Users : IntIdTable() {
    val name = varchar("name", 255)

    fun initializeTable() {
        val names = listOf("Andre", "David", "Sarah", "John", "Peter")

        names.forEach { name ->
            Users.insert {
                it[Users.name] = name
            }
        }
    }

}

@Serializable
data class User(
    val id: UserId,
    val name: String
)
