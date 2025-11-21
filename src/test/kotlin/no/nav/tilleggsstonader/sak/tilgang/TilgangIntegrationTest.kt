package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tilgangstester mot endepunkt
 */
class TilgangIntegrationTest : IntegrationTest() {
    lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(status = BehandlingStatus.UTREDES),
                stønadstype = Stønadstype.BARNETILSYN,
            )
    }

    @Test
    fun `kan lese behandling med rolle veileder, saksbehandler og beslutter`() {
        listOf(rolleConfig.beslutterRolle, rolleConfig.saksbehandlerRolle, rolleConfig.veilederRolle).forEach {
            medBrukercontext(roller = listOf(it)) {
                kall.behandling.hent(behandling.id)
            }
        }
    }

    @Test
    fun `kan ikke lese behandling uten rolle veileder, saksbehandler eller beslutter`() {
        medBrukercontext(roller = listOf("dummy")) {
            kall.behandling.apiRespons
                .hent(behandling.id)
                .expectStatus()
                .isForbidden
        }
    }

    @Test
    fun `kan opprette vilkårsperioder på behandling med rolle saksbehandler og beslutter når tilordnet oppgave`() {
        opprettOgTilordneOppgaveForBehandling(behandling.id)
        listOf(rolleConfig.beslutterRolle, rolleConfig.saksbehandlerRolle).forEach {
            medBrukercontext(roller = listOf(it)) {
                kall.vilkårperiode.opprett(
                    lagreVilkårperiodeAktivitet(behandlingId = behandling.id, faktaOgSvar = VilkårperiodeTestUtil.faktaOgSvarTilsynBarnDto),
                )
            }
        }
    }

    @Test
    fun `kan ikke opprette vilkårsperioder på behandling med rolle veileder`() {
        medBrukercontext(roller = listOf(rolleConfig.veilederRolle)) {
            kall.vilkårperiode.apiRespons
                .opprett(
                    lagreVilkårperiodeAktivitet(
                        behandlingId = behandling.id,
                        faktaOgSvar = VilkårperiodeTestUtil.faktaOgSvarTilsynBarnDto,
                    ),
                ).expectStatus()
                .isForbidden
        }
    }

    @Test
    fun `kan ikke opprette vilkårsperioder på behandling uten noen rolle`() {
        medBrukercontext(roller = listOf("dummy")) {
            kall.vilkårperiode.apiRespons
                .opprett(
                    lagreVilkårperiodeAktivitet(behandlingId = behandling.id, faktaOgSvar = VilkårperiodeTestUtil.faktaOgSvarTilsynBarnDto),
                ).expectStatus()
                .isForbidden
        }
    }
}
