package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OppdaterVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {

    fun hentVilkårperioder(behandlingId: UUID): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId)

        return Vilkårperioder(
            målgrupper = finnPerioder<MålgruppeType>(vilkårsperioder),
            aktiviteter = finnPerioder<AktivitetType>(vilkårsperioder),
        )
    }

    private inline fun <reified T : VilkårperiodeType> finnPerioder(
        vilkårsperioder: List<Vilkårperiode>,
    ) = vilkårsperioder.filter { it.type is T }.map(Vilkårperiode::tilDto)

    @Transactional
    fun opprettVilkårperiode(behandlingId: UUID, opprettVilkårperiode: OpprettVilkårperiode): Vilkårperiode {
        feilHvis(behandlingErLåstForVidereRedigering(behandlingId)) {
            "Kan ikke opprette vilkår når behandling er låst for videre redigering"
        }

        val resultatEvaluering = EvalueringVilkårperiode.evaulerVilkårperiode(opprettVilkårperiode.delvilkår)

        return vilkårperiodeRepository.insert(
            Vilkårperiode(
                behandlingId = behandlingId,
                fom = opprettVilkårperiode.fom,
                tom = opprettVilkårperiode.tom,
                type = opprettVilkårperiode.type,
                delvilkår = resultatEvaluering.delvilkår,
                begrunnelse = opprettVilkårperiode.begrunnelse,
                resultat = resultatEvaluering.resultat,
                kilde = KildeVilkårsperiode.MANUELL,
            ),
        )
    }

    fun oppdaterVilkårperiode(id: UUID, oppdaterVilkårperiode: OppdaterVilkårperiode): Vilkårperiode {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(oppdaterVilkårperiode.behandlingId, vilkårperiode.behandlingId)
        feilHvis(behandlingErLåstForVidereRedigering(vilkårperiode.behandlingId)) {
            "Kan ikke oppdatere vilkårperiode når behandling er låst for videre redigering"
        }
        val resultatEvaluering = EvalueringVilkårperiode.evaulerVilkårperiode(oppdaterVilkårperiode.delvilkår)
        val oppdatert = when (vilkårperiode.kilde) {
            KildeVilkårsperiode.MANUELL -> {
                vilkårperiode.copy(
                    begrunnelse = oppdaterVilkårperiode.begrunnelse,
                    fom = oppdaterVilkårperiode.fom,
                    tom = oppdaterVilkårperiode.tom,
                    delvilkår = resultatEvaluering.delvilkår,
                    resultat = resultatEvaluering.resultat,
                )
            }
            KildeVilkårsperiode.SYSTEM -> {
                validerIkkeEndretFomTomForSystem(vilkårperiode, oppdaterVilkårperiode)
                vilkårperiode.copy(
                    begrunnelse = oppdaterVilkårperiode.begrunnelse,
                    delvilkår = resultatEvaluering.delvilkår,
                    resultat = resultatEvaluering.resultat,
                )
            }
        }
        return vilkårperiodeRepository.update(oppdatert)
    }

    private fun validerIkkeEndretFomTomForSystem(
        vilkårperiode: Vilkårperiode,
        oppdaterVilkårperiode: OppdaterVilkårperiode,
    ) {
        feilHvis(vilkårperiode.fom != oppdaterVilkårperiode.fom) {
            "Kan ikke oppdatere fom når kilde=${KildeVilkårsperiode.SYSTEM}"
        }
        feilHvis(vilkårperiode.tom != oppdaterVilkårperiode.tom) {
            "Kan ikke oppdatere tom når kilde=${KildeVilkårsperiode.SYSTEM}"
        }
    }

    fun slettVilkårperiode(id: UUID, slettVikårperiode: SlettVikårperiode): Vilkårperiode {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(slettVikårperiode.behandlingId, vilkårperiode.behandlingId)

        feilHvis(behandlingErLåstForVidereRedigering(vilkårperiode.behandlingId)) {
            "Kan ikke slette vilkårperiode når behandling er låst for videre redigering"
        }

        return vilkårperiodeRepository.update(
            vilkårperiode.copy(
                resultat = ResultatVilkårperiode.SLETTET,
                slettetKommentar = slettVikårperiode.kommentar,
            ),
        )
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
