package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper
import java.time.LocalDate

data class VedtaksperioderDvhV2(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeTypeDvh,
    val aktivitet: AktivitetTypeDvh,
    val lovverketsMålgruppe: LovverketsMålgruppeDvh,
    // Tilsyn barn
    val antallBarn: Int? = null,
    val barn: BarnDvh.JsonWrapper? = null,
    // Løremidler
    val studienivå: StudienivåDvh? = null,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvhV2>,
    )

    companion object {
        fun fraDomene(
            vedtak: Vedtak,
            barn: List<BehandlingBarn>,
        ): JsonWrapper =
            when (val data = vedtak.data) {
                is InnvilgelseEllerOpphørTilsynBarn ->
                    mapVedtaksperioderTilsynBarn(
                        beregningsresultat = data.beregningsresultat,
                        barnIBehandlingen = barn,
                    )
                is InnvilgelseEllerOpphørLæremidler ->
                    mapVedtaksperioderLæremidler(
                        beregningsresultat = data.beregningsresultat,
                    )
                is AvslagLæremidler, is AvslagTilsynBarn -> JsonWrapper(vedtaksperioder = emptyList())
            }

        private fun mapVedtaksperioderLæremidler(beregningsresultat: BeregningsresultatLæremidler): JsonWrapper =
            JsonWrapper(
                vedtaksperioder =
                    VedtaksperiodeLæremidlerMapper
                        .mapTilVedtaksperiode(beregningsresultat.perioder)
                        .map {
                            VedtaksperioderDvhV2(
                                fom = it.fom,
                                tom = it.tom,
                                målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                                aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                                lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                                studienivå = StudienivåDvh.fraDomene(it.studienivå),
                            )
                        },
            )

        private fun mapVedtaksperioderTilsynBarn(
            beregningsresultat: BeregningsresultatTilsynBarn,
            barnIBehandlingen: List<BehandlingBarn>,
        ) = JsonWrapper(
            vedtaksperioder =
                VedtaksperiodeTilsynBarnMapper
                    .mapTilVedtaksperiode(beregningsresultat.perioder)
                    .map {
                        VedtaksperioderDvhV2(
                            fom = it.fom,
                            tom = it.tom,
                            målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            antallBarn = it.antallBarn,
                            barn = BarnDvh.fraDomene(it.barn.finnFødselsnumre(barnIBehandlingen)),
                        )
                    },
        )

        fun List<BarnId>.finnFødselsnumre(barn: List<BehandlingBarn>) =
            this.mapNotNull { barnId ->
                barn.find { barnId == it.id }?.ident
            }
    }
}
