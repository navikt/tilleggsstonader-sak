package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.søknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SøknadMetadataRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var søknadBehandlingRepository: SøknadBehandlingRepository

    @Autowired
    lateinit var søknadBarnetilsynRepository: SøknadBarnetilsynRepository

    @Autowired
    lateinit var søknadMetadataRepository: SøknadMetadataRepository

    @Test
    fun `skal hente metadata fra søknaden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val søknadBarnetilsyn = søknadBarnetilsynRepository.insert(søknadBarnetilsyn())
        søknadBehandlingRepository.insert(
            SøknadBehandling(
                behandlingId = behandling.id,
                søknadId = søknadBarnetilsyn.id,
            ),
        )

        val metadata = søknadMetadataRepository.finnForBehandling(behandling.id)!!
        assertThat(metadata.språk).isEqualTo(søknadBarnetilsyn.språk)
        assertThat(metadata.journalpostId).isEqualTo(søknadBarnetilsyn.journalpostId)
        assertThat(metadata.mottattTidspunkt).isEqualTo(søknadBarnetilsyn.mottattTidspunkt)
    }

    @Test
    fun `skal ikke finne noen informasjon hvis det ikke finnes en søknad`() {
        assertThat(søknadMetadataRepository.finnForBehandling(BehandlingId.random())).isNull()
    }
}
