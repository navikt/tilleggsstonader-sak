package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaBarnetilsynMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SøknadService(
    private val søknadBehandlingRepository: SøknadBehandlingRepository,
    private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentSøknadBarnetilsyn(behandlingId: BehandlingId): SøknadBarnetilsyn? {
        return søknadBehandlingRepository.findByIdOrNull(behandlingId)
            ?.let { søknadBarnetilsynRepository.findByIdOrThrow(it.søknadId) }
    }

    fun lagreSøknad(
        behandlingId: BehandlingId,
        journalpost: Journalpost,
        skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>,
    ): SøknadBarnetilsyn {
        val søknad = SøknadsskjemaBarnetilsynMapper.map(skjema, journalpost)
        val søknadBarnetilsyn = søknadBarnetilsynRepository.insert(søknad)
        søknadBehandlingRepository.insert(SøknadBehandling(behandlingId, søknad.id))
        return søknadBarnetilsyn
    }

    fun kopierSøknad(forrigeBehandlingId: BehandlingId, nyBehandlingId: BehandlingId) {
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
