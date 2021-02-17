package com.andreromano.foodmix.db

import com.andreromano.foodmix.ImageId
import com.andreromano.foodmix.models.Image
import com.andreromano.foodmix.models.Images
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ImageService {

    suspend fun get(id: ImageId): Image? = newSuspendedTransaction {
        Images.select { Images.id eq id }.singleOrNull()?.toImage()
    }

    suspend fun insert(bytes: ByteArray) = newSuspendedTransaction {
        Images.insert {
            it[image] = ExposedBlob(bytes)
        }.get(Images.id)
    }

    private fun ResultRow.toImage() = Image(this[Images.image].bytes)

}