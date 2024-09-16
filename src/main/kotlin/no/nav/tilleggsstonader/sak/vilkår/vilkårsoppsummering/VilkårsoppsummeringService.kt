package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.harBarnUnder2ÅrIStønadsperiode
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårsoppsummeringService(
    private val stønadsperiodeService: StønadsperiodeService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {

    fun hentVilkårsoppsummering(behandlingId: UUID): VilkårsoppsummeringDto {
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)

        return VilkårsoppsummeringDto(
            stønadsperioder = stønadsperioder,
            visVarselKontantstøtte = visVarselForKontantstøtte(behandlingId, stønadsperioder),
        )
    }

    private fun visVarselForKontantstøtte(behandlingId: UUID, stønadsperioder: List<StønadsperiodeDto>): Boolean {
        if (stønadsperioder.isEmpty()) {
            return false
        }
        val barn = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlag.barn
        return harBarnUnder2ÅrIStønadsperiode(barn, stønadsperioder)
    }
}
