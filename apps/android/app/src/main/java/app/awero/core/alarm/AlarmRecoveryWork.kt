package app.awero.core.alarm

internal fun <T> runIndependently(items: Iterable<T>, action: (T) -> Unit, onFailure: (T, Exception) -> Unit) {
    for (item in items) {
        try {
            action(item)
        } catch (error: Exception) {
            try { onFailure(item, error) } catch (_: Exception) { }
        }
    }
}

internal fun deliverAlarm(startRinging: () -> Unit, reschedule: () -> Unit, onFailure: (Exception) -> Unit) {
    reportFailure(onFailure, startRinging)
    reportFailure(onFailure, reschedule)
}

private inline fun reportFailure(onFailure: (Exception) -> Unit, action: () -> Unit) {
    try {
        action()
    } catch (error: Exception) {
        try { onFailure(error) } catch (_: Exception) { }
    }
}
