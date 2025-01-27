package no.nav.tilleggsstonader.sak.util

object EnumUtil {
    fun Any.enumName() = (this as Enum<*>).name
}
