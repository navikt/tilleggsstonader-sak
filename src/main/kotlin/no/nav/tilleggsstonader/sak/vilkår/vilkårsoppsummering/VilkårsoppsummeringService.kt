package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.harBarnUnder2ÅrIStønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.utledAlderNårStønadsperiodeBegynner
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårsoppsummeringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårService: VilkårService,
    private val behandlingFaktaService: BehandlingFaktaService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {

    fun hentVilkårsoppsummering(behandlingId: UUID): VilkårsoppsummeringDto {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)

        return VilkårsoppsummeringDto(
            stønadsperioder = stønadsperioder,
            visVarselKontantstøtte = visVarselForKontantstøtte(behandlingId, stønadsperioder),

            aktivitet = vilkårperioder.aktiviteter.any { it.resultat == ResultatVilkårperiode.OPPFYLT },
            målgruppe = vilkårperioder.målgrupper.any { it.resultat == ResultatVilkårperiode.OPPFYLT },
            stønadsperiode = stønadsperioder.isNotEmpty(),
            passBarn = oppsummeringPassBarnVilkår(behandlingId, stønadsperioder),
        )
    }

    private fun visVarselForKontantstøtte(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>): Boolean {
        if (stønadsperioder.isEmpty()) {
            return false
        }
        val barn = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlag.barn
        return harBarnUnder2ÅrIStønadsperiode(barn, stønadsperioder)
    }

    private fun oppsummeringPassBarnVilkår(
        behandlingId: UUID,
        stønadsperioder: List<StønadsperiodeDto>,
    ): List<BarnOppsummering> {
        val passBarnVilkår = vilkårService.hentPassBarnVilkår(behandlingId)
        val fakta = behandlingFaktaService.hentFakta(behandlingId)

        val datoFørsteStønadsperidoe = stønadsperioder.minOfOrNull { it.fom }

        return passBarnVilkår.map { vilkår ->
            val barn = fakta.barn.find { it.barnId == vilkår.barnId }

            feilHvis(barn == null) { "Fant ikke barn med id ${vilkår.barnId}" }

            val alderNårStønadsperiodeBegynner =
                utledAlderNårStønadsperiodeBegynner(barn.registergrunnlag.fødselsdato, datoFørsteStønadsperidoe)
            BarnOppsummering(
                barnId = barn.barnId,
                ident = barn.ident,
                navn = barn.registergrunnlag.navn,
                alder = barn.registergrunnlag.alder,
                alderNårStønadsperiodeBegynner = alderNårStønadsperiodeBegynner,
                oppfyllerAlleVilkår = vilkår.resultat == Vilkårsresultat.OPPFYLT,
            )
        }
    }
}
