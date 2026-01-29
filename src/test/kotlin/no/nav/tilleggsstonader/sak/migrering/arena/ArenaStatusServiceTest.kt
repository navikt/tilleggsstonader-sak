package no.nav.tilleggsstonader.sak.migrering.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.henlagtBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ArenaStatusServiceTest {
    private val personService = mockk<PersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()

    val arenaStatusService =
        ArenaStatusService(
            personService = personService,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
        )

    val ident = "ident"
    val fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO, identer = setOf(PersonIdent(ident)))

    val request = ArenaFinnesPersonRequest(ident, Rettighet.DAGLIG_REISE.kodeArena)

    @BeforeEach
    fun setUp() {
        mockFinnFagsak(fagsak)
        every { personService.hentFolkeregisterIdenter(ident) } returns
            PdlIdenter(
                listOf(
                    PdlIdent(
                        ident,
                        false,
                        "FOLKEREGISTERIDENT",
                    ),
                ),
            )
    }

    @Test
    fun `skal returnere false når det ikke finnes noen fagsak`() {
        mockFinnFagsak(null)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 0) { behandlingService.hentBehandlinger(fagsakId = any()) }
    }

    @Test
    fun `skal returnere false når det ikke finnes noen behandlinger`() {
        mockBehandling(null, fagsak)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsakId = any()) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingResultat::class, names = ["HENLAGT"], mode = EnumSource.Mode.EXCLUDE)
    fun `skal returnere true hvis det finnes behandlinger`(resultat: BehandlingResultat) {
        mockBehandling(resultat, fagsak)

        assertThat(arenaStatusService.finnStatus(request).finnes).isTrue()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsakId = any()) }
    }

    @Test
    fun `skal returnere false hvis det kun finnes en henlagt behandling`() {
        mockBehandling(BehandlingResultat.HENLAGT, fagsak)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsakId = any()) }
    }

    @Test
    fun `skal returnere false hvis behandlingen har unntak og kan behandles i arena`() {
        val fagsakId = FagsakId.random()
        val fagsakMedUnntak = fagsak(id = fagsakId, eksternId = EksternFagsakId(fagsakId = fagsakId, id = 9807))

        mockBehandling(BehandlingResultat.INNVILGET, fagsakMedUnntak)
        mockFinnFagsak(fagsakMedUnntak)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsakId = any()) }
    }

    private fun mockFinnFagsak(fagsak: Fagsak?) {
        every { fagsakService.finnFagsak(eq(setOf(ident)), request.stønadstype) } returns fagsak
    }

    /**
     * Returnerer ingen behandling dersom resultat settes til null
     */
    private fun mockBehandling(
        resultat: BehandlingResultat?,
        fagsak: Fagsak,
    ) {
        val behandlinger =
            when (resultat) {
                BehandlingResultat.HENLAGT -> listOf(henlagtBehandling(fagsak))
                null -> emptyList()
                else -> listOf(behandling(fagsak, resultat = resultat))
            }

        every { behandlingService.hentBehandlinger(fagsak.id) } returns behandlinger
    }
}
