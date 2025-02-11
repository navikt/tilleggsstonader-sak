package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.splitPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.Uke
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class VedtaksperiodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppeType: MålgruppeType,
    val aktivitetType: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeDto> {
    /**
     * Splitter en vedtaksperioder opp i uker (kun hverdager inkludert)
     * Antall dager i uken er oppad begrenset til antall dager i vedtaksperioden som er innenfor uken
     */
    fun tilUke(): Map<Uke, PeriodeMedDager> =
        this.splitPerUke { fom, tom ->
            antallDagerIPeriodeInklusiv(fom, tom)
        }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeDto = this.copy(fom = fom, tom = tom)
}

fun List<VedtaksperiodeDto>.tilVedtaksperiode() = map { Vedtaksperiode(it) }
