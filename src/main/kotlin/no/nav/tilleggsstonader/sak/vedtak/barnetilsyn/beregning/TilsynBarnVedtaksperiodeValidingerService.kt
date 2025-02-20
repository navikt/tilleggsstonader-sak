package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerVedtaksperioderEksisterer
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.mergeSammenhengendeOppfylteVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtaksperiodeValidingerService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        behandlingId: BehandlingId,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
        validerVedtaksperioderEksisterer(vedtaksperioder)
        validerIngenOverlappMellomVedtaksperioder(vedtaksperioder)
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifter)

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, vedtaksperioder)

        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteVilkårperioder()

        val fødselsdato =
            grunnlagsdataService
                .hentGrunnlagsdata(behandlingId)
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
    }
}
