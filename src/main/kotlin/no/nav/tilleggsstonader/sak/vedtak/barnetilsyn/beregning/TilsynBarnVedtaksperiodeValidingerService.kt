package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.mergeSammenhengendeOppfylteVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtaksperiodeValidingerService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val grunnlagsdataService: GrunnlagsdataService,
    @Lazy // For å unngå sirkulær avhenighet i spring
    private val vedtakService: VedtakService,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        behandling: Saksbehandling,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
        validerVedtaksperioderEksisterer(vedtaksperioder)
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifter)

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandling.id)
        validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, vedtaksperioder)

        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteVilkårperioder()

        val fødselsdato =
            grunnlagsdataService
                .hentGrunnlagsdata(behandling.id)
                .grunnlag.fødsel
                ?.fødselsdatoEller1JanForFødselsår()

        vedtaksperioder.forEach {
            validerEnkeltperiode(
                it,
                målgrupper,
                aktiviteter,
                fødselsdato,
            )
        }

        validerIngenEndringerFørRevurderFra(
            vedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling),
            revurderFra = behandling.revurderFra,
        )
    }

    private fun hentForrigeVedtaksperioder(behandling: Saksbehandling): List<Vedtaksperiode>? =
        behandling.forrigeBehandlingId?.let {
            when (val forrigeVedtak = vedtakService.hentVedtak(it)?.data) {
                is InnvilgelseTilsynBarn -> forrigeVedtak.vedtaksperioder
                is OpphørTilsynBarn -> forrigeVedtak.vedtaksperioder
                is Avslag -> null
                else -> error("Håndterer ikke ${forrigeVedtak?.javaClass?.simpleName}")
            }
        }
}
