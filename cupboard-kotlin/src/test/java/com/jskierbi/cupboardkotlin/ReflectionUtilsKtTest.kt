package com.jskierbi.cupboardkotlin

import org.junit.Test
import kotlin.test.expect

/**
 * Created by q on 02/05/16.
 */
class ReflectionUtilsKtTest {

  @Test fun isKotlinClass_kotlinClass_true() {
    expect(false) { JavaClass::class.java.isKotlinClass() }
  }

  @Test fun isKotlinClass_javaClass_false() {
    expect(true) { KotlinClass::class.java.isKotlinClass() }
  }

  @Test fun fields_baseClass_returnAllFields() {
    val fields = KotlinBaseClass::class.fields

    expect(3) { fields.size }
    expect(mapOf("field1" to String::class, "field2" to Int::class, "innerField" to Boolean::class)) {
      fields.associate { it.name to it.type.kotlin }
    }
  }

  @Test fun fields_derivedClass_returnFieldsFromBaseAndDerived() {
    val fields = KotlinDerivedClass::class.fields

    expect(6) { fields.size }
    expect(mapOf("field1" to String::class,
        "field2" to Int::class,
        "innerField" to Boolean::class,
        "childField1" to String::class,
        "childField2" to String::class,
        "childInnerField" to String::class)) {
      fields.associate { it.name to it.type.kotlin }
    }
  }

  class KotlinClass

  open class KotlinBaseClass(val field1: String = "", val field2: Int = 0) {
    val innerField = true
  }

  class KotlinDerivedClass(val childField1: String, val childField2: String) : KotlinBaseClass() {
    val childInnerField = "12"
  }
}