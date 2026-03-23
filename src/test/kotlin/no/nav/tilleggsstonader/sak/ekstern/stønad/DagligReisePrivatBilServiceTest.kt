package no.nav.tilleggsstonader.sak.ekstern.stønad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DagligReisePrivatBilServiceTest {
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val personService = mockk<PersonService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val vedtakService = mockk<VedtakService>()

    private val service = DagligReisePrivatBilService(
        fagsakPersonService = fagsakPersonService,
        personService = personService,
        behandlingRepository = behandlingRepository,
        vedtakService = vedtakService,
    )

    private val ident = "12345678901"

    @BeforeEach
    fun setUp() {
        every { personService.hentFolkeregisterIdenter(ident) } returns
            PdlIdenter(listOf(PdlIdent(ident = ident, historisk = false, gruppe = "FOLKEREGISTERIDENT")))
    }

    @Test
    fun `skal returnere ingen rammevedtak når person ikke finnes`() {
        every { fagsakPersonService.finnPerson(any<Set<String>>()) } returns null

        val resultat = service.hentRammevedtakPåIdent(ident)

        assertThat(resultat).isEmpty()
        verify(exactly = 0) { behandlingRepository.finnSisteIverksatteBehandlingerForFagsakPersonId(any(), any()) }
    }

}
