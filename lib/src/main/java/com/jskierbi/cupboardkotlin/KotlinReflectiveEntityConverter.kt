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
import java.util.*
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

  private val mUseAnnotations by lazy { cupboard.isUseAnnotations }
  private val mReflectTable by lazy {

    val tableName = entityClass.simpleName
        ?: throw IllegalArgumentException("Cannot serialize entity class without name: ${entityClass.toString()}")
    val allColumns = mutableListOf<ReflectColumn>()
    var indexColumn: ReflectColumn? = null

    getClassFields(entityClass)
        .filter { isIgnored(it).not() }
        .forEachIndexed { idx, field ->
          val genericType = field.genericType
          val converter = cupboard.getFieldConverter(genericType)
              ?: throw IllegalArgumentException("Do not know how to convert field ${field.name}  in entity ${entityClass.simpleName} of type $genericType")
          if (converter.columnType == null) return@forEachIndexed
          if (field.isAccessible.not()) field.isAccessible = true

          val column = ReflectColumn(
              getColumnName(field),
              getDbIndexDef(field),
              idx,
              field,
              converter as FieldConverter<in Any>)

          allColumns.add(column)

          // _id field + checks
          if (BaseColumns._ID == column.name) {
            if (column.isFinal) {
              throw IllegalArgumentException("${entityClass.simpleName}.${BaseColumns._ID} field cannot be final")
            }
            if (column.field.kotlinProperty?.returnType?.isMarkedNullable?.not() ?: false) {
              throw IllegalArgumentException("${entityClass.simpleName}.${BaseColumns._ID} field has to be nullable")
            }
            indexColumn = column
          }
        }

    var constructor =
        // constructor for filans (if applicable)
        if (allColumns.filter { it.isFinal }.isNotEmpty()) {
          if (entityClass.java.isKotlinClass().not()) {
            throw IllegalArgumentException("${entityClass.simpleName} is not a Kotlin class and has final fields. Either ignore final fields or use Kotlin class with primary constructor")
          }
          val ctor = entityClass.primaryConstructor
              ?: throw IllegalArgumentException("${entityClass.simpleName} has final fields and no primary constructor (required)")

          ctor.parameters.forEach { param ->
            val column = allColumns.find { it.name == param.name && it.field.type == param.type.javaType } ?:
                if (param.isOptional) return@forEach // Skip optionals
                else throw IllegalArgumentException("${entityClass.simpleName} constructor parameter ${param.name} doesn't map to any final fields")
            column.ctorParameter = param
          }

          allColumns.forEach { column ->
            if (column.isFinal && column.ctorParameter == null)
              throw IllegalArgumentException("${entityClass.simpleName} final field ${column.name} doesn't map to constructor parameter")
          }

          ctor
        } else {
          null
        }

    ReflectTable(tableName, allColumns, indexColumn, constructor)
  }

  override fun getColumns(): List<EntityConverter.Column> = mReflectTable.columns

  override fun fromCursor(cursor: Cursor): T {
    val result = if (mReflectTable.constructor != null) {
      val map = mReflectTable.columns // Create instance using constructor with parameters
          .filter { it.isFinal }
          .associate { it.ctorParameter!! to it.fromCursor(cursor) }
      mReflectTable.constructor?.callBy(map)!!
    } else {
      entityClass.java.newInstance()
    }
    mReflectTable.columns
        .filter { it.isFinal.not() }
        .forEach { column ->
          if (column.cursorIdx > cursor.columnCount - 1) return@forEach
          if (cursor.isNull(column.cursorIdx)) {
            if (column.field.type.isPrimitive.not()) {
              column.field.set(result, null)
            }
          } else {
            val value = column.fieldConverter.fromCursorValue(cursor, column.cursorIdx)
            column.field.set(result, value)
          }
        }
    return result
  }

  override fun toValues(obj: T, values: ContentValues) {
    mReflectTable.columns.forEachIndexed { idx, column ->
      if (column.type == EntityConverter.ColumnType.JOIN) return@forEachIndexed
      val value = column.field.get(obj)
      if (value == null && column != mReflectTable.indexColumn) {
        values.putNull(column.name)
      } else {
        column.fieldConverter.toContentValue(value, column.name, values)
      }
    }
  }

  override fun setId(id: Long?, instance: T) {
    mReflectTable.indexColumn?.field?.set(instance, id)
  }

  override fun getId(instance: T) = mReflectTable.indexColumn?.field?.get(instance) as Long?

  override fun getTable() = mReflectTable.name

  protected fun isIgnored(field: Field) =
      Modifier.isStatic(field.modifiers)
          || Modifier.isTransient(field.modifiers)
          || (mUseAnnotations && field.getAnnotation(Ignore::class.java) != null)

  protected fun getColumnName(field: Field) =
      if (mUseAnnotations) {
        field.getAnnotation(Column::class.java)?.value ?: field.name
      } else {
        field.name
      }

  protected fun getDbIndexDef(field: Field) =
      if (mUseAnnotations) {
        field.getAnnotation(Index::class.java)
      } else {
        null
      }

  protected fun getClassFields(kClass: KClass<T>) =
      if (kClass.java.superclass == null) {
        // optimize for the case where an entity is not inheriting from a base class.
        kClass.java.declaredFields
      } else {
        val allFields = ArrayList<Field>(256)
        var c: Class<*>? = kClass.java
        while (c != null) {
          allFields.addAll(c.declaredFields)
          c = c.superclass
        }
        allFields.toArray(arrayOfNulls<Field>(allFields.size))
      }

  /** Reflection resolved table instance */
  private class ReflectTable<T>(val name: String,
                                val columns: List<ReflectColumn>,
                                val indexColumn: ReflectColumn?,
                                val constructor: KFunction<T>?)

  /** Reflection resolved table column */
  private class ReflectColumn(name: String,
                              dbIndex: Index?,
                              val cursorIdx: Int,
                              val field: Field,
                              val fieldConverter: FieldConverter<in Any>) :
      EntityConverter.Column(name, fieldConverter.columnType, dbIndex) {
    /** Corresponding constructor parameter, if applicable */
    var ctorParameter: KParameter? = null
    val isFinal = Modifier.isFinal(field.modifiers)
    fun fromCursor(c: Cursor) = fieldConverter.fromCursorValue(c, cursorIdx)
  }
}