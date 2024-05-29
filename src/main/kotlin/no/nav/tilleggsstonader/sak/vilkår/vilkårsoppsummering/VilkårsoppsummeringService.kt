package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårsoppsummeringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårService: VilkårService,
    private val behandlingFaktaService: BehandlingFaktaService,
) {

    fun hentVilkårsoppsummering(behandlingId: UUID): Vilkårsoppsummering {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        return Vilkårsoppsummering(
            aktivitet = vilkårperioder.aktiviteter.any { it.resultat == ResultatVilkårperiode.OPPFYLT },
            målgruppe = vilkårperioder.målgrupper.any { it.resultat == ResultatVilkårperiode.OPPFYLT },
            stønadsperiode = oppsummerStønadsperioder(behandlingId),
            passBarn = oppsummeringPassBarnVilkår(behandlingId),
        )
    }

    private fun oppsummerStønadsperioder(behandlingId: UUID): Boolean {
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)
        return stønadsperioder.isNotEmpty()
    }

    private fun oppsummeringPassBarnVilkår(behandlingId: UUID): List<BarnOppsummering> {
        val passBarnVilkår = vilkårService.hentPassBarnVilkår(behandlingId)
        val fakta = behandlingFaktaService.hentFakta(behandlingId)

        return passBarnVilkår.map { vilkår ->
            val barn = fakta.barn.find { it.barnId == vilkår.barnId }

            feilHvis(barn == null) { "Fant ikke barn med id ${vilkår.barnId}" }

            BarnOppsummering(
                barnId = barn.barnId,
                ident = barn.ident,
                navn = barn.registergrunnlag.navn,
                alder = barn.registergrunnlag.alder,
                oppfyllerAlleVilkår = vilkår.resultat == Vilkårsresultat.OPPFYLT,
            )
        }
    }
}
