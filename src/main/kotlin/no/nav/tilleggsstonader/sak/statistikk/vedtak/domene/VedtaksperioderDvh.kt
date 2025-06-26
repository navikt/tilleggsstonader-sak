package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate

data class VedtaksperioderDvh(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetTypeDvh,
    val lovverketsMålgruppe: LovverketsMålgruppeDvh,
    // Tilsyn barn
    val antallBarn: Int? = null,
    val barn: BarnDvh.JsonWrapper? = null,
    // Løremidler
    val studienivå: StudienivåDvh? = null,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvh>,
    )

    companion object {
        fun fraDomene(
            vedtak: Vedtak,
            barn: List<BehandlingBarn>,
        ): JsonWrapper =
            when (val vedtaksdata = vedtak.data) {
                is InnvilgelseEllerOpphørTilsynBarn ->
                    mapVedtaksperioderTilsynBarn(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                        barnIBehandlingen = barn,
                    )

                is InnvilgelseEllerOpphørLæremidler ->
                    mapVedtaksperioderLæremidler(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                    )

                is InnvilgelseEllerOpphørBoutgifter -> mapVedtaksperioderBoutgifter(vedtaksdata)
//                is InnvilgelseEllerOpphørDagligReise -> TODO()

                is AvslagBoutgifter, is AvslagLæremidler, is AvslagTilsynBarn ->
                    JsonWrapper(
                        vedtaksperioder = emptyList(),
                    )
            }

        private fun mapVedtaksperioderLæremidler(beregningsresultat: BeregningsresultatLæremidler): JsonWrapper =
            JsonWrapper(
                vedtaksperioder =
                    VedtaksperiodeLæremidlerMapper
                        .mapTilVedtaksperiode(beregningsresultat.perioder)
                        .map {
                            VedtaksperioderDvh(
                                fom = it.fom,
                                tom = it.tom,
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
                        VedtaksperioderDvh(
                            fom = it.fom,
                            tom = it.tom,
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            antallBarn = it.antallBarn,
                            barn = BarnDvh.fraDomene(it.barn.finnFødselsnumre(barnIBehandlingen)),
                        )
                    },
        )

        private fun mapVedtaksperioderBoutgifter(vedtaksdata: InnvilgelseEllerOpphørBoutgifter) =
            JsonWrapper(
                vedtaksperioder =
                    vedtaksdata.vedtaksperioder.map {
                        VedtaksperioderDvh(
                            fom = it.fom,
                            tom = it.tom,
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        )
                    },
            )

        fun List<BarnId>.finnFødselsnumre(barn: List<BehandlingBarn>) =
            this.mapNotNull { barnId ->
                barn.find { barnId == it.id }?.ident
            }
    }
}

enum class AktivitetTypeDvh {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    INGEN_AKTIVITET,
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) =
            when (vilkårsperiodeType) {
                is AktivitetType -> fraDomene(aktivitetType = vilkårsperiodeType)
                is MålgruppeType -> throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type aktivitet.")
            }

        fun fraDomene(aktivitetType: AktivitetType) =
            when (aktivitetType) {
                AktivitetType.TILTAK -> TILTAK
                AktivitetType.UTDANNING -> UTDANNING
                AktivitetType.REELL_ARBEIDSSØKER -> REELL_ARBEIDSSØKER
                AktivitetType.INGEN_AKTIVITET -> INGEN_AKTIVITET
            }
    }
}
