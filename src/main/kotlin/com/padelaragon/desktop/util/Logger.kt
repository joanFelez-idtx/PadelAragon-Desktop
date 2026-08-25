package com.padelaragon.desktop.util

/**
 * Minimal desktop replacement for android.util.Log, since Android's Log class
 * isn't available on the JVM/desktop target.
 */
object Logger {
    fun d(tag: String, msg: String) = println("D/$tag: $msg")
    fun w(tag: String, msg: String) = println("W/$tag: $msg")
    fun w(tag: String, msg: String, t: Throwable?) {
        println("W/$tag: $msg${t?.let { " - ${it.message}" } ?: ""}")
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        println("E/$tag: $msg${t?.let { " - ${it.message}" } ?: ""}")
    }
}
