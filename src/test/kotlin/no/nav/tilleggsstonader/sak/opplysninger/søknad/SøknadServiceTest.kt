package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadUtil.barnMedBarnepass
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SøknadServiceTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var søknadBehandlingRepository: SøknadBehandlingRepository

    @Autowired
    lateinit var søknadService: SøknadService

    @Test
    internal fun `skal kopiere kobling av søknad til ny behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = testoppsettService.lagre(behandling(fagsak))

        val søknadsskjema = lagreSøknad(behandling)
        val søknadsskjemaForRevurdering = kopierSøknadTilRevurdering(behandling, revurdering)

        assertThat(søknadsskjema).isEqualTo(søknadsskjemaForRevurdering)
    }

    @Test
    internal fun `kopiering av søknad til annen behandling skal kun beholde søknadId`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = testoppsettService.lagre(behandling(fagsak))

        lagreSøknad(behandling)
        kopierSøknadTilRevurdering(behandling, revurdering)

        val søknad = søknadBehandlingRepository.findByIdOrThrow(behandling.id)
        val søknadForRevurdering = søknadBehandlingRepository.findByIdOrThrow(revurdering.id)

        assertThat(søknad.søknadId).isEqualTo(søknadForRevurdering.søknadId)
        assertThat(søknad.behandlingId).isNotEqualTo(søknadForRevurdering.behandlingId)
        assertThat(søknad.sporbar).isNotEqualTo(søknadForRevurdering.sporbar)
    }

    @Test
    fun `skal kunne lagre komplett søknad for barnetilsyn`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val skjema = søknadskjemaBarnetilsyn(
            barnMedBarnepass = listOf(barnMedBarnepass(ident = "barn1", navn = "navn1")),
        )
        val søknad = søknadService.lagreSøknad(behandling.id, "journalpostId", skjema)
        assertThat(søknad.journalpostId).isEqualTo("journalpostId")
        assertThat(søknad.barn).hasSize(1)
        assertThat(søknad.barn.single().ident).isEqualTo("barn1")
    }

    private fun kopierSøknadTilRevurdering(
        behandling: Behandling,
        revurdering: Behandling,
    ): SøknadBarnetilsyn? {
        søknadService.kopierSøknad(behandling.id, revurdering.id)
        return søknadService.hentSøknadBarnetilsyn(revurdering.id)
    }

    private fun lagreSøknad(
        behandling: Behandling,
    ): SøknadBarnetilsyn {
        søknadService.lagreSøknad(behandling.id, "123", søknadskjemaBarnetilsyn())
        return søknadService.hentSøknadBarnetilsyn(behandling.id)!!
    }
}
