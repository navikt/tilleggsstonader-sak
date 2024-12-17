package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeValidering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapFaktaOgSvarDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
    private val vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService,
) {
    fun hentVilkårperioder(behandlingId: BehandlingId): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId).sorted()

        return Vilkårperioder(
            målgrupper = vilkårsperioder.ofType<MålgruppeFaktaOgVurdering>(),
            aktiviteter = vilkårsperioder.ofType<AktivitetFaktaOgVurdering>(),
        )
    }

    fun hentVilkårperioderResponse(behandlingId: BehandlingId): VilkårperioderResponse {
        val grunnlagsdataVilkårsperioder = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandlingId)

        return VilkårperioderResponse(
            vilkårperioder = hentVilkårperioder(behandlingId).tilDto(),
            grunnlag = grunnlagsdataVilkårsperioder?.tilDto(),
        )
    }

    fun validerOgLagResponse(behandlingId: BehandlingId, periode: Vilkårperiode? = null): LagreVilkårperiodeResponse {
        val valideringsresultat = validerStønadsperioder(behandlingId)

        return LagreVilkårperiodeResponse(
            periode = periode?.tilDto(),
            stønadsperiodeStatus = if (valideringsresultat.isSuccess) Stønadsperiodestatus.OK else Stønadsperiodestatus.FEIL,
            stønadsperiodeFeil = valideringsresultat.exceptionOrNull()?.message,
        )
    }

    @Transactional
    fun opprettVilkårperiode(vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerNyPeriodeRevurdering(behandling, vilkårperiode.fom)

        if (vilkårperiode.type is MålgruppeType) {
            validerKanLeggeTilMålgruppeManuelt(behandling.stønadstype, vilkårperiode.type)
        }

        validerKildeIdFinnesIGrunnlaget(
            behandlingId = vilkårperiode.behandlingId,
            type = vilkårperiode.type,
            kildeId = vilkårperiode.kildeId,
        )

        val faktaOgVurdering =
            mapFaktaOgSvarDto(stønadstype = behandling.stønadstype, vilkårperiode = vilkårperiode)
        return vilkårperiodeRepository.insert(
            GeneriskVilkårperiode(
                behandlingId = vilkårperiode.behandlingId,
                resultat = faktaOgVurdering.utledResultat(),
                status = Vilkårstatus.NY,
                kildeId = vilkårperiode.kildeId,
                type = vilkårperiode.type,
                faktaOgVurdering = faktaOgVurdering,
                fom = vilkårperiode.fom,
                tom = vilkårperiode.tom,
                begrunnelse = vilkårperiode.begrunnelse,
            ),
        )
    }

    @Transactional
    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        validerBehandling(behandling)

        feilHvis(eksisterendeVilkårperiode.kildeId != vilkårperiode.kildeId) {
            "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
        }

        val oppdatert = eksisterendeVilkårperiode.medVilkårOgVurdering(
            fom = vilkårperiode.fom,
            tom = vilkårperiode.tom,
            begrunnelse = vilkårperiode.begrunnelse,
            faktaOgVurdering = mapFaktaOgSvarDto(
                stønadstype = behandling.stønadstype,
                vilkårperiode = vilkårperiode,
            ),
        )

        validerEndrePeriodeRevurdering(behandling, eksisterendeVilkårperiode, oppdatert)
        return vilkårperiodeRepository.update(oppdatert)
    }

    fun slettVilkårperiode(id: UUID, slettVikårperiode: SlettVikårperiode): Vilkårperiode? {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(slettVikårperiode.behandlingId, vilkårperiode.behandlingId)

        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerSlettPeriodeRevurdering(behandling, vilkårperiode)

        if (vilkårperiode.kanSlettesPermanent()) {
            vilkårperiodeRepository.deleteById(vilkårperiode.id)
            return null
        } else {
            return vilkårperiodeRepository.update(vilkårperiode.markerSlettet(slettVikårperiode.kommentar))
        }
    }

    fun gjenbrukVilkårperioder(forrigeBehandlingId: BehandlingId, nyBehandlingId: BehandlingId) {
        val eksisterendeVilkårperioder =
            vilkårperiodeRepository.findByBehandlingIdAndResultatNot(forrigeBehandlingId, ResultatVilkårperiode.SLETTET)

        val kopiertePerioderMedReferanse = eksisterendeVilkårperioder.map { it.kopierTilBehandling(nyBehandlingId) }
        vilkårperiodeRepository.insertAll(kopiertePerioderMedReferanse)
    }

    private fun validerKildeIdFinnesIGrunnlaget(behandlingId: BehandlingId, type: VilkårperiodeType, kildeId: String?) {
        val kildeId = kildeId ?: return

        feilHvis(type is MålgruppeType) {
            "Kan ikke sende inn kildeId på målgruppe, då målgruppeperioder ikke direkt har en id som aktivitet"
        }

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)
            ?: error("Finner ikke grunnlag til behandling=$behandlingId")
        val idIGrunnlag = grunnlag.grunnlag.aktivitet.aktiviteter.map { it.id }
        feilHvis(kildeId !in idIGrunnlag) {
            "Aktivitet med id=$kildeId finnes ikke i grunnlag"
        }
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette eller endre periode når behandling er låst for videre redigering"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre periode når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }

    private fun validerStønadsperioder(behandlingId: BehandlingId): Result<Unit> {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        val vilkårperioder = hentVilkårperioder(behandlingId)

        return kotlin.runCatching {
            StønadsperiodeValidering.validerStønadsperioderVedEndringAvVilkårperiode(
                stønadsperioder,
                vilkårperioder,
            )
        }
    }
}
