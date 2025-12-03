package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningStegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførIngangsvilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførVilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningRevurderingTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `forlengelse av reise der perioden allerede har blitt utbetalt skal validere feil`() {
        val dagensDato = LocalDate.now()

        val reiser =
            lagreDagligReiseDto(
                fom = dagensDato.minusDays(46),
                tom = dagensDato.plusDays(7),
                fakta =
                    FaktaDagligReiseOffentligTransportDto(
                        reiseId = dummyReiseId,
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
                medVilkår = listOf(reiser),
            )

        val førstegangsbehandling = kall.behandling.hent(førstegangsbehandlingId)

        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = dagensDato,
                        nyeOpplysningerMetadata = null,
                    ),
            )
        gjennomførIngangsvilkårSteg(behandlingId = revurderingId)

        kjørTasksKlareForProsessering()
        val vilkårId =
            kall.vilkårDagligReise
                .hentVilkår(revurderingId)
                .first()
                .id

        kall.vilkårDagligReise.oppdaterVilkår(
            lagreVilkår =
                lagreDagligReiseDto(
                    fom = dagensDato.minusDays(46),
                    tom = dagensDato.plusDays(42),
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
                """
                I den revurderte beregningen vil en allerede utbetalt periode med enkeltbilletter bli endret 
                til en periode med månedskort, som kan være til ugunst for søker. For å hindre dette kan du legge 
                inn en ny reise i stedet for å forlenge den eksisterende.
                """.trimIndent(),
            )
    }
}

private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeAktivitet(behandlingId, fom = LocalDate.now().minusDays(46), tom = LocalDate.now().plusDays(42))

private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeMålgruppe(behandlingId, fom = LocalDate.now().minusDays(46), tom = LocalDate.now().plusDays(42))
