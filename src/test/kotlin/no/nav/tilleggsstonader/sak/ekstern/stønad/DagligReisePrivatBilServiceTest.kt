package no.nav.tilleggsstonader.sak.ekstern.stønad

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DagligReisePrivatBilServiceTest {
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val personService = mockk<PersonService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val vedtakService = mockk<VedtakService>()

    private val service =
        DagligReisePrivatBilService(
            fagsakPersonService = fagsakPersonService,
            personService = personService,
            behandlingRepository = behandlingRepository,
            vedtakService = vedtakService,
        )

    private val ident = "12345678901"

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erTokenMedIssuerTokenX() } returns false
        every { personService.hentFolkeregisterIdenter(any()) } returns
            PdlIdenter(listOf(PdlIdent(ident, false, "FOLKEREGISTERIDENT")))
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `skal returnere tom liste når fagsakPerson ikke finnes`() {
        every { fagsakPersonService.finnPerson(any()) } returns null

        val result = service.hentRammevedtaksPrivatBil(ident)

        assertThat(result).isEmpty()
    }
}
