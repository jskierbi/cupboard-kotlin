package com.jskierbi.cupboard

import android.database.sqlite.SQLiteDatabase
import com.jskierbi.cupboardkotlin.KotlinReflectiveEntityConverter
import nl.qbusict.cupboard.CupboardBuilder
import nl.qbusict.cupboard.CupboardFactory
import nl.qbusict.cupboard.CupboardFactory.cupboard
import nl.qbusict.cupboard.DatabaseCompartment
import nl.qbusict.cupboard.convert.EntityConverterFactory
import nl.qbusict.cupboard.convert.FieldConverter
import nl.qbusict.cupboard.convert.FieldConverterFactory
import kotlin.reflect.KClass

/**
 * Created by q on 30/04/16.
 */
fun cupboard(db: SQLiteDatabase) = cupboard().withDatabase(db)

inline fun configureCupboard(call: CupboardBuilder.() -> Any?) =
    CupboardFactory.setCupboard(CupboardBuilder()
        .register(KotlinReflectiveEntityConverter.Factory)
        .apply { call() }
        .build())

inline fun <reified T : Any> CupboardBuilder.register() = apply { build().register(T::class.java) }
inline fun <reified T : Any> CupboardBuilder.register(fieldConverter: FieldConverter<T>) = registerFieldConverter(T::class.java, fieldConverter)
fun CupboardBuilder.register(vararg types: KClass<*>) = apply { types.forEach { build().register(it.java) } }

fun CupboardBuilder.register(entityConverterFactory: EntityConverterFactory) = registerEntityConverterFactory(entityConverterFactory)
fun CupboardBuilder.register(fieldConverterFactory: FieldConverterFactory) = registerFieldConverterFactory(fieldConverterFactory)

inline fun <reified T : Any> DatabaseCompartment.query() = query(T::class.java)
