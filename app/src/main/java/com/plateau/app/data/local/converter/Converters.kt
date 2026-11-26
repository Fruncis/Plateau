package com.plateau.app.data.local.converter

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.ZoneOffset
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDateTime.ofEpochMilliseconds(it, ZoneOffset.UTC).toLocalDate() }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? = value?.let { LocalDateTime.ofEpochMilliseconds(it, ZoneOffset.UTC) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? = value?.let { json.decodeFromString<Map<String, Any>>(it) }
}