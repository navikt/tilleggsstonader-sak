package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class KjørelisteBehandlingBrevServiceTest {
    private val kjørelisteBehandlingBrevRepository = mockk<KjørelisteBehandlingBrevRepository>()
    private val service =
        KjørelisteBehandlingBrevService(
            kjørelisteBehandlingBrevRepository = kjørelisteBehandlingBrevRepository,
            familieDokumentClient = mockk(),
            htmlifyClient = mockk(),
            personService = mockk(),
            vedtakService = mockk(),
            behandlingService = mockk(),
        )

    private val behandlingId = BehandlingId(UUID.randomUUID())
    private val brevMedBegrunnelse =
        KjørelisteBehandlingBrev(
            behandlingId = behandlingId,
            saksbehandlerHtml = "<html/>",
            pdf = Fil(ByteArray(0)),
            saksbehandlerIdent = "Z999999",
            begrunnelse = "eksisterende tekst",
        )

    @Nested
    inner class UtledBegrunnelse {
        @Test
        fun `null i request bevarer eksisterende begrunnelse`() {
            every { kjørelisteBehandlingBrevRepository.findByBehandlingId(behandlingId) } returns brevMedBegrunnelse
            val resultat = service.utledBegrunnelse(nyBegrunnelse = null, behandlingId)
            assertThat(resultat).isEqualTo("eksisterende tekst")
        }

        @Test
        fun `null i request returnerer null når det ikke finnes eksisterende begrunnelse`() {
            every { kjørelisteBehandlingBrevRepository.findByBehandlingId(behandlingId) } returns null
            val resultat = service.utledBegrunnelse(nyBegrunnelse = null, behandlingId)
            assertThat(resultat).isNull()
        }

        @Test
        fun `tom streng fjerner begrunnelsen`() {
            val resultat = service.utledBegrunnelse(nyBegrunnelse = "", behandlingId)
            assertThat(resultat).isNull()
        }

        @Test
        fun `streng med kun whitespace fjerner begrunnelsen`() {
            val resultat = service.utledBegrunnelse(nyBegrunnelse = "   ", behandlingId)
            assertThat(resultat).isNull()
        }

        @Test
        fun `eksplisitt tekst overskriver eksisterende begrunnelse`() {
            val resultat = service.utledBegrunnelse(nyBegrunnelse = "ny tekst", behandlingId)
            assertThat(resultat).isEqualTo("ny tekst")
        }

        @Test
        fun `eksplisitt tekst lagres selv om det ikke finnes noen eksisterende`() {
            val resultat = service.utledBegrunnelse(nyBegrunnelse = "ny tekst", behandlingId)
            assertThat(resultat).isEqualTo("ny tekst")
        }
    }
}
