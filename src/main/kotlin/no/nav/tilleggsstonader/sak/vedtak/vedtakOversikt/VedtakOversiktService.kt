package no.nav.tilleggsstonader.sak.vedtak.vedtakOversikt

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import org.springframework.stereotype.Service
import kotlin.collections.List
import kotlin.collections.sorted

@Service
class VedtakOversiktService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
) {
    fun hentVedtakOversikt(fagsakPersonId: FagsakPersonId): VedtaksperiodeOversikt {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return VedtaksperiodeOversikt(
            tilsynBarn = fagsaker.barnetilsyn?.let { oppsummerVedtaksperioderTilsynBarn(it.id) } ?: emptyList(),
            læremidler = fagsaker.læremidler?.let { oppsummerVedtaksperioderLæremidler(it.id) } ?: emptyList(),
        )
    }

    private fun oppsummerVedtaksperioderTilsynBarn(fagsakId: FagsakId): List<VedtaksperiodeOversiktTilsynBarn> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørTilsynBarn>(fagsakId) ?: return emptyList()

        val vedtaksperioderFraBeregningsresultat: List<VedtaksperiodeOversiktTilsynBarn> =
            finnVedtaksperioderFraBeregningsresultatTilsynBarn(vedtakForSisteIverksatteBehandling.beregningsresultat)

        return vedtaksperioderFraBeregningsresultat
            .sorted()
            .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }
    }

    private fun oppsummerVedtaksperioderLæremidler(fagsakId: FagsakId): List<VedtaksperiodeOversiktLæremidler> {
        val vedtakForSisteIverksatteBehandling =
            hentVedtaksdataForSisteIverksatteBehandling<InnvilgelseEllerOpphørLæremidler>(fagsakId) ?: return emptyList()

        val vedtaksperioderFraBeregningsresultat: List<VedtaksperiodeOversiktLæremidler> =
            vedtakForSisteIverksatteBehandling.beregningsresultat.perioder.map { periode ->
                VedtaksperiodeOversiktLæremidler(
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

        return vedtaksperioderFraBeregningsresultat
            .sorted()
            .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }
    }

    private fun finnVedtaksperioderFraBeregningsresultatTilsynBarn(beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn) =
        // TODO - Kommenter hva som skjer her (Gå igjennom og lære hva som skjer)
        beregningsresultatTilsynBarn.perioder.flatMap { resultatMåned ->
            resultatMåned.grunnlag.vedtaksperiodeGrunnlag
                .map { vedtaksperiodeGrunnlag ->
                    VedtaksperiodeOversiktTilsynBarn(
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
