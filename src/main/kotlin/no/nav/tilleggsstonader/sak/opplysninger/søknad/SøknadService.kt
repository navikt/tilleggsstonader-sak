package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SøknadsskjemaBarnetilsyn
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SøknadService(
    private val søknadBehandlingRepository: SøknadBehandlingRepository,
    private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentSøknadBarnetilsyn(behandlingId: UUID): SøknadBarnetilsyn? {
        return søknadBehandlingRepository.findByIdOrNull(behandlingId)
            ?.let { søknadBarnetilsynRepository.findByIdOrThrow(it.søknadId) }
    }

    fun lagreSøknad(behandlingId: UUID, journalpostId: String, skjema: SøknadsskjemaBarnetilsyn): SøknadBarnetilsyn {
        val søknad = SøknadsskjemaMapper.map(skjema, journalpostId)
        val søknadBarnetilsyn = søknadBarnetilsynRepository.insert(søknad)
        søknadBehandlingRepository.insert(SøknadBehandling(behandlingId, søknad.id))
        return søknadBarnetilsyn
    }

    fun kopierSøknad(forrigeBehandlingId: UUID, nyBehandlingId: UUID) {
        val søknad = søknadBehandlingRepository.findByIdOrNull(forrigeBehandlingId)
        if (søknad == null) {
            logger.info("Finner ingen søknad på forrige behandling=$forrigeBehandlingId")
            return
        }
        søknadBehandlingRepository.insert(
            søknad.copy(
                behandlingId = nyBehandlingId,
                sporbar = Sporbar(),
            ),
        )
    }
}
