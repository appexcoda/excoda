package app.excoda.core.logging

object LxLog {
    private const val baseTag = "LxLog"

    var writers: List<LogWriter> = listOf(LogcatLogWriter())

    inline fun d(source: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            dispatchImpl(LogLevel.Debug, source, message, throwable)
        }
    }

    inline fun i(source: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            dispatchImpl(LogLevel.Info, source, message, throwable)
        }
    }

    fun w(source: String, message: String, throwable: Throwable? = null) {
        dispatchImpl(LogLevel.Warn, source, message, throwable)
    }

    fun e(source: String, message: String, throwable: Throwable? = null) {
        dispatchImpl(LogLevel.Error, source, message, throwable)
    }

    @PublishedApi
    internal fun dispatchImpl(
        level: LogLevel,
        source: String,
        message: String,
        throwable: Throwable?
    ) {
        val tag = "$baseTag:$source"
        writers.forEach { writer ->
            runCatching {
                writer.log(level, tag, message, throwable)
            }
        }
    }

    interface LogWriter {
        fun log(
            level: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?
        )
    }

    enum class LogLevel {
        Debug,
        Info,
        Warn,
        Error
    }
}