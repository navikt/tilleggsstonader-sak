package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
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

    private fun oppsummerVedtaksperioderBoutgifter(fagsakId: FagsakId): List<DetaljertVedtaksperiodeBoutgifter> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørBoutgifter>(fagsakId)
                ?: return emptyList()

        val vedtaksperioderFraBeregningsresultat: List<DetaljertVedtaksperiodeBoutgifter> =
            vedtakForSisteIverksatteBehandling.beregningsresultat.perioder.flatMap { periode ->
                periode.grunnlag.utgifter.flatMap { utgift ->
                    utgift.value.map { it ->
                        DetaljertVedtaksperiodeBoutgifter(
                            fom = if (utgift.key == TypeBoutgift.UTGIFTER_OVERNATTING) it.fom else periode.fom,
                            tom = if (utgift.key == TypeBoutgift.UTGIFTER_OVERNATTING) it.tom else periode.tom,
                            antallMåneder = 1,
                            type = utgift.key,
                            aktivitet = periode.grunnlag.aktivitet,
                            målgruppe = periode.grunnlag.målgruppe,
                            utgift = it.utgift,
                            stønad = periode.stønadsbeløp,
                        )
                    }
                }
            }

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende()
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
