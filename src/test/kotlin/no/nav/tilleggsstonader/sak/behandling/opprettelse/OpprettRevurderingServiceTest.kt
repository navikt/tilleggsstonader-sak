package no.nav.tilleggsstonader.sak.behandling.opprettelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.GjenbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OpprettRevurderingServiceTest {
    private val opprettBehandlingService = mockk<OpprettBehandlingService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val barnService = mockk<BarnService>(relaxed = true)
    private val unleashService = mockk<UnleashService>()
    private val gjenbrukDataRevurderingService = mockk<GjenbrukDataRevurderingService>(relaxed = true)
    private val personService = mockk<PersonService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>()

    private val service =
        OpprettRevurderingService(
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            barnService = barnService,
            unleashService = unleashService,
            gjenbrukDataRevurderingService = gjenbrukDataRevurderingService,
            personService = personService,
            fagsakService = fagsakService,
        )

    @Test
    fun `skal ikke opprette revurdering når det finnes en kjørelistebehandling på vent`() {
        val fagsakId = FagsakId.random()
        every { unleashService.isEnabled(Toggle.KAN_OPPRETTE_REVURDERING) } returns true
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO, id = fagsakId)
        every { behandlingService.harKjørelisteBehandlingPåVent(fagsakId) } returns true

        assertThatThrownBy {
            service.opprettRevurdering(
                opprettRevurdering(
                    fagsakId = fagsakId,
                ),
            )
        }.hasMessageContaining("Det finnes en kjørelistebehandling på vent")

        verify(exactly = 0) { opprettBehandlingService.opprettBehandling(any()) }
    }

    private fun opprettRevurdering(
        fagsakId: FagsakId,
        årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    ) = OpprettRevurdering(
        fagsakId = fagsakId,
        årsak = årsak,
        valgteBarn = emptySet(),
        kravMottatt = null,
        nyeOpplysningerMetadata = null,
        skalOppretteOppgave = true,
        behandlingMetode = no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode.MANUELL,
        forenkletBehandlingstype = ForenkletBehandlingstype.ORDINAER_BEHANDLING,
    )
}
