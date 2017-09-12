package com.jskierbi.cupboardkotlin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jskierbi.cupboard.configureCupboard
import com.jskierbi.cupboard.cupboard
import com.jskierbi.cupboard.query
import com.jskierbi.cupboard.register
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Created by jakub on 12.09.17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class DatabaseIntegrationTest {

  val dbOpenHelper by lazy { DbOpenHelper(RuntimeEnvironment.application) }
  val db by lazy { cupboard(dbOpenHelper.writableDatabase) }

  @Before
  fun setUp() {
    configureCupboard {
      register<EntityWithFinalFields>()
      register<EntityWithEmptyCtor>()
      register<EntityWithoutFinalFields>()
    }
  }

  @After
  fun tearDown() {
    dbOpenHelper.deleteDb()
  }

  @Test
  fun ` insert data - assigns _id field`() {
    // Assemble
    val data = EntityWithFinalFields("field1", 10, true)

    // Act
    db.put(data)
    val result = db.query<EntityWithFinalFields>().get()

    // Assert
    assertNotNull(data._id, "_id field is set")
  }

  @Test
  fun ` entity with final fields - CRUD`() {
    // Assemble
    val initialData = listOf(
      EntityWithFinalFields("field1", 10, true),
      EntityWithFinalFields("field1", 11, false),
      EntityWithFinalFields("field1", 12, true)
    )
    db.put(initialData)

    // Act
    val result = db.query<EntityWithFinalFields>().list()

    // Assert
    assertEquals(initialData, result)
  }

  @Test
  fun ` entity without final fields - CRUD`() {
    // Assemble
    val initialData = listOf(
      EntityWithoutFinalFields("field1", 10),
      EntityWithoutFinalFields("field1", 11),
      EntityWithoutFinalFields("field1", 12)
    )
    db.put(initialData)

    // Act
    val result = db.query<EntityWithoutFinalFields>().list()

    // Assert
    assertEquals(initialData, result)
  }

  @Test
  fun ` entity with empty constructor - CRUD`() {
    // Assemble
    val initialData = listOf(
      EntityWithEmptyCtor().apply {
        field1 = "stth"
        field2 = "sth else"
      },
      EntityWithEmptyCtor().apply {
        field1 = "stth"
        field3 = "sth else"
      }
    )
    db.put(initialData)

    // Act
    val result = db.query<EntityWithEmptyCtor>().list()

    // Assert
    assertEquals(2, result.size)
    assertEquals(initialData[0].field1, result[0].field1)
    assertEquals(initialData[0].field2, result[0].field2)
    assertEquals(initialData[0].field3, result[0].field3)
    assertEquals(initialData[1].field1, result[1].field1)
    assertEquals(initialData[1].field2, result[1].field2)
    assertEquals(initialData[1].field3, result[1].field3)
  }

  @Test
  fun ` query selection`() {
    // Assemble
    val initialData = listOf(
      EntityWithFinalFields("first", 10, true),
      EntityWithFinalFields("second", 11, false),
      EntityWithFinalFields("third", 12, true)
    )
    db.put(initialData)

    // Act
    val result = db.query<EntityWithFinalFields>().withSelection("field1 in (?, ?)", "first", "third").list()

    // Assert
    assertEquals(listOf(initialData[0], initialData[2]), result)
  }

  @Test
  fun ` query by id`() {
    // Assemble
    val initialData = listOf(
      EntityWithFinalFields("first", 10, true),
      EntityWithFinalFields("second", 11, false, _id = 101),
      EntityWithFinalFields("third", 12, true)
    )
    db.put(initialData)

    // Act
    val result = db.query<EntityWithFinalFields>().byId(101).get()

    // Assert
    assertEquals(initialData[1], result)
  }

  data class EntityWithFinalFields(val field1: String,
                                   val field2: Int,
                                   val field3: Boolean,
                                   var _id: Long? = null)

  data class EntityWithoutFinalFields(var field1: String,
                                      var field2: Int,
                                      var _id: Long? = null)

  class EntityWithEmptyCtor {
    var field1: String? = null
    var field2: String? = null
    var field3: String? = null
  }

  class DbOpenHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
      private val DB_NAME = "local.db"
      private val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) = cupboard(db).createTables()
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = cupboard(db).upgradeTables()

    fun deleteDb() = RuntimeEnvironment.application.deleteDatabase(DB_NAME)
  }

}