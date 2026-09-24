package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
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
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetTypeDvh,
    val lovverketsMålgruppe: LovverketsMålgruppeDvh,
    /**
     * Er `null` når [no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle.KNYTT_ANDEL_TIL_VEDTAKSPERIODE]
     * er avskrudd, slik at feltet ikke er med i json-en som sendes til DVH (se [JsonInclude]).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val id: VedtaksperiodeId? = null,
    // Tilsyn barn
    val antallBarn: Int? = null,
    val barn: BarnDvh.JsonWrapper? = null,
    // Løremidler
    val studienivå: StudienivåDvh? = null,
) : Periode<LocalDate> {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvh>,
    )

    companion object {
        fun fraDomene(
            vedtak: Vedtak,
            barn: List<BehandlingBarn>,
            behandlingId: BehandlingId,
            knyttAndelTilVedtaksperiode: Boolean = true,
        ): JsonWrapper =
            when (val vedtaksdata = vedtak.data) {
                is InnvilgelseEllerOpphørPassAvBarn ->
                    mapVedtaksperioderPassAvBarn(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                        barnIBehandlingen = barn,
                        behandlingId = behandlingId,
                        knyttAndelTilVedtaksperiode = knyttAndelTilVedtaksperiode,
                    )

                is InnvilgelseEllerOpphørLæremidler ->
                    mapVedtaksperioderLæremidler(
                        beregningsresultat = vedtaksdata.beregningsresultat,
                        behandlingId = behandlingId,
                        knyttAndelTilVedtaksperiode = knyttAndelTilVedtaksperiode,
                    )

                is InnvilgelseEllerOpphørBoutgifter -> mapVedtaksperioderBoutgifter(vedtaksdata, knyttAndelTilVedtaksperiode)

                is InnvilgelseEllerOpphørDagligReise -> mapVedtaksperioderDagligReise(vedtaksdata, knyttAndelTilVedtaksperiode)
                is InnvilgelseEllerOpphørReiseTilSamling ->
                    mapVedtaksperioderReiseTilSamling(vedtaksdata, knyttAndelTilVedtaksperiode)
                is InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise ->
                    mapVedtaksperioderReiseOppstartAvslutningHjemreise(vedtaksdata, knyttAndelTilVedtaksperiode)

                is AvslagBoutgifter, is AvslagLæremidler, is AvslagPassAvBarn, is AvslagDagligReise, is AvslagReiseTilSamling ->
                    JsonWrapper(
                        vedtaksperioder = emptyList(),
                    )
            }

        private fun mapVedtaksperioderLæremidler(
            beregningsresultat: BeregningsresultatLæremidler,
            behandlingId: BehandlingId,
            knyttAndelTilVedtaksperiode: Boolean,
        ): JsonWrapper =
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
                                id =
                                    if (knyttAndelTilVedtaksperiode) {
                                        VedtaksperiodeDvhIdUtil.genererDeterministiskIdLæremidler(
                                            behandlingId = behandlingId,
                                            fom = it.fom,
                                            tom = it.tom,
                                            faktiskMålgruppe = it.målgruppe,
                                            aktivitetType = it.aktivitet,
                                            studienivå = it.studienivå,
                                        )
                                    } else {
                                        null
                                    },
                                studienivå = StudienivåDvh.fraDomene(it.studienivå),
                            )
                        },
            )

        private fun mapVedtaksperioderPassAvBarn(
            beregningsresultat: BeregningsresultatPassAvBarn,
            barnIBehandlingen: List<BehandlingBarn>,
            behandlingId: BehandlingId,
            knyttAndelTilVedtaksperiode: Boolean,
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
                            id =
                                if (knyttAndelTilVedtaksperiode) {
                                    VedtaksperiodeDvhIdUtil.genererDeterministiskIdPassAvBarn(
                                        behandlingId = behandlingId,
                                        fom = it.fom,
                                        tom = it.tom,
                                        faktiskMålgruppe = it.målgruppe,
                                        aktivitetType = it.aktivitet,
                                        antallBarn = it.antallBarn,
                                    )
                                } else {
                                    null
                                },
                            antallBarn = it.antallBarn,
                            barn = BarnDvh.fraDomene(it.barn.finnFødselsnumre(barnIBehandlingen)),
                        )
                    },
        )

        private fun mapVedtaksperioderBoutgifter(
            vedtaksdata: InnvilgelseEllerOpphørBoutgifter,
            knyttAndelTilVedtaksperiode: Boolean,
        ) = JsonWrapper(
            vedtaksperioder =
                vedtaksdata.vedtaksperioder.map {
                    VedtaksperioderDvh(
                        fom = it.fom,
                        tom = it.tom,
                        aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        id = it.id.takeIf { knyttAndelTilVedtaksperiode },
                    )
                },
        )

        private fun mapVedtaksperioderDagligReise(
            vedtaksdata: InnvilgelseEllerOpphørDagligReise,
            knyttAndelTilVedtaksperiode: Boolean,
        ) = JsonWrapper(
            vedtaksperioder =
                vedtaksdata.vedtaksperioder.map {
                    VedtaksperioderDvh(
                        fom = it.fom,
                        tom = it.tom,
                        aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        id = it.id.takeIf { knyttAndelTilVedtaksperiode },
                    )
                },
        )

        private fun mapVedtaksperioderReiseTilSamling(
            vedtaksdata: InnvilgelseEllerOpphørReiseTilSamling,
            knyttAndelTilVedtaksperiode: Boolean,
        ) = JsonWrapper(
            vedtaksperioder =
                vedtaksdata.vedtaksperioder.map {
                    VedtaksperioderDvh(
                        fom = it.fom,
                        tom = it.tom,
                        aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        id = it.id.takeIf { knyttAndelTilVedtaksperiode },
                    )
                },
        )

        private fun mapVedtaksperioderReiseOppstartAvslutningHjemreise(
            vedtaksdata: InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise,
            knyttAndelTilVedtaksperiode: Boolean,
        ) = JsonWrapper(
            vedtaksperioder =
                vedtaksdata.vedtaksperioder.map {
                    VedtaksperioderDvh(
                        fom = it.fom,
                        tom = it.tom,
                        aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                        id = it.id.takeIf { knyttAndelTilVedtaksperiode },
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
