package no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import java.time.LocalDate
import java.util.UUID

data class Vedtaksperiode(
    override val id: UUID = UUID.randomUUID(),
    override val fom: LocalDate,
    override val tom: LocalDate,
    val status: VedtaksperiodeStatus = VedtaksperiodeStatus.NY,
) : Periode<LocalDate>,
    KopierPeriode<Vedtaksperiode>,
    PeriodeMedId {
    init {
        validatePeriode()
    }

    override fun kopier(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)
}

enum class VedtaksperiodeStatus {
    NY,
    ENDRET,
    UENDRET,
}

// fun avkortVedtaksperiodeVedOpphør(
//    forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørBoutgifter>,
//    revurderFra: LocalDate,
// ): List<Vedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(revurderFra.minusDays(1))
//
