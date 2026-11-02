package net.wshmkr.launcher.util

fun <T> List<T>.movedItem(item: T, target: T): List<T>? {
    val from = indexOf(item)
    val to = indexOf(target)
    if (from < 0 || to < 0 || from == to) return null
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}
