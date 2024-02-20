package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.evaulerVilkårperiode
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
    fun opprettVilkårperiode(vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette vilkår når behandling er låst for videre redigering"
        }

        if (vilkårperiode.type is MålgruppeType) {
            validerKanLeggeTilMålgruppeManuelt(behandling.stønadstype, vilkårperiode.type)
        }

        val resultatEvaluering = evaulerVilkårperiode(vilkårperiode.type, vilkårperiode.delvilkår)

        return vilkårperiodeRepository.insert(
            Vilkårperiode(
                behandlingId = vilkårperiode.behandlingId,
                fom = vilkårperiode.fom,
                tom = vilkårperiode.tom,
                type = vilkårperiode.type,
                delvilkår = resultatEvaluering.delvilkår,
                begrunnelse = vilkårperiode.begrunnelse,
                resultat = resultatEvaluering.resultat,
                kilde = KildeVilkårsperiode.MANUELL,
            ),
        )
    }

    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        feilHvis(behandlingErLåstForVidereRedigering(eksisterendeVilkårperiode.behandlingId)) {
            "Kan ikke oppdatere vilkårperiode når behandling er låst for videre redigering"
        }
        val resultatEvaluering = evaulerVilkårperiode(eksisterendeVilkårperiode.type, vilkårperiode.delvilkår)
        val oppdatert = when (eksisterendeVilkårperiode.kilde) {
            KildeVilkårsperiode.MANUELL -> {
                eksisterendeVilkårperiode.copy(
                    begrunnelse = vilkårperiode.begrunnelse,
                    fom = vilkårperiode.fom,
                    tom = vilkårperiode.tom,
                    delvilkår = resultatEvaluering.delvilkår,
                    resultat = resultatEvaluering.resultat,
                )
            }
            KildeVilkårsperiode.SYSTEM -> {
                validerIkkeEndretFomTomForSystem(eksisterendeVilkårperiode, vilkårperiode)
                eksisterendeVilkårperiode.copy(
                    begrunnelse = vilkårperiode.begrunnelse,
                    delvilkår = resultatEvaluering.delvilkår,
                    resultat = resultatEvaluering.resultat,
                )
            }
        }
        return vilkårperiodeRepository.update(oppdatert)
    }

    private fun validerIkkeEndretFomTomForSystem(
        vilkårperiode: Vilkårperiode,
        oppdaterVilkårperiode: LagreVilkårperiode,
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

    fun finnVilkårperioderAktivitet(behandlingId: UUID, type: AktivitetType): List<Vilkårperiode>{
        return vilkårperiodeRepository.findByBehandlingIdAndType(behandlingId, type)
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
