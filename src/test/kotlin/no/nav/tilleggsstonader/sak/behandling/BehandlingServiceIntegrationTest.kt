package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class BehandlingServiceIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingService: BehandlingService
    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    @Test
    internal fun `opprettBehandling skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES,
            ),
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering om det ikke finnes en behandling fra før`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    internal fun `hentBehandlinger - skal kaste feil hvis behandling ikke finnes`() {
        assertThatThrownBy { behandlingService.hentBehandlinger(setOf(UUID.randomUUID())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Finner ikke Behandling for")
    }

    @Test
    internal fun `hentBehandlinger - skal returnere behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = testoppsettService.lagre(behandling(fagsak))

        assertThat(behandlingService.hentBehandlinger(setOf(behandling.id, behandling2.id))).hasSize(2)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis kun avslått`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val avslag = testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(avslag.id)
    }

    @Test
    internal fun `skal plukke ut førstegangsbehandling hvis det finnes førstegangsbehandling, avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val førstegang = testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
        )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(førstegang.id)
    }

    /* TODO fix senere har ikke flere stønadstyper
    @Test
    internal fun `hentBehandlingForGjenbrukAvVilkår - skal returnere en sortert liste av aktuelle behandlinger for vilkårsgjenbruk`() {
        val fagsakPersonId = UUID.randomUUID()
        val fagsakOs = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.OVERGANGSSTØNAD,
                fagsakPersonId = fagsakPersonId,
            ),
        )
        val fagsakBt = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.BARNETILSYN,
                fagsakPersonId = fagsakPersonId,
            ),
        )
        val fagsakSp = testoppsettService.lagreFagsak(
            fagsak(
                stønadstype = StønadType.SKOLEPENGER,
                fagsakPersonId = fagsakPersonId,
            ),
        )

        testoppsettService.lagre(
            behandling(fagsakOs, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val førstegangBt = testoppsettService.lagre(
            behandling(fagsakBt, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
        )
        val førstegangSp = testoppsettService.lagre(
            behandling(fagsakSp, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
        )
        val revurderingUnderArbeidSP = testoppsettService.lagre(
            behandling(fagsakSp, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES),
        )

        val behandlingerForVilkårsgjenbrukHentet = behandlingService.hentBehandlingerForGjenbrukAvVilkår(fagsakPersonId)
        val behandlingerForVilkårsgjenbrukkLagret = listOf(revurderingUnderArbeidSP, førstegangSp, førstegangBt)
        assertThat(behandlingerForVilkårsgjenbrukHentet).isEqualTo(behandlingerForVilkårsgjenbrukkLagret)
    }
     */

    @Nested
    inner class BehandlingPåVent {
        @Test
        internal fun `opprettBehandling av førstegangsbehandling er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Kan ikke opprette ny behandling når det finnes en førstegangsbehandling på vent")
        }

        @Test
        internal fun `opprettBehandling av revurdering er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    BehandlingType.REVURDERING,
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Kan ikke opprette ny behandling når det finnes en førstegangsbehandling på vent")
        }

        @Test
        internal fun `opprettBehandling er mulig hvis det finnes en revurdering på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.FERDIGSTILT))
            testoppsettService.lagre(
                behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING),
            )
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }
    }
}
