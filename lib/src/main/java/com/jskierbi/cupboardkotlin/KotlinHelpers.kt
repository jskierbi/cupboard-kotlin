package com.jskierbi.cupboardkotlin

/**
 * Helper originally found in com.fasterxml.jackson.module.kotlin pakcage
 * (com.fasterxml.jackson.module:jackson-module-kotlin:2.7.1-2)
 *
 * Created by q on 02/05/16.
 */
private val metadataFqName = "kotlin.Metadata"

fun Class<*>.isKotlinClass(): Boolean = this.declaredConstructors.any {
  this.declaredAnnotations.singleOrNull { it.annotationClass.java.name == metadataFqName } != null
}