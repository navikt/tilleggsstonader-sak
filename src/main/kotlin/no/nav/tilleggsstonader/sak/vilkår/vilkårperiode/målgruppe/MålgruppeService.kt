package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.målgruppe

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MålgruppeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettMålgruppe(målgruppe: LagreMålgruppe): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(målgruppe.behandlingId)
        validerBehandlingStatusOgSteg(behandling)

        val faktaOgVurdering = mapMålgruppe(målgruppe)

        return vilkårperiodeRepository.insert(
            GeneriskVilkårperiode(
                behandlingId = målgruppe.behandlingId,
                resultat = faktaOgVurdering.utledResultat(),
                status = Vilkårstatus.NY,
                kildeId = målgruppe.kildeId,
                type = målgruppe.type,
                faktaOgVurdering = faktaOgVurdering,
                fom = målgruppe.fom,
                tom = målgruppe.tom,
                begrunnelse = målgruppe.begrunnelse,
            ),
        )
    }

    fun oppdaterMålgruppe(id: UUID, målgruppe: LagreMålgruppe): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(målgruppe.behandlingId)
        validerBehandlingStatusOgSteg(behandling)

        val eksisterendeMålgruppe = vilkårperiodeRepository.findByIdOrThrow(id)
        validerOppdatering(målgruppe, eksisterendeMålgruppe)

        val faktaOgVurdering = mapMålgruppe(målgruppe)

        val oppdatert = eksisterendeMålgruppe.medVilkårOgVurdering(
            fom = målgruppe.fom,
            tom = målgruppe.tom,
            begrunnelse = målgruppe.begrunnelse,
            faktaOgVurdering = faktaOgVurdering,
        )

        validerEndrePeriodeRevurdering(behandling, eksisterendeMålgruppe, oppdatert)
        return vilkårperiodeRepository.update(oppdatert)
    }

    private fun validerOppdatering(aktivitet: LagreMålgruppe, eksisterendeAktivitet: Vilkårperiode) {
        validerBehandlingIdErLik(aktivitet.behandlingId, eksisterendeAktivitet.behandlingId)

        feilHvis(eksisterendeAktivitet.kildeId != aktivitet.kildeId) {
            "Kan ikke oppdatere kildeId på en allerede eksisterende vilkårperiode"
        }
    }

    private fun validerBehandlingStatusOgSteg(behandling: Saksbehandling) {
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette eller endre målgruppe når behandling er låst for videre redigering"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre målgruppe når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }
}
