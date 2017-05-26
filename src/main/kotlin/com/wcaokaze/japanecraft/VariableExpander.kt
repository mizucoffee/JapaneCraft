package com.wcaokaze.japanecraft

import kotlin.coroutines.experimental.buildSequence

class VariableExpander(strWithVars: String) {
  private val tokenExpanderList = parse(strWithVars)

  fun expand(variableMap: Map<String, String>): String {
    return tokenExpanderList
        .map { it.expand(variableMap) }
        .fold(StringBuffer()) { buffer, token -> buffer.append(token) }
        .toString()
  }

  private fun parse(str: String): List<TokenExpander> {
    return buildSequence {
      val buffer = StringBuffer(str)

      yieldToken@ while (buffer.isNotEmpty()) {
        searchDollar@ for (i in 0 until buffer.lastIndex) {
          if (buffer[i] != '$') continue@searchDollar

          if (buffer[i + 1] == '{') {
            val closeBraceIdx = buffer.indexOf("}")
            if (closeBraceIdx == -1) continue@searchDollar

            val constantStr  = buffer.substring(0, i)
            val variableName = buffer.substring(i + 2, closeBraceIdx)
                                     .dropWhile     { it == ' ' }
                                     .dropLastWhile { it == ' ' }

            yield(TokenExpander.ConstantString(constantStr))
            yield(TokenExpander.VariableExpander(variableName))

            buffer.delete(0, closeBraceIdx + 1)
            continue@yieldToken
          } else if (buffer[i + 1].isJavaIdentifierStart()) {
            yield(TokenExpander.ConstantString(buffer.substring(0, i)))
            buffer.delete(0, i + 1)

            val variableName
                = buffer.takeWhile { it.isJavaIdentifierPart() } .toString()

            yield(TokenExpander.VariableExpander(variableName))

            buffer.delete(0, variableName.length)
            continue@yieldToken
          }
        }

        yield(TokenExpander.ConstantString(buffer.toString()))
        buffer.delete(0, buffer.length)
      }
    } .toList()
  }

  private sealed class TokenExpander {
    abstract fun expand(variableMap: Map<String, String>): String

    class ConstantString(private val str: String) : TokenExpander() {
      override fun expand(variableMap: Map<String, String>): String {
        return str
      }
    }

    class VariableExpander(private val variableName: String) : TokenExpander() {
      override fun expand(variableMap: Map<String, String>): String {
        return variableMap.getOrDefault(variableName, "\${$variableName}")
      }
    }
  }
}
