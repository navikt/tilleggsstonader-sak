package no.nav.tilleggsstonader.sak.vedtak.felles

sealed interface Vedtak

sealed interface Innvilgelse

data class Yolo(val a: String): Vedtak, Innvilgelse