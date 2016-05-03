# Cupboard Kotlin Module
[ ![Download](https://api.bintray.com/packages/jskierbi/maven/cupboard-kotlin/images/download.svg) ](https://bintray.com/jskierbi/maven/cupboard-kotlin/_latestVersion)

Kotlin language support for [Cupboard](https://bitbucket.org/littlerobots/cupboard) (a simple Android SQLite persistence library)

## Features
* Added handling of data classes and classes with val fields defined on constructor level (aka finals)
* Additional extension functions for convenient cupboard confuguration and querying, using Kotlin specific syntax sugar (lambdas, reified type parameters)

## Installation (build.gradle)
```gradle
repositories {
  maven {
    url  "http://dl.bintray.com/jskierbi/maven"
  }
}

dependencies {
  compile "com.jskierbi:cupboard-kotlin:0.9.0"
  
  // cupboard-kotlin does not include cupboard itself
  compile "nl.qbusict:cupboard:2.1.4" 
  
  // cupboard-kotlin requires both kotln-stdlib and kotlin-reflect
  compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
  compile "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
  
  ...
}
```
## Usage
Given two data classes used for persistence:
```koltin
data class Book(val title: String)
data class Author(val name: String, val lastName: String)
```

### Initialization
```kotlin
class SampleOpenHelper(context: Context) : SQLiteOpenHelper(context, "sample_db", null, 1) {

  init {
    configureCupboard {
      register<Book>()
      register<Author>()
    }
  }

  override fun onCreate(db: SQLiteDatabase) {
    cupboard(db).createTables()
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    cupboard(db).upgradeTables()
  }
}
```

### Querying database
```kotlin
val book = Book("Lord of the Rings")
cupboard(db).put(book)
val bookList = cupboard(db).query<DataWithFintals>().list()
```
License
--------

    Copyright 2016 Jakub Skierbiszewski.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
