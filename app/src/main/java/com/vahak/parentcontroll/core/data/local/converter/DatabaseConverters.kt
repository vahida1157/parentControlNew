package com.vahak.parentcontroll.core.data.local.converter

import androidx.room.TypeConverter
import com.vahak.parentcontroll.core.data.local.entity.Gender
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DatabaseConverters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // "YYYY-MM-DD"

    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate {
        return try { LocalDate.parse(dateString, dateFormatter) }
        catch (e: Exception) { LocalDate.now() }
    }

    @TypeConverter
    fun fromGender(gender: Gender): String = gender.name

    @TypeConverter
    fun toGender(name: String): Gender {
        return try { Gender.valueOf(name) }
        catch (e: Exception) { Gender.BOY }
    }
}