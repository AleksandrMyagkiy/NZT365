package com.nzt365.app

import java.util.Enumeration

internal fun <T> Enumeration<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    while (hasMoreElements()) result += nextElement()
    return result
}
