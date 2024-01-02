package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetReelArbeidssøkerRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetTiltakRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetUtdanningRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeAAPFerdigAvklartRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeAAPRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeDagpengerRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeOmstillingsstønadRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeOvergangsstønadRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeUføretrygdRegel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårRepository: VilkårRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {
    fun hentVilkårperioder(behandlingId: UUID): Vilkårperioder {
        val vilkår = vilkårRepository.findByBehandlingId(behandlingId)
        val vilkårsperioder =
            vilkårperiodeRepository.finnVilkårperioderForBehandling(behandlingId).associateBy { it.vilkårId }

        return Vilkårperioder(
            målgrupper = finnPerioder(vilkår, vilkårsperioder, VilkårType::gjelderMålgruppe),
            aktiviteter = finnPerioder(vilkår, vilkårsperioder, VilkårType::gjelderAktivitet),
        )
    }

    @Transactional
    fun opprettVilkårperiode(behandlingId: UUID, opprettVilkårperiode: OpprettVilkårperiode): VilkårperiodeDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette vilkår når behandling er låst for videre redigering"
        }

        val vilkår = OppdaterVilkår.lagNyttVilkår(
            vilkårsregel = vilkårsregelForVilkårsperiodeType(opprettVilkårperiode.type),
            metadata = HovedregelMetadata(emptyList(), behandling),
            behandlingId = behandlingId,
        )
        vilkårRepository.insert(vilkår)

        val vilkårperiode = vilkårperiodeRepository.insert(
            Vilkårperiode(
                vilkårId = vilkår.id,
                fom = opprettVilkårperiode.fom,
                tom = opprettVilkårperiode.tom,
                type = opprettVilkårperiode.type,
            ),
        )

        return vilkårperiode.tilDto(vilkår.tilDto())
    }

    private fun vilkårsregelForVilkårsperiodeType(vilkårperiodeType: VilkårperiodeType) =
        when (vilkårperiodeType) {
            MålgruppeType.AAP -> MålgruppeAAPRegel()
            MålgruppeType.AAP_FERDIG_AVKLART -> MålgruppeAAPFerdigAvklartRegel()
            MålgruppeType.DAGPENGER -> MålgruppeDagpengerRegel()
            MålgruppeType.OMSTILLINGSSTØNAD -> MålgruppeOmstillingsstønadRegel()
            MålgruppeType.OVERGANGSSTØNAD -> MålgruppeOvergangsstønadRegel()
            MålgruppeType.UFØRETRYGD -> MålgruppeUføretrygdRegel()

            AktivitetType.TILTAK -> AktivitetTiltakRegel()
            AktivitetType.UTDANNING -> AktivitetUtdanningRegel()
            AktivitetType.REEL_ARBEIDSSØKER -> AktivitetReelArbeidssøkerRegel()
        }

    private fun finnPerioder(
        vilkår: List<Vilkår>,
        vilkårsperioder: Map<UUID, Vilkårperiode>,
        gjelderFilter: (VilkårType) -> Boolean,
    ) = vilkår.filter { gjelderFilter(it.type) }.map { vilkårsperioder.getValue(it.id).tilDto(it.tilDto()) }
}
