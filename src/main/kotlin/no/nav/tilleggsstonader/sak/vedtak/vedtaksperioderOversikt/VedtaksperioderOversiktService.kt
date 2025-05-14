package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.BoLøpendeUtgift
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.BoOvernatting
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.BoutgifterVedtaksperioderUtils.finnVedtaksperioderMedLøpendeUtgifter
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.DetaljertVedtaksperiodeBoutgifterV2
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.UtgifterBo
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.sorterOgMergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.tilDetaljertVedtaksperiodeV2
import org.springframework.stereotype.Service

@Service
class VedtaksperioderOversiktService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
) {
    /**
     * Oversikten baserer seg på vedtaksperiodene fra beregningsresultatet, som inneholder mer
     * detaljert informasjon spesifikk for stønadstypen.
     * De er derfor ikke nødvendigvis en til en med vedtaksperiodene som saksbehandler registrerer.
     */
    fun hentVedtaksperioderOversikt(fagsakPersonId: FagsakPersonId): VedtaksperioderOversikt {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return VedtaksperioderOversikt(
            tilsynBarn = fagsaker.barnetilsyn?.let { oppsummerVedtaksperioderTilsynBarn(it.id) } ?: emptyList(),
            læremidler = fagsaker.læremidler?.let { oppsummerVedtaksperioderLæremidler(it.id) } ?: emptyList(),
            boutgifter = fagsaker.boutgifter?.let { oppsummerVedtaksperioderBoutgifter(it.id) } ?: emptyList(),
        )
    }

    private fun oppsummerVedtaksperioderTilsynBarn(fagsakId: FagsakId): List<DetaljertVedtaksperiodeTilsynBarn> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørTilsynBarn>(fagsakId)
                ?: return emptyList()

        val vedtaksperioderFraBeregningsresultat: List<DetaljertVedtaksperiodeTilsynBarn> =
            finnVedtaksperioderFraBeregningsresultatTilsynBarn(vedtakForSisteIverksatteBehandling.beregningsresultat)

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende()
    }

    private fun oppsummerVedtaksperioderLæremidler(fagsakId: FagsakId): List<DetaljertVedtaksperiodeLæremidler> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørLæremidler>(fagsakId)
                ?: return emptyList()

        val vedtaksperioderFraBeregningsresultat: List<DetaljertVedtaksperiodeLæremidler> =
            vedtakForSisteIverksatteBehandling.beregningsresultat.perioder.map { periode ->
                DetaljertVedtaksperiodeLæremidler(
                    fom = periode.fom,
                    tom = periode.tom,
                    antallMåneder = 1,
                    aktivitet = periode.grunnlag.aktivitet,
                    målgruppe = periode.grunnlag.målgruppe,
                    studienivå = periode.grunnlag.studienivå,
                    studieprosent = periode.grunnlag.studieprosent,
                    månedsbeløp = periode.beløp,
                )
            }

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende()
    }

    private fun oppsummerVedtaksperioderBoutgifter(fagsakId: FagsakId): List<DetaljertVedtaksperiodeBoutgifterV2> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørBoutgifter>(fagsakId)
                ?: return emptyList()

        val overnatting =
            finnOvernatting(vedtakForSisteIverksatteBehandling).map { it.tilDetaljertVedtaksperiodeV2() }

        val løpende = finnVedtaksperioderMedLøpendeUtgifter(vedtakForSisteIverksatteBehandling).sorterOgMergeSammenhengende()
            .map { it.tilDetaljertVedtaksperiodeV2() }

        return (overnatting + løpende).sorted()
    }

    private fun finnOvernatting(
        vedtak: InnvilgelseEllerOpphørBoutgifter,
    ): List<BoOvernatting> {
        return vedtak.beregningsresultat.perioder
            .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.UTGIFTER_OVERNATTING) }
            .map { resultatForMåned ->
                BoOvernatting(
                    fom = resultatForMåned.fom,
                    tom = resultatForMåned.tom,
                    aktivitet = resultatForMåned.grunnlag.aktivitet,
                    målgruppe = resultatForMåned.grunnlag.målgruppe,
                    utgifter = overnattingRegnUtDekningAvUtgift(
                        resultatForMåned.grunnlag.utgifter,
                        resultatForMåned.grunnlag.makssats
                    ),
                )
            }
    }

    private fun overnattingRegnUtDekningAvUtgift(
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        makssats: Int
    ): List<UtgifterBo> {
        val utgifterOvernatting = utgifter[TypeBoutgift.UTGIFTER_OVERNATTING] ?: error("Burde ikke være mulig")

        var sumForMåned = 0

        return utgifterOvernatting.map { utgift ->
            val beløpSomDekkes = minOf(utgift.utgift, makssats - sumForMåned)
            sumForMåned += beløpSomDekkes

            UtgifterBo(
                fom = utgift.fom,
                tom = utgift.tom,
                utgift = utgift.utgift,
                beløpSomDekkes = beløpSomDekkes,
            )
        }
    }

    private fun finnLøpende(
        vedtak: InnvilgelseEllerOpphørBoutgifter,
    ): List<BoLøpendeUtgift> {
        return vedtak.beregningsresultat.perioder
            .filter { it.grunnlag.utgifter.containsKey(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG) }
            .map { resultatForMåned ->
                BoLøpendeUtgift(
                    fom = resultatForMåned.fom,
                    tom = resultatForMåned.tom,
                    aktivitet = resultatForMåned.grunnlag.aktivitet,
                    målgruppe = resultatForMåned.grunnlag.målgruppe,
                    utgift = summerLøpendeUtgifterBo(
                        resultatForMåned.grunnlag.utgifter,
                    ),
                    stønad = resultatForMåned.stønadsbeløp,
                )
            }
    }

    private fun summerLøpendeUtgifterBo(utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>): Int {
        return utgifter.flatMap { (type, liste) ->
            liste.filter { type == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || type == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER }
        }.sumOf { it.utgift }
    }

    private fun finnVedtaksperioderFraBeregningsresultatTilsynBarn(beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn) =
        beregningsresultatTilsynBarn.perioder.flatMap { resultatMåned ->
            resultatMåned.grunnlag.vedtaksperiodeGrunnlag
                .map { vedtaksperiodeGrunnlag ->
                    DetaljertVedtaksperiodeTilsynBarn(
                        fom = vedtaksperiodeGrunnlag.vedtaksperiode.fom,
                        tom = vedtaksperiodeGrunnlag.vedtaksperiode.tom,
                        aktivitet = vedtaksperiodeGrunnlag.vedtaksperiode.aktivitet,
                        målgruppe = vedtaksperiodeGrunnlag.vedtaksperiode.målgruppe,
                        antallBarn = resultatMåned.grunnlag.antallBarn,
                        totalMånedsUtgift = resultatMåned.grunnlag.utgifterTotal,
                    )
                }
        }

    private inline fun <reified T : Vedtaksdata> hentVedtaksdataForSisteIverksatteBehandling(fagsakId: FagsakId): T? {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId) ?: return null
        val vedtak = vedtakService.hentVedtak<T>(sisteIverksatteBehandling.id) ?: return null

        return vedtak.data
    }
}
