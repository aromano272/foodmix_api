package com.andreromano.foodmix.models

import com.andreromano.foodmix.ImageId
import com.andreromano.foodmix.ImageUrl
import org.jetbrains.exposed.dao.id.IdTable
import java.net.NetworkInterface
import java.util.*

object Images : IdTable<String>() {
    override val id = char("id", length = 36).clientDefault { UUID.randomUUID().toString() }.entityId()
    val image = blob("image")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

inline class Image(val value: ByteArray)

fun ImageId.toImageUrl(): ImageUrl {
    val hostAddress = NetworkInterface.getNetworkInterfaces().toList().first().inetAddresses.toList().first().hostAddress
    return "http://$hostAddress:8080/images/$this"
}
