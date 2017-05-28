package com.wcaokaze.util

import java.io.InputStream
import kotlin.Boolean          as KBoolean
import kotlin.Number           as KNumber
import kotlin.String           as KString
import kotlin.collections.List as KList
import kotlin.collections.Map  as KMap

sealed class JsonDatum<out T>(val value: T) {
  class  Number (value: KNumber)                     : JsonDatum<KNumber>                    (value)
  class  String (value: KString)                     : JsonDatum<KString>                    (value)
  class  Boolean(value: KBoolean)                    : JsonDatum<KBoolean>                   (value)
  class  List   (value: KList<JsonDatum<*>>)         : JsonDatum<KList<JsonDatum<*>>>        (value)
  class  Map    (value: KMap<KString, JsonDatum<*>>) : JsonDatum<KMap<KString, JsonDatum<*>>>(value)
  object Null                                        : JsonDatum<Nothing?>                   (null)

  fun getNumber (default: () -> KNumber)                     = if (this is Number)  value else default()
  fun getString (default: () -> KString)                     = if (this is String)  value else default()
  fun getBoolean(default: () -> KBoolean)                    = if (this is Boolean) value else default()
  fun getList   (default: () -> KList<JsonDatum<*>>)         = if (this is List)    value else default()
  fun getMap    (default: () -> KMap<KString, JsonDatum<*>>) = if (this is Map)     value else default()
  fun getNull() = null
}

fun parseJson(inputStream: InputStream): JsonDatum<*> = throw NotImplementedError()
