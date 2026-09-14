package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.VedtaksperiodePassAvBarnMapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate

data class VedtaksperioderDvh(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitet: AktivitetTypeDvh,
    val lovverketsMålgruppe: LovverketsMålgruppeDvh,
    /**
     * TODO: er foreløpig alltid null for LÆREMIDLER og BARNETILSYN, ettersom disse periodene
     * bygges fra sammenslåtte beregningsperioder som ikke er 1:1 med en lagret [no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode].
     */
    val id: VedtaksperiodeId? = null,
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
                is InnvilgelseEllerOpphørPassAvBarn ->
                    mapVedtaksperioderPassAvBarn(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                        barnIBehandlingen = barn,
                    )

                is InnvilgelseEllerOpphørLæremidler ->
                    mapVedtaksperioderLæremidler(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                    )

                is InnvilgelseEllerOpphørBoutgifter -> mapVedtaksperioderBoutgifter(vedtaksdata)

                is InnvilgelseEllerOpphørDagligReise -> mapVedtaksperioderDagligReise(vedtaksdata)
                is InnvilgelseEllerOpphørReiseTilSamling -> mapVedtaksperioderReiseTilSamling(vedtaksdata)
                is InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise ->
                    mapVedtaksperioderReiseOppstartAvslutningHjemreise(vedtaksdata)

                is AvslagBoutgifter, is AvslagLæremidler, is AvslagPassAvBarn, is AvslagDagligReise, is AvslagReiseTilSamling ->
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
                                // TODO: vedtaksperiode-id er ikke tilgjengelig for læremidler ennå, se doc på feltet
                                id = null,
                                studienivå = StudienivåDvh.fraDomene(it.studienivå),
                            )
                        },
            )

        private fun mapVedtaksperioderPassAvBarn(
            beregningsresultat: BeregningsresultatPassAvBarn,
            barnIBehandlingen: List<BehandlingBarn>,
        ) = JsonWrapper(
            vedtaksperioder =
                VedtaksperiodePassAvBarnMapper
                    .mapTilVedtaksperiode(beregningsresultat.perioder)
                    .map {
                        VedtaksperioderDvh(
                            fom = it.fom,
                            tom = it.tom,
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            // TODO: vedtaksperiode-id er ikke tilgjengelig for barnetilsyn ennå, se doc på feltet
                            id = null,
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
                            id = it.id,
                        )
                    },
            )

        private fun mapVedtaksperioderDagligReise(vedtaksdata: InnvilgelseEllerOpphørDagligReise) =
            JsonWrapper(
                vedtaksperioder =
                    vedtaksdata.vedtaksperioder.map {
                        VedtaksperioderDvh(
                            fom = it.fom,
                            tom = it.tom,
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            id = it.id,
                        )
                    },
            )

        private fun mapVedtaksperioderReiseTilSamling(vedtaksdata: InnvilgelseEllerOpphørReiseTilSamling) =
            JsonWrapper(
                vedtaksperioder =
                    vedtaksdata.vedtaksperioder.map {
                        VedtaksperioderDvh(
                            fom = it.fom,
                            tom = it.tom,
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            id = it.id,
                        )
                    },
            )

        private fun mapVedtaksperioderReiseOppstartAvslutningHjemreise(
            vedtaksdata: InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise,
        ) = JsonWrapper(
            vedtaksperioder =
                vedtaksdata.vedtaksperioder.map {
                    VedtaksperioderDvh(
                        fom = it.fom,
                        tom = it.tom,
                        aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        id = it.id,
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
