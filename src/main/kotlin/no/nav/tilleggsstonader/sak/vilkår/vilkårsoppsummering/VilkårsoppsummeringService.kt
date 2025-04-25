package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.harBarnUnder2ÅrIAktivitetsperiode
import org.springframework.stereotype.Service

@Service
class VilkårsoppsummeringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
) {
    fun hentVilkårsoppsummering(behandlingId: BehandlingId): VilkårsoppsummeringDto {
        val vilkårperioder = finnPerioderForOppfylteAktiviteter(behandlingId)

        return VilkårsoppsummeringDto(
            visVarselKontantstøtte = visVarselForKontantstøtte(behandlingId, vilkårperioder),
        )
    }

    private fun visVarselForKontantstøtte(
        behandlingId: BehandlingId,
        aktivitetsperioder: List<Datoperiode>,
    ): Boolean {
        if (aktivitetsperioder.isEmpty()) {
            return false
        }
        val barn = faktaGrunnlagService.hentEnkeltGrunnlag<FaktaGrunnlagPersonopplysninger>(behandlingId).data.barn
        return harBarnUnder2ÅrIAktivitetsperiode(barn, aktivitetsperioder)
    }

    private fun finnPerioderForOppfylteAktiviteter(behandlingId: BehandlingId): List<Datoperiode> =
        vilkårperiodeService
            .hentVilkårperioder(behandlingId)
            .aktiviteter
            .ofType<AktivitetTilsynBarn>()
            .filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
            .map { Datoperiode(fom = it.fom, tom = it.tom) }
}
