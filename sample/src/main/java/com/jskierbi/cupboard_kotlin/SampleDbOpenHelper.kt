package com.jskierbi.cupboard_kotlin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.jskierbi.cupboard.configureCupboard
import com.jskierbi.cupboard.cupboard
import com.jskierbi.cupboard.query
import com.jskierbi.cupboard.register

/**
 * Created by q on 03/05/16.
 */
class SampleDbOpenHelper(context: Context) : SQLiteOpenHelper(context, "sample_db", null, 1) {

  val TAG = this.javaClass.simpleName

  init {
    configureCupboard {
      register<DataWithFintals>()
      register<DataComplex>()
      register<Complex>()
      register<NoDefaultCtor>()
    }
  }

  override fun onCreate(db: SQLiteDatabase) {
    cupboard(db).createTables()

    val d1 = DataWithFintals("hello", "world")
    val d2 = DataWithFintals("jan", "kazmirz")
    cupboard(db).put(d1, d2)
    cupboard(db).query<DataWithFintals>().list().forEach {
      Log.d(TAG, it.toString())
    }

    val dc1 = DataComplex("aaa", 1)
    dc1.notIgnoredField = "aaa +++"
    val dc2 = DataComplex("bbb", 2)
    cupboard(db).put(dc1, dc2)
    cupboard(db).query<DataComplex>().list().forEach {
      Log.d(TAG, it.toString())
    }

    val c1 = Complex("aaa", 1, "this will be ignored")
    val c2 = Complex("bbb", 2)
    c2.notIgnoredField = "asdf"
    cupboard(db).put(c1, c2)
    cupboard(db).query<Complex>().list().forEach {
      Log.d(TAG, it.toString())
    }

    val noDefaultCtor = NoDefaultCtor()
    noDefaultCtor.noFinalField = "not final field"
    cupboard(db).put(noDefaultCtor)
    cupboard(db).query<NoDefaultCtor>().get().apply {
      Log.d(TAG, toString())
    }
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    cupboard(db).upgradeTables()
  }

}