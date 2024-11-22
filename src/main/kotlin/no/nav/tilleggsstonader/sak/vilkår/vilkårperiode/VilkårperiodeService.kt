package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeValideringUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapFaktaOgVurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
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
            vilkårperioder = hentVilkårperioderDto(behandlingId),
            grunnlag = grunnlagsdataVilkårsperioder?.tilDto(),
        )
    }

    fun hentVilkårperioderDto(behandlingId: BehandlingId): VilkårperioderDto {
        return hentVilkårperioder(behandlingId).tilDto()
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
        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)
        validerKildeId(vilkårperiode)

        val faktaOgVurdering = mapFaktaOgVurderingDto(vilkårperiode)
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

    private fun validerKildeId(vilkårperiode: LagreVilkårperiode) {
        val behandlingId = vilkårperiode.behandlingId
        val kildeId = vilkårperiode.kildeId ?: return
        feilHvis(vilkårperiode.type is MålgruppeType) {
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
            "Kan ikke opprette eller endre vilkårperiode når behandling er låst for videre redigering"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre vilkårperiode når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }

    private fun validerAktivitetsdager(vilkårPeriodeType: VilkårperiodeType, aktivitetsdager: Int?) {
        if (vilkårPeriodeType is AktivitetType) {
            brukerfeilHvis(vilkårPeriodeType != AktivitetType.INGEN_AKTIVITET && aktivitetsdager !in 1..5) {
                "Aktivitetsdager må være et heltall mellom 1 og 5"
            }
        } else if (vilkårPeriodeType is MålgruppeType) {
            brukerfeilHvisIkke(aktivitetsdager == null) { "Kan ikke registrere aktivitetsdager på målgrupper" }
        }
    }

    private fun validerStønadsperioder(behandlingId: BehandlingId): Result<Unit> {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        val vilkårperioder = hentVilkårperioder(behandlingId)

        return kotlin.runCatching {
            StønadsperiodeValideringUtil.validerStønadsperioderVedEndringAvVilkårperiode(
                stønadsperioder,
                vilkårperioder.tilDto(),
            )
        }
    }

    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        validerBehandling(behandling)

        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)
        feilHvis(eksisterendeVilkårperiode.kildeId != vilkårperiode.kildeId) {
            "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
        }

        val oppdatert = eksisterendeVilkårperiode.medVilkårOgVurdering(
            fom = vilkårperiode.fom,
            tom = vilkårperiode.tom,
            begrunnelse = vilkårperiode.begrunnelse,
            faktaOgVurdering = mapFaktaOgVurderingDto(vilkårperiode),
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
}
