package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import java.time.LocalDate

data class DetaljertVedtaksperiode<DETALJER>(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallMåneder: Int = 1,
    val detaljer: DETALJER,
) : Periode<LocalDate>,
    Mergeable<LocalDate, DetaljertVedtaksperiode<DETALJER>> {
    override fun merge(other: DetaljertVedtaksperiode<DETALJER>): DetaljertVedtaksperiode<DETALJER> =
        this.copy(
            tom = other.tom,
            antallMåneder = this.antallMåneder + 1,
        )

    fun skalMerges(other: DetaljertVedtaksperiode<DETALJER>): Boolean = this.detaljer == other.detaljer && this.påfølgesAv(other)
}

fun <DETALJER> List<DetaljertVedtaksperiode<DETALJER>>.sorterOgMergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { p1, p2 -> p1.skalMerges(p2) }
