package com.jskierbi.cupboardkotlin

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import nl.qbusict.cupboard.Cupboard
import nl.qbusict.cupboard.annotation.Column
import nl.qbusict.cupboard.annotation.Ignore
import nl.qbusict.cupboard.annotation.Index
import nl.qbusict.cupboard.convert.EntityConverter
import nl.qbusict.cupboard.convert.EntityConverterFactory
import nl.qbusict.cupboard.convert.FieldConverter
import nl.qbusict.cupboard.convert.ReflectiveEntityConverter
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinProperty
import kotlin.reflect.primaryConstructor

/**
 * Created by q on 01/05/16.
 */
class KotlinReflectiveEntityConverter<T : Any>(val cupboard: Cupboard,
                                               val entityClass: KClass<T>) : EntityConverter<T> {

  object Factory : EntityConverterFactory {
    override fun <T : Any> create(cupboard: Cupboard, type: Class<T>) =
        if (type.isKotlinClass()) KotlinReflectiveEntityConverter(cupboard, type.kotlin)
        else ReflectiveEntityConverter(cupboard, type)
  }

  val mTableName: String
  val mColumns: List<ReflectColumn>
  val mConstructor: KFunction<T>?
  val mIdColumn: ReflectColumn?

  init {
    mTableName = entityClass.simpleName
        ?: throw IllegalArgumentException("Cannot serialize entity class without name: ${entityClass.toString()}")

    val columns = mutableListOf<ReflectColumn.Builder>()

    entityClass.fields
        .filter { it.isIgnored().not() }
        .forEach { field ->
          val column = ReflectColumn.Builder(cupboard, field)
          if (column.fieldConverter.columnType != null) {
            columns += column
          }
        }

    // constructor for finals (if applicable)
    if (columns.filter { it.isFinal }.isNotEmpty()) {
      if (entityClass.java.isKotlinClass().not()) {
        throw IllegalArgumentException("${entityClass.simpleName} is not a Kotlin class and has final fields. Either ignore final fields or use Kotlin class with primary constructor")
      }
      mConstructor = entityClass.primaryConstructor
          ?: throw IllegalArgumentException("${entityClass.simpleName} has final fields and no primary constructor (required)")
      mConstructor.parameters.forEach { param ->
        columns.find { it.dbColumnName == param.name && it.field.type == param.type.javaType }
            ?.apply { constructorParameter = param }
            ?: if (param.isOptional.not()) throw IllegalArgumentException("${entityClass.simpleName} constructor parameter ${param.name} doesn't map to any final fields")
      }
    } else {
      mConstructor = null
    }

    mColumns = columns
        .filter { it.isFinal.not() || it.constructorParameter != null }
        .mapIndexed { i, builder -> builder.build(i) }

    mIdColumn = mColumns.find { it.isIdColumn }
  }

  override fun getColumns(): List<EntityConverter.Column> = mColumns

  override fun fromCursor(cursor: Cursor): T {
    val result = when {
      mConstructor != null -> mConstructor.callBy(mColumns
          .filter { it.constructorParameter != null }
          .associate { it.constructorParameter!! to it.fromCursor(cursor) })
      else -> entityClass.java.newInstance()
    }

    mColumns
        .filter { it.isFinal.not() }
        .forEach { column ->
          if (column.cursorIndex > cursor.columnCount - 1) return@forEach
          if (cursor.isNull(column.cursorIndex)) {
            if (column.field.type.isPrimitive.not()) {
              column.field.set(result, null)
            }
          } else {
            val value = column.fromCursor(cursor)
            column.field.set(result, value)
          }
        }
    return result
  }

  override fun toValues(obj: T, values: ContentValues) {
    mColumns.forEachIndexed { idx, column ->
      if (column.type == EntityConverter.ColumnType.JOIN) return@forEachIndexed
      val value = column.field.get(obj)
      if (value == null && column != mIdColumn) {
        values.putNull(column.name)
      } else {
        column.fieldConverter.toContentValue(value, column.name, values)
      }
    }
  }

  override fun setId(id: Long?, instance: T) {
    mIdColumn?.field?.set(instance, id)
  }

  override fun getId(instance: T) = mIdColumn?.field?.get(instance) as Long?

  override fun getTable() = mTableName

  protected fun Field.isIgnored() = Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)
      || (cupboard.isUseAnnotations && getAnnotation(Ignore::class.java) != null)
}

/** Reflection resolved table column */
class ReflectColumn
private constructor(name: String,
                    dbIndex: Index?,
                    val cursorIndex: Int,
                    val field: Field,
                    val fieldConverter: FieldConverter<in Any>,
                    val constructorParameter: KParameter?) : EntityConverter.Column(name, fieldConverter.columnType, dbIndex) {

  val isFinal = Modifier.isFinal(field.modifiers)
  val isNullable = field.kotlinProperty?.returnType?.isMarkedNullable?.not() ?: false
  val isIdColumn = name == BaseColumns._ID

  init {
    if (field.isAccessible.not()) field.isAccessible = true
    if (isIdColumn) {
      // _id field checks
      if (isFinal) throw IllegalArgumentException("${field.declaringClass.simpleName}.${BaseColumns._ID} field cannot be final")
      if (isNullable) throw IllegalArgumentException("${field.declaringClass.simpleName}.${BaseColumns._ID} field has to be nullable")
    }
  }

  fun fromCursor(c: Cursor) = fieldConverter.fromCursorValue(c, cursorIndex)

  class Builder(val cupboard: Cupboard,
                val field: Field) {
    @Suppress("UNCHECKED_CAST")
    val fieldConverter = getConverter(field)
    val isFinal = Modifier.isFinal(field.modifiers)
    val dbColumnName = getDebColumnName(field)

    // Modificable params
    var constructorParameter: KParameter? = null

    fun build(cursorIndex: Int) = ReflectColumn(dbColumnName, getDbColumnIndex(field), cursorIndex, field, fieldConverter, constructorParameter)

    fun getDebColumnName(field: Field) = when {
      cupboard.isUseAnnotations -> field.getAnnotation(Column::class.java)?.value ?: field.name
      else -> field.name
    }

    fun getDbColumnIndex(field: Field) = when {
      cupboard.isUseAnnotations -> field.getAnnotation(Index::class.java)
      else -> null
    }

    @Suppress("UNCHECKED_CAST")
    fun getConverter(field: Field) = cupboard.getFieldConverter(field.genericType) as FieldConverter<in Any>?
        ?: throw IllegalArgumentException("Do not know how to convert field ${field.name} of type ${field.type.simpleName} in class ${field.declaringClass.simpleName} ")
  }
}

