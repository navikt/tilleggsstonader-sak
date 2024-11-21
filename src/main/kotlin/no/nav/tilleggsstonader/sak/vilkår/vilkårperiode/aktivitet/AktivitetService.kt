package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
        validerBehandlingStatusOgSteg(behandling)
        validerKildeId(aktivitet)

        // TODO: Flytt validering inn i mapper?
        validerFaktafelter(stønadstype = behandling.stønadstype, aktivitet = aktivitet)

        val faktaOgVurdering = mapAktiviteter(stønadstype = behandling.stønadstype, aktivitet)

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

    fun oppdaterAktivitet(id: UUID, aktivitet: LagreAktivitet): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(aktivitet.behandlingId)
        validerBehandlingStatusOgSteg(behandling)

        val eksisterendeAktivitet = vilkårperiodeRepository.findByIdOrThrow(id)
        validerOppdatering(aktivitet, eksisterendeAktivitet)

        // TODO: Flytt validering inn i mapper?
        validerFaktafelter(stønadstype = behandling.stønadstype, aktivitet = aktivitet)

        val faktaOgVurdering = mapAktiviteter(stønadstype = behandling.stønadstype, aktivitet)

        val oppdatert = eksisterendeAktivitet.medVilkårOgVurdering(
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            begrunnelse = aktivitet.begrunnelse,
            faktaOgVurdering = faktaOgVurdering,
        )

        validerEndrePeriodeRevurdering(behandling, eksisterendeAktivitet, oppdatert)
        return vilkårperiodeRepository.update(oppdatert)
    }

    private fun validerOppdatering(aktivitet: LagreAktivitet, eksisterendeAktivitet: Vilkårperiode) {
        validerBehandlingIdErLik(aktivitet.behandlingId, eksisterendeAktivitet.behandlingId)

        feilHvis(eksisterendeAktivitet.kildeId != aktivitet.kildeId) {
            "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
        }
    }

    private fun validerBehandlingStatusOgSteg(behandling: Saksbehandling) {
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette eller endre aktivitet når behandling er låst for videre redigering"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre aktivitet når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }

    private fun validerFaktafelter(stønadstype: Stønadstype, aktivitet: LagreAktivitet) {
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> {
                require(aktivitet.faktaOgVurderinger is FaktaOgVurderingerAktivitetBarnetilsynDto)
                validerAktivitetsdager(aktivitet.type, aktivitet.faktaOgVurderinger.aktivitetsdager)
            }
            Stønadstype.LÆREMIDLER -> {
                require(aktivitet.faktaOgVurderinger is FaktaOgVurderingerAktivitetLæremidlerDto)
                // TODO: Validering av fakta læremidler
            }
        }
    }

    private fun validerAktivitetsdager(aktivitetType: AktivitetType, aktivitetsdager: Int?) {
        brukerfeilHvis(aktivitetType != AktivitetType.INGEN_AKTIVITET && aktivitetsdager !in 1..5) {
            "Aktivitetsdager må være et heltall mellom 1 og 5"
        }
    }

    private fun validerKildeId(aktivitet: LagreAktivitet) {
        val behandlingId = aktivitet.behandlingId
        val kildeId = aktivitet.kildeId ?: return

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)
            ?: error("Finner ikke grunnlag til behandling=$behandlingId")
        val idIGrunnlag = grunnlag.grunnlag.aktivitet.aktiviteter.map { it.id }
        feilHvis(kildeId !in idIGrunnlag) {
            "Aktivitet med id=$kildeId finnes ikke i grunnlag"
        }
    }
}
