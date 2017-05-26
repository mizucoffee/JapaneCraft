package com.wcaokaze.util

import kotlin.Boolean          as KBoolean
import kotlin.Number           as KNumber
import kotlin.String           as KString
import kotlin.collections.List as KList
import kotlin.collections.Map  as KMap

sealed class JsonDatum<out T> {
  abstract val value: T

  class Number (override val value: KNumber)                     : JsonDatum<KNumber>()
  class String (override val value: KString)                     : JsonDatum<KString> ()
  class Boolean(override val value: KBoolean)                    : JsonDatum<KBoolean>()
  class List   (override val value: KList<JsonDatum<*>>)         : JsonDatum<KList<JsonDatum<*>>>()
  class Map    (override val value: KMap<KString, JsonDatum<*>>) : JsonDatum<KMap<KString, JsonDatum<*>>>()

  object Null : JsonDatum<Nothing>() {
    override val value: Nothing get() = throw NoSuchElementException()
  }
}
