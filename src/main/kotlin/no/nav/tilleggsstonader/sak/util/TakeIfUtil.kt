package no.nav.tilleggsstonader.sak.util

object TakeIfUtil {

    inline fun <reified T> Any.takeIfType(): T? =
        takeIf { it is T }?.let { it as T }
}
