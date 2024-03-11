package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkårsreglerForStønad
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("!prod")
class TestSaksbehandlingService(
    private val vilkårService: VilkårService,
    private val behandlingService: BehandlingService,
) {
    fun utfyllVilkår(behandlingId: UUID): UUID {
        val vilkårsett = vilkårService.hentVilkårsett(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val regler = vilkårsreglerForStønad(saksbehandling.stønadstype).associateBy { it.vilkårType }

        vilkårsett.forEach { vilkår ->
            val delvilkårsett = lagDelvilkårsett(regler.getValue(vilkår.vilkårType), vilkår)
            vilkårService.oppdaterVilkår(SvarPåVilkårDto(vilkår.id, behandlingId, delvilkårsett))
        }
        return behandlingId
    }

    private fun lagDelvilkårsett(
        vilkårsregel: Vilkårsregel,
        vilkår: VilkårDto,
    ): List<DelvilkårDto> {
        return vilkår.delvilkårsett.map { delvilkår ->
            val hovedregel = delvilkår.hovedregel()
            val regelSteg = vilkårsregel.regler.getValue(hovedregel)
            regelSteg.svarMapping.mapNotNull { (svarId, svarRegel) ->
                lagOppfyltDelvilkår(delvilkår, svarRegel, svarId)
            }.firstOrNull()
                ?: error("Finner ikke oppfylt svar for vilkårstype=${vilkår.vilkårType} hovedregel=$hovedregel")
        }
    }

    private fun lagOppfyltDelvilkår(
        delvilkår: DelvilkårDto,
        svarRegel: SvarRegel,
        svarId: SvarId,
    ) = when (svarRegel) {
        SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
        SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
        SluttSvarRegel.OPPFYLT,
        -> delvilkår(
            delvilkår.hovedregel(),
            svarId,
            if (svarRegel == SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE) "begrunnelse" else null,
        )

        else -> null
    }

    private fun delvilkår(regelId: RegelId, svar: SvarId, begrunnelse: String? = null) = DelvilkårDto(
        Vilkårsresultat.OPPFYLT,
        listOf(VurderingDto(regelId, svar, begrunnelse)),
    )
}
