package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.vilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.VilkårStegService
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkårsreglerForStønad
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestSaksbehandlingService(
    private val vilkårService: VilkårService,
    private val behandlingService: BehandlingService,
    private val vilkårStegService: VilkårStegService,
) {
    fun utfyllVilkår(behandlingId: UUID): UUID {
        val vilkårsett = vilkårService.hentVilkårsett(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val regler = vilkårsreglerForStønad(saksbehandling.stønadstype).associateBy { it.vilkårType }

        vilkårsett.forEach { vilkår ->
            val delvilkårsett = lagDelvilkårsett(regler.getValue(vilkår.vilkårType), vilkår)
            vilkårStegService.oppdaterVilkår(SvarPåVilkårDto(vilkår.id, behandlingId, delvilkårsett))
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
