package com.vahak.parentcontroll.core.data.local.converter

import androidx.room.TypeConverter
import com.vahak.parentcontroll.core.data.local.entity.Gender
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DatabaseConverters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // "YYYY-MM-DD"
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME // "HH:mm:ss"

    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate {
        return try { LocalDate.parse(dateString, dateFormatter) }
        catch (e: Exception) { LocalDate.now() }
    }

    // --- NEW: LocalTime Converters ---
    @TypeConverter
    fun fromLocalTime(time: LocalTime): String = time.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(timeString: String): LocalTime {
        return try { LocalTime.parse(timeString, timeFormatter) }
        catch (e: Exception) { LocalTime.of(0, 0) } // Default to midnight if corrupt
    }

    @TypeConverter
    fun fromGender(gender: Gender): String = gender.name

    @TypeConverter
    fun toGender(name: String): Gender {
        return try { Gender.valueOf(name) }
        catch (e: Exception) { Gender.BOY }
    }
}