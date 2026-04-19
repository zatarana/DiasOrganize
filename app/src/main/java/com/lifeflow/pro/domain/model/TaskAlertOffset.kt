package com.lifeflow.pro.domain.model

enum class TaskAlertOffset(val value: String, val minutesBefore: Long) {
    AT_TIME("NO_MOMENTO", 0),
    FIFTEEN_MINUTES("15_MIN", 15),
    ONE_HOUR("1_HORA", 60),
    ONE_DAY("1_DIA", 24 * 60);

    companion object {
        fun default(): TaskAlertOffset = AT_TIME
    }
}
