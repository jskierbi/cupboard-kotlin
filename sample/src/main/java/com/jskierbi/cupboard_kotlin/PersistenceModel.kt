package com.jskierbi.cupboard_kotlin

/**
 * Created by q on 03/05/16.
 */
data class DataWithFintals(val field1: String,
                           val field2: String)

data class DataComplex(val field1: String,
                       val field2: Int,
                       var _id: Long? = null) {
  val ignoredFinal = "Ignored!"
  var notIgnoredField = "notIgnored"
}

class Complex(val field1: String,
              val field2: Int,
              ignoredDefault: String = "ignored",
              var _id: Long? = null) {
  val ignoredFinal = "Ignored!"
  var notIgnoredField = "Not ignored"
}

class NoDefaultCtor {
  var noFinalField = ""
  val finalField = ""
}