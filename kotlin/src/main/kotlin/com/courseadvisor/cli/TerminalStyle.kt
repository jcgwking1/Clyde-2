package com.courseadvisor.cli

import kotlin.math.roundToInt

object TerminalStyle {
    private val isAnsiSupported = System.console() != null ||
            System.getenv("TERM") != null ||
            System.getProperty("os.name").lowercase().contains("windows")

    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"

    const val FG_CYAN = "\u001B[36m"
    const val FG_BLUE = "\u001B[34m"
    const val FG_GREEN = "\u001B[32m"
    const val FG_YELLOW = "\u001B[33m"
    const val FG_RED = "\u001B[31m"
    const val FG_MAGENTA = "\u001B[35m"
    const val FG_WHITE = "\u001B[37m"

    fun bold(text: String): String = if (isAnsiSupported) "$BOLD$text$RESET" else text
    fun cyan(text: String): String = if (isAnsiSupported) "$FG_CYAN$text$RESET" else text
    fun green(text: String): String = if (isAnsiSupported) "$FG_GREEN$text$RESET" else text
    fun yellow(text: String): String = if (isAnsiSupported) "$FG_YELLOW$text$RESET" else text
    fun blue(text: String): String = if (isAnsiSupported) "$FG_BLUE$text$RESET" else text
    fun dim(text: String): String = if (isAnsiSupported) "$DIM$text$RESET" else text

    fun badge(text: String, color: String): String {
        return if (isAnsiSupported) "$color[$text]$RESET" else "[$text]"
    }

    fun progressBar(percent: Double, width: Int = 28): String {
        val filled = (percent * width / 100.0).roundToInt().coerceIn(0, width)
        val empty = width - filled
        val bar = "#".repeat(filled) + "-".repeat(empty)
        return "[$bar]"
    }
}
