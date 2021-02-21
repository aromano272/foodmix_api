package com.andreromano.foodmix.extensions

import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*


fun List<PartData>.findFormPart(name: String): String? =
    (this.find { it is PartData.FormItem && it.name == name } as? PartData.FormItem)?.value

fun List<PartData>.filterFormPart(name: String): List<String> =
    this.filter { it is PartData.FormItem && it.name == name }
        .map { (it as PartData.FormItem).value }

fun List<PartData>.filterFormPartStartingWith(name: String): List<String> =
    this.filter { it is PartData.FormItem && it.name?.startsWith(name) == true }
        .map { (it as PartData.FormItem).value }

suspend inline fun List<PartData>.findFilePart(name: String): ByteArray? =
    withContext(Dispatchers.IO) {
        val part = this@findFilePart.find { it is PartData.FileItem && it.name == name } as? PartData.FileItem
        @Suppress("BlockingMethodInNonBlockingContext")
        part?.streamProvider?.invoke()?.readAllBytes()
    }


fun List<PartData>.getFormPartArrayStartingWith(matchingName: String): SortedMap<Int, String> =
    this.mapNotNull {
        val part = it as? PartData.FormItem ?: return@mapNotNull null
        val name = part.name ?: return@mapNotNull null

        val start = "$matchingName["
        val end = "]"

        if (!name.startsWith(start) || !name.endsWith(end)) return@mapNotNull null

        val index = name.substring(start.length, name.length - end.length).toIntOrNull() ?: return@mapNotNull null

        index to part.value
    }.associate { it }
        .toSortedMap()

suspend inline fun List<PartData>.getFilePartArrayStartingWith(matchingName: String): SortedMap<Int, ByteArray> =
    this.mapNotNull {
        val part = it as? PartData.FileItem ?: return@mapNotNull null
        val name = part.name ?: return@mapNotNull null

        val start = "$matchingName["
        val end = "]"

        if (!name.startsWith(start) || !name.endsWith(end)) return@mapNotNull null

        val index = name.substring(start.length, name.length - end.length).toIntOrNull() ?: return@mapNotNull null

        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            val bytes = part.streamProvider().readAllBytes()
            index to bytes
        }
    }.associate { it }
        .toSortedMap()
