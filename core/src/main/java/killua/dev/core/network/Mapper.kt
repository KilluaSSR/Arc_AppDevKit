package killua.dev.core.network

interface MappableTo<out O> {
    fun toDomain(): O
}

fun <T : MappableTo<O>, O> Iterable<T>.toDomain(): List<O> {
    return this.map { it.toDomain() }
}