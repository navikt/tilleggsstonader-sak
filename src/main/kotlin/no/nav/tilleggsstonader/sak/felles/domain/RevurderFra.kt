package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.sak.util.norskFormat
import java.time.LocalDate

@JvmInline
value class RevurderFra(
    val dato: LocalDate,
) {
    fun norskFormat(): String = dato.norskFormat()

    fun minusDays(daysToSubtract: Long): RevurderFra = RevurderFra(dato.minusDays(daysToSubtract))

    companion object {
        operator fun RevurderFra.compareTo(other: LocalDate?): Int = this.dato.compareTo(other)

        operator fun RevurderFra.compareTo(other: RevurderFra?): Int = this.compareTo(other?.dato)

        operator fun LocalDate.compareTo(other: RevurderFra?): Int = this.compareTo(other?.dato)
    }
}
