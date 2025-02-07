package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus
import java.time.LocalDate
import java.util.UUID

data class Vedtaksperiode(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val status: VedtaksperiodeStatus,
) : Periode<LocalDate>,
    KopierPeriode<Vedtaksperiode> {
    init {
        validatePeriode()
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)
}

fun avkortVedtaksperiodeVedOpphør(
    forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørLæremidler>,
    revurderFra: LocalDate,
): List<Vedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(revurderFra.minusDays(1))
