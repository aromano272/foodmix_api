package com.andreromano.foodmix.models

import com.andreromano.foodmix.CategoryId
import com.andreromano.foodmix.ImageUrl
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

object Categories : IntIdTable() {
    val name = varchar("name", 255)
    val imageId = reference("imageId", Images)

    fun initializeTable() {
        val categories = listOf(
            InsertCategory("Easy dishes", javaClass.classLoader.getResource("database/categories/easy-dishes-pexels-rachel-claire-4992703.jpg")!!.readBytes()),
            InsertCategory("Breakfast", javaClass.classLoader.getResource("database/categories/breakfast-pexels-diva-plavalaguna-5710795.jpg")!!.readBytes()),
            InsertCategory("Dinner", javaClass.classLoader.getResource("database/categories/dinner-pexels-rajesh-tp-1624487.jpg")!!.readBytes()),
            InsertCategory("Salads", javaClass.classLoader.getResource("database/categories/salads-pexels-anete-lusina-6331154.jpg")!!.readBytes()),
            InsertCategory("Dessert", javaClass.classLoader.getResource("database/categories/desserts-pexels-any-lane-5945566.jpg")!!.readBytes()),
            InsertCategory("Soups", javaClass.classLoader.getResource("database/categories/soups-pexels-foodie-factor-539451.jpg")!!.readBytes()),
            InsertCategory("Lunch", javaClass.classLoader.getResource("database/categories/lunch-pexels-william-choquette-2641886.jpg")!!.readBytes())
        )
        categories.forEach { category ->
            val imageId = Images.insertAndGetId {
                it[Images.image] = ExposedBlob(category.image)
            }

            Categories.insert {
                it[name] = category.name
                it[Categories.imageId] = imageId
            }
        }
    }
}

@Serializable
data class Category(
    val id: CategoryId,
    val name: String,
    val imageUrl: ImageUrl?
)

private data class InsertCategory(
    val name: String,
    val image: ByteArray
)
