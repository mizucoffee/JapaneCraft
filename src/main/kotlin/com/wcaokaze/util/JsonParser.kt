package com.wcaokaze.util

import java.io.InputStream

private typealias KNumber  = kotlin.Number
private typealias KBoolean = kotlin.Boolean
private typealias KString  = kotlin.String
private typealias KList    = kotlin.collections.List<JsonDatum<*>>
private typealias KMap     = kotlin.collections.Map<KString, JsonDatum<*>>

sealed class JsonDatum<out T>(val value: T) {
  class  Number (value: KNumber)  : JsonDatum<KNumber>  (value)
  class  String (value: KString)  : JsonDatum<KString>  (value)
  class  Boolean(value: KBoolean) : JsonDatum<KBoolean> (value)
  class  List   (value: KList)    : JsonDatum<KList>    (value)
  class  Map    (value: KMap)     : JsonDatum<KMap>     (value)
  object Null                     : JsonDatum<Nothing?> (null)

  fun getNumber (default: () -> KNumber)  = if (this is Number)  value else default()
  fun getString (default: () -> KString)  = if (this is String)  value else default()
  fun getBoolean(default: () -> KBoolean) = if (this is Boolean) value else default()
  fun getList   (default: () -> KList)    = if (this is List)    value else default()
  fun getMap    (default: () -> KMap)     = if (this is Map)     value else default()
  fun getNull() = null
}

fun parseJson(inputStream: InputStream): JsonDatum<*> = throw NotImplementedError()
