package me.calebbassham.scenariomanager.slavemarket

internal fun <E> Collection<E>.format(): String {
    val list = this.toList()
    val sb = StringBuilder()

    for (i in 0 until this.size) {
        sb.append(list[i])

        if (i == list.size - 1) {
            continue
        } else if (i == list.size - 2) {
            sb.append(" & ")
        } else {
            sb.append(", ")
        }
    }

    return sb.toString()
}