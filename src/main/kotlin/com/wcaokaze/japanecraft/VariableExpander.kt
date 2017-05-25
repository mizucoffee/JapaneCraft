package com.wcaokaze.japanecraft

class VariableExpander {
  private val tokenExpanderList: List<TokenExpander>

  constructor(strWithVars: String) {

  }

  fun expand(variableMap: Map<String, String>): String {
    return tokenExpanderList
        .map { it.expand(variableMap) }
        .fold(StringBuffer()) { buffer, token -> buffer.append(token) }
        .toString()
  }

  private sealed class TokenExpander {
    abstract fun expand(variableMap: Map<String, String>): String

    private class ConstantString(private val str: String) : TokenExpander() {
      override fun expand(variableMap: Map<String, String>): String {
        return str
      }
    }

    private class VariableExpander
        (private val variableName: String) : TokenExpander()
    {
      override fun expand(variableMap: Map<String, String>): String {
        return variableMap.getOrDefault(variableName, variableName)
      }
    }
  }
}
