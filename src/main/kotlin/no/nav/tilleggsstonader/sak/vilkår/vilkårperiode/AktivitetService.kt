package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktivitetService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettAktivitet(aktivitet: LagreAktivitet): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(aktivitet.behandlingId)

        validerAktivitetsdager(vilkårPeriodeType = aktivitet.type, aktivitetsdager = aktivitet.aktivitetsdager)
        validerKildeId(aktivitet)

        val faktaOgVurdering = mapAktiviteter(aktivitet)

        return vilkårperiodeRepository.insert(
            GeneriskVilkårperiode(
                behandlingId = aktivitet.behandlingId,
                resultat = faktaOgVurdering.utledResultat(),
                status = Vilkårstatus.NY,
                kildeId = aktivitet.kildeId,
                type = aktivitet.type,
                faktaOgVurdering = faktaOgVurdering,
                fom = aktivitet.fom,
                tom = aktivitet.tom,
                begrunnelse = aktivitet.begrunnelse,
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
            brukerfeilHvis(vilkårPeriodeType != AktivitetType.INGEN_AKTIVITET && aktivitetsdager !in 1..5) {
                "Aktivitetsdager må være et heltall mellom 1 og 5"
            }
        } else if (vilkårPeriodeType is MålgruppeType) {
            brukerfeilHvisIkke(aktivitetsdager == null) { "Kan ikke registrere aktivitetsdager på målgrupper" }
        }
    }

    private fun validerKildeId(vilkårperiode: LagreAktivitet) {
        val behandlingId = vilkårperiode.behandlingId
        val kildeId = vilkårperiode.kildeId ?: return

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)
            ?: error("Finner ikke grunnlag til behandling=$behandlingId")
        val idIGrunnlag = grunnlag.grunnlag.aktivitet.aktiviteter.map { it.id }
        feilHvis(kildeId !in idIGrunnlag) {
            "Aktivitet med id=$kildeId finnes ikke i grunnlag"
        }
    }
}

// fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
//    val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)
//
//    val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
//    validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
//    validerBehandling(behandling)
//
//    validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)
//    feilHvis(eksisterendeVilkårperiode.kildeId != vilkårperiode.kildeId) {
//        "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
//    }
//
//    val resultatEvaluering = evaulerVilkårperiode(eksisterendeVilkårperiode.type, vilkårperiode.delvilkår)
//    val oppdatert = eksisterendeVilkårperiode.medVilkårOgVurdering(
//        fom = vilkårperiode.fom,
//        tom = vilkårperiode.tom,
//        begrunnelse = vilkårperiode.begrunnelse,
//        faktaOgVurdering = mapFaktaOgVurderingDto(vilkårperiode, resultatEvaluering),
//        resultat = resultatEvaluering.resultat,
//    )
//
//    validerEndrePeriodeRevurdering(behandling, eksisterendeVilkårperiode, oppdatert)
//    return vilkårperiodeRepository.update(oppdatert)
// }

// private fun behandlingErLåstForVidereRedigering(behandlingId: BehandlingId) =
//    behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
//    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
//        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)
//
//        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
//        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
//        validerBehandling(behandling)
//
//        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)
//        feilHvis(eksisterendeVilkårperiode.kildeId != vilkårperiode.kildeId) {
//            "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
//        }
//
//        val resultatEvaluering = evaulerVilkårperiode(eksisterendeVilkårperiode.type, vilkårperiode.delvilkår)
//        val oppdatert = eksisterendeVilkårperiode.medVilkårOgVurdering(
//            fom = vilkårperiode.fom,
//            tom = vilkårperiode.tom,
//            begrunnelse = vilkårperiode.begrunnelse,
//            faktaOgVurdering = mapFaktaOgVurderingDto(vilkårperiode, resultatEvaluering),
//            resultat = resultatEvaluering.resultat,
//        )
//
//        validerEndrePeriodeRevurdering(behandling, eksisterendeVilkårperiode, oppdatert)
//        return vilkårperiodeRepository.update(oppdatert)
//    }
// }
