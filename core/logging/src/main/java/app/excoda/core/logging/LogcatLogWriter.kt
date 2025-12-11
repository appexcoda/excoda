package app.excoda.core.logging

import android.util.Log

class LogcatLogWriter : LxLog.LogWriter {
    override fun log(
        level: LxLog.LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        when (level) {
            LxLog.LogLevel.Debug -> Log.d(tag, message, throwable)
            LxLog.LogLevel.Info -> Log.i(tag, message, throwable)
            LxLog.LogLevel.Warn -> Log.w(tag, message, throwable)
            LxLog.LogLevel.Error -> Log.e(tag, message, throwable)
        }
    }
}