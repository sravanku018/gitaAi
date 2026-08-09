@file:Suppress("unused")

package android.util

object Log {
    const val DEBUG = 3
    const val ERROR = 6
    const val INFO = 4
    const val WARN = 5

    @JvmStatic
    fun d(tag: String?, msg: String?): Int {
        // Print to stdout to aid debugging in tests
        if (tag != null && msg != null) println("D/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun d(tag: String?, msg: String?, tr: Throwable?): Int {
        if (tag != null && msg != null) println("D/$tag: $msg ${tr?.message ?: ""}")
        return 0
    }

    @JvmStatic
    fun e(tag: String?, msg: String?): Int {
        if (tag != null && msg != null) System.err.println("E/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, tr: Throwable?): Int {
        if (tag != null && msg != null) System.err.println("E/$tag: $msg ${tr?.message ?: ""}")
        return 0
    }

    @JvmStatic
    fun i(tag: String?, msg: String?): Int {
        if (tag != null && msg != null) println("I/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun i(tag: String?, msg: String?, tr: Throwable?): Int {
        if (tag != null && msg != null) println("I/$tag: $msg ${tr?.message ?: ""}")
        return 0
    }

    @JvmStatic
    fun w(tag: String?, msg: String?): Int {
        if (tag != null && msg != null) System.err.println("W/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun w(tag: String?, msg: String?, tr: Throwable?): Int {
        if (tag != null && msg != null) System.err.println("W/$tag: $msg ${tr?.message ?: ""}")
        return 0
    }

    @JvmStatic
    fun v(tag: String?, msg: String?): Int {
        if (tag != null && msg != null) println("V/$tag: $msg")
        return 0
    }

    @JvmStatic
    fun v(tag: String?, msg: String?, tr: Throwable?): Int {
        if (tag != null && msg != null) println("V/$tag: $msg ${tr?.message ?: ""}")
        return 0
    }
}
