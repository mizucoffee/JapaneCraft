package com.wcaokaze.util

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
}
