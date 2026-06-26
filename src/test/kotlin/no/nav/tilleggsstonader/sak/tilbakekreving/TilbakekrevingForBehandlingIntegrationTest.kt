package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockIsEnabled
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevingsstatus
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

class TilbakekrevingForBehandlingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var tilbakekrevinghendelseService: TilbakekrevinghendelseService

    @Nested
    inner class HentTilbakekrevingHendelser {
        @Test
        fun `henter tom liste når det ikke finnes hendelser på behandlingen`() {
            val behandling = opprettBehandling()

            val hendelser = kall.tilbakekreving.hentHendelser(behandling.id)

            assertThat(hendelser).isEmpty()
        }

        @Test
        fun `henter hendelser sortert på hendelseOpprettet`() {
            val behandling = opprettBehandling()

            val eldsteHendelseOpprettet = (1 januar 2025).atTime(10, 0)
            val nyesteHendelseOpprettet = (1 februar 2025).atTime(10, 0)

            persisterHendelse(behandling.id, nyesteHendelseOpprettet, "TIL_BEHANDLING")
            persisterHendelse(behandling.id, eldsteHendelseOpprettet, "OPPRETTET")

            val hendelser = kall.tilbakekreving.hentHendelser(behandling.id)

            assertThat(hendelser.map { it.behandlingstatus }).containsExactly("OPPRETTET", "TIL_BEHANDLING")
        }

        @Test
        fun `henter hendelser med alle felter`() {
            val behandling = opprettBehandling()

            val hendelseOpprettet = (1 januar 2025).atTime(10, 0)
            persisterHendelse(behandling.id, hendelseOpprettet, "TIL_BEHANDLING")

            val hendelse = kall.tilbakekreving.hentHendelser(behandling.id).single()

            assertThat(hendelse.hendelseOpprettet).isEqualTo(hendelseOpprettet)
            assertThat(hendelse.sakOpprettet).isEqualTo(hendelseOpprettet)
            assertThat(hendelse.varselSendtTidspunkt).isNull()
            assertThat(hendelse.behandlingstatus).isEqualTo("TIL_BEHANDLING")
            assertThat(hendelse.totaltFeilutbetaltBeløp).isEqualByComparingTo(BigDecimal("10000"))
            assertThat(hendelse.tilbakekrevingFom).isEqualTo(1 januar 2025)
            assertThat(hendelse.tilbakekrevingTom).isEqualTo(31 mai 2025)
            assertThat(hendelse.tilbakekrevingBehandlingId).isEqualTo("tilbakekreving-1")
            assertThat(hendelse.saksbehandlingURL).isEqualTo("http://localhost/tilbakekreving-1")
        }

        @Test
        fun `henter kun hendelser tilknyttet behandlingen i forespørselen`() {
            val behandling = opprettBehandling()
            val annenBehandling = opprettBehandling(stønadstype = Stønadstype.BARNETILSYN)

            persisterHendelse(annenBehandling.id, LocalDateTime.now(), "TIL_BEHANDLING")

            assertThat(kall.tilbakekreving.hentHendelser(behandling.id)).isEmpty()
            assertThat(kall.tilbakekreving.hentHendelser(annenBehandling.id)).hasSize(1)
        }
    }

    @Nested
    inner class HarTilbakekrevingSak {
        @Test
        fun `behandling har ikke tilbakekrevingssak når det ikke finnes hendelser`() {
            val behandling = opprettBehandling()

            assertThat(kall.behandling.hent(behandling.id).harTilbakekrevingSak).isFalse()
        }

        @Test
        fun `behandling har tilbakekrevingssak når det finnes hendelser`() {
            val behandling = opprettBehandling()
            persisterHendelse(behandling.id, LocalDateTime.now(), "TIL_BEHANDLING")

            assertThat(kall.behandling.hent(behandling.id).harTilbakekrevingSak).isTrue()
        }

        @Test
        fun `behandling har ikke tilbakekrevingssak når feature-toggle er av selv om det finnes hendelser`() {
            val behandling = opprettBehandling()
            persisterHendelse(behandling.id, LocalDateTime.now(), "TIL_BEHANDLING")

            unleashService.mockIsEnabled(Toggle.KAN_VISE_TILBAKEKREVING, false)

            assertThat(kall.behandling.hent(behandling.id).harTilbakekrevingSak).isFalse()
        }
    }

    private fun opprettBehandling(stønadstype: Stønadstype = Stønadstype.LÆREMIDLER) =
        testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = stønadstype)

    private fun persisterHendelse(
        behandlingId: BehandlingId,
        hendelseOpprettet: LocalDateTime,
        behandlingstatus: String,
    ) = tilbakekrevinghendelseService.persisterHendelse(
        behandlingId,
        Tilbakekrevingsstatus(
            hendelseOpprettet = hendelseOpprettet,
            sakOpprettet = hendelseOpprettet,
            varselSendtTidspunkt = null,
            behandlingstatus = behandlingstatus,
            totaltFeilutbetaltBeløp = BigDecimal("10000"),
            tilbakekrevingFom = 1 januar 2025,
            tilbakekrevingTom = 31 mai 2025,
            tilbakekrevingBehandlingId = "tilbakekreving-1",
            saksbehandlingURL = "http://localhost/tilbakekreving-1",
        ),
    )
}
