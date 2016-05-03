package com.jskierbi.cupboardkotlin

import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KClass

/**
 * Helper originally found in com.fasterxml.jackson.module.kotlin pakcage
 * (com.fasterxml.jackson.module:jackson-module-kotlin:2.7.1-2)
 *
 * Created by q on 02/05/16.
 */
private val metadataFqName = "kotlin.Metadata"

internal fun Class<*>.isKotlinClass(): Boolean = this.declaredConstructors.any {
  this.declaredAnnotations.singleOrNull { it.annotationClass.java.name == metadataFqName } != null
}

internal val <T : Any> KClass<T>.fields: Array<Field>
  get() = if (this.java.superclass == null) {
    // optimize for the case where an entity is not inheriting from a base class.
    this.java.declaredFields
  } else {
    val allFields = ArrayList<Field>(256)
    var c: Class<*>? = this.java
    while (c != null) {
      allFields.addAll(c.declaredFields)
      c = c.superclass
    }
    allFields.toArray(arrayOfNulls<Field>(allFields.size))
  }
