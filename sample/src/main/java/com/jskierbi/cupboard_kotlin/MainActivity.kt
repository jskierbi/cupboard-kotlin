package com.jskierbi.cupboard_kotlin

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jskierbi.cupboard.cupboard
import com.jskierbi.cupboard.query

/**
 * Created by q on 03/05/16.
 */
class MainActivity : AppCompatActivity() {

  val db by lazy { SampleDbOpenHelper(this).writableDatabase }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    cupboard(db).query<Complex>().list().forEach {}
  }
}