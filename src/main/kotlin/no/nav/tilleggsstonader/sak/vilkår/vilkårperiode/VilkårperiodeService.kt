package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeValideringUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.evaulerVilkårperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) {

    fun hentVilkårperioder(behandlingId: UUID): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId)

        return Vilkårperioder(
            målgrupper = finnPerioder<MålgruppeType>(vilkårsperioder),
            aktiviteter = finnPerioder<AktivitetType>(vilkårsperioder),
        )
    }

    fun hentVilkårperioderDto(behandlingId: UUID): VilkårperioderDto {
        return hentVilkårperioder(behandlingId).tilDto()
    }

    private inline fun <reified T : VilkårperiodeType> finnPerioder(
        vilkårsperioder: List<Vilkårperiode>,
    ) = vilkårsperioder.filter { it.type is T }

    fun validerOgLagResponse(periode: Vilkårperiode): LagreVilkårperiodeResponse {
        val valideringsresultat = validerStønadsperioder(periode.behandlingId)

        return LagreVilkårperiodeResponse(
            periode.tilDto(),
            stønadsperiodeStatus = if (valideringsresultat.isSuccess) Stønadsperiodestatus.OK else Stønadsperiodestatus.FEIL,
            stønadsperiodeFeil = valideringsresultat.exceptionOrNull()?.message,
        )
    }

    @Transactional
    fun opprettVilkårperiode(vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)

        if (vilkårperiode.type is MålgruppeType) {
            validerKanLeggeTilMålgruppeManuelt(behandling.stønadstype, vilkårperiode.type)
        }
        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)

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
                aktivitetsdager = vilkårperiode.aktivitetsdager,
                kilde = KildeVilkårsperiode.MANUELL,
            ),
        )
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
            brukerfeilHvisIkke(aktivitetsdager in 1..5) {
                "Aktivitetsdager må være et heltall mellom 1 og 5"
            }
        } else if (vilkårPeriodeType is MålgruppeType) {
            brukerfeilHvisIkke(aktivitetsdager == null) { "Kan ikke registrere aktivitetsdager på målgrupper" }
        }
    }

    private fun validerStønadsperioder(behandlingId: UUID): Result<Unit> {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        val vilkårperioder = hentVilkårperioder(behandlingId)

        return kotlin.runCatching {
            StønadsperiodeValideringUtil.validerStønadsperioder(stønadsperioder, vilkårperioder.tilDto())
        }
    }

    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
        validerBehandling(behandling)

        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)

        val resultatEvaluering = evaulerVilkårperiode(eksisterendeVilkårperiode.type, vilkårperiode.delvilkår)
        val oppdatert = when (eksisterendeVilkårperiode.kilde) {
            KildeVilkårsperiode.MANUELL -> {
                eksisterendeVilkårperiode.copy(
                    begrunnelse = vilkårperiode.begrunnelse,
                    fom = vilkårperiode.fom,
                    tom = vilkårperiode.tom,
                    delvilkår = resultatEvaluering.delvilkår,
                    aktivitetsdager = vilkårperiode.aktivitetsdager,
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

        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)

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
