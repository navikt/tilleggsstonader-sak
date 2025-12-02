package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningStegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførRevurderingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførVilkårSteg
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OffentligTransportBeregningRevurderingTest(
    @Autowired private val vedtakRepository: VedtakRepository,
) : CleanDatabaseIntegrationTest() {
    @Test
    fun forlengelseAvReiserSkalKasteFeil() {
        val reiser =
            lagreDagligReiseDto(
                fom = 1 januar 2025,
                tom = 16 februar 2025,
                fakta =
                    FaktaDagligReiseOffentligTransportDto(
                        reisedagerPerUke = 2,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 800,
                    ),
            )

        val førstegangsbehandlingId =
            gjennomførBehandlingsløp(
                medAktivitet = ::lagreAktivitet,
                medMålgruppe = ::lagreMålgruppe,
                medVilkår =
                    listOf(
                        reiser,
                    ),
            )

        val førstegangsbehandling = kall.behandling.hent(førstegangsbehandlingId)

        val revurderingId =
            gjennomførRevurderingsløp(
                tilSteg = StegType.VILKÅR,
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 17 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )

        val vilkårId =
            kall.vilkårDagligReise
                .hentVilkår(revurderingId)
                .first()
                .id

        kall.vilkårDagligReise.oppdaterVilkår(
            lagreVilkår =
                lagreDagligReiseDto(
                    fom = 1 januar 2025,
                    tom = 30 mars 2025,
                ),
            vilkårId = vilkårId,
            behandlingId = revurderingId,
        )

        gjennomførVilkårSteg(
            medVilkår = emptyList(),
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )

        gjennomførBeregningStegKall(
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ).expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath(
                "$.detail",
            ).isEqualTo(
                "Kan ikke endre fra enkeltbilletter til månedskort i en periode som allerede er aktiv. " +
                    "Legg inn månedskortet som en egen reise.",
            )
    }
}

private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeAktivitet(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)

private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeMålgruppe(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)
