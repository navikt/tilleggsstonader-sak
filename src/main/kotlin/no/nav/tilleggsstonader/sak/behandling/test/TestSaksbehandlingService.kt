package no.nav.tilleggsstonader.sak.behandling.test

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
@Profile("!prod")
class TestSaksbehandlingService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårService: VilkårService,
    private val behandlingService: BehandlingService,
) {
    @Transactional
    fun utfyllInngangsvilkår(behandlingId: UUID): UUID {
        opprettVilkårperioder(behandlingId)
        opprettStønadsperiode(behandlingId)
        return behandlingId
    }

    private fun opprettVilkårperioder(behandlingId: UUID) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        brukerfeilHvis(vilkårperioder.aktiviteter.isNotEmpty() || vilkårperioder.målgrupper.isNotEmpty()) {
            "Har allerede vilkårperioder"
        }
        vilkårperiodeService.opprettVilkårperiode(opprettMålgruppe(behandlingId))
        vilkårperiodeService.opprettVilkårperiode(opprettAktivitet(behandlingId))
    }

    fun utfyllVilkår(behandlingId: UUID): UUID {
        utfyllStønadsvilkår(behandlingId)
        return behandlingId
    }

    private fun opprettMålgruppe(behandlingId: UUID) = LagreVilkårperiode(
        behandlingId = behandlingId,
        type = MålgruppeType.AAP,
        fom = YearMonth.now().atDay(1),
        tom = YearMonth.now().atEndOfMonth(),
        aktivitetsdager = null,
        delvilkår = DelvilkårMålgruppeDto(medlemskap = null),
        begrunnelse = "Begrunnelse målgruppe",
    )

    private fun opprettAktivitet(behandlingId: UUID) = LagreVilkårperiode(
        behandlingId = behandlingId,
        type = AktivitetType.TILTAK,
        fom = YearMonth.now().atDay(1),
        tom = YearMonth.now().atEndOfMonth(),
        aktivitetsdager = 5,
        delvilkår = DelvilkårAktivitetDto(
            lønnet = no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto(
                SvarJaNei.NEI,
                "Begrunnelse lønnet",
            ),
            mottarSykepenger = no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto(
                SvarJaNei.NEI,
                "Begrunnelse sykepenger",
            ),
        ),
        begrunnelse = "Begrunnelse aktivitet",
    )

    private fun opprettStønadsperiode(behandlingId: UUID) {
        brukerfeilHvis(stønadsperiodeService.hentStønadsperioder(behandlingId).isNotEmpty()) {
            "Har allerede stønadsperioder"
        }
        stønadsperiodeService.lagreStønadsperioder(
            behandlingId,
            listOf(
                StønadsperiodeDto(
                    fom = YearMonth.now().atDay(1),
                    tom = YearMonth.now().atEndOfMonth(),
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                ),
            ),
        )
    }

    private fun utfyllStønadsvilkår(behandlingId: UUID) {
        val vilkårsett = vilkårService.hentVilkårsett(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val regler = vilkårsreglerForStønad(saksbehandling.stønadstype).associateBy { it.vilkårType }

        vilkårsett.forEach { vilkår ->
            val delvilkårsett = lagDelvilkårsett(regler.getValue(vilkår.vilkårType), vilkår)
            vilkårService.oppdaterVilkår(SvarPåVilkårDto(vilkår.id, behandlingId, delvilkårsett))
        }
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
