package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Søknad
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadLæremidler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadMetadata
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadskjemaLæremidlerMapper
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaBarnetilsynMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SøknadService(
    private val søknadMetadataRepository: SøknadMetadataRepository,
    private val søknadBehandlingRepository: SøknadBehandlingRepository,
    private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
    private val søknadLæremidlerRepository: SøknadLæremidlerRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentSøknadMetadata(behandlingId: BehandlingId): SøknadMetadata? = søknadMetadataRepository.finnForBehandling(behandlingId)

    fun hentSøknadBarnetilsyn(behandlingId: BehandlingId): SøknadBarnetilsyn? =
        søknadBehandlingRepository
            .findByIdOrNull(behandlingId)
            ?.let { søknadBarnetilsynRepository.findByIdOrThrow(it.søknadId) }

    fun hentSøknadLæremidler(behandlingId: BehandlingId): SøknadLæremidler? =
        søknadBehandlingRepository
            .findByIdOrNull(behandlingId)
            ?.let { søknadLæremidlerRepository.findByIdOrThrow(it.søknadId) }

    fun lagreSøknad(
        behandlingId: BehandlingId,
        journalpost: Journalpost,
        skjema: Søknadsskjema<out Skjema>,
    ): Søknad<*> {
        val søknad =
            when (skjema.skjema) {
                is SøknadsskjemaBarnetilsyn ->
                    SøknadsskjemaBarnetilsynMapper.map(
                        skjema.mottattTidspunkt,
                        skjema.språk,
                        journalpost,
                        skjema.skjema as SøknadsskjemaBarnetilsyn,
                    )
                is SøknadsskjemaLæremidler ->
                    SøknadskjemaLæremidlerMapper.map(
                        skjema.mottattTidspunkt,
                        skjema.språk,
                        journalpost,
                        skjema.skjema as SøknadsskjemaLæremidler,
                    )
            }
        val lagretSøknad =
            when (søknad) {
                is SøknadBarnetilsyn -> søknadBarnetilsynRepository.insert(søknad)
                is SøknadLæremidler -> søknadLæremidlerRepository.insert(søknad)
            }
        søknadBehandlingRepository.insert(SøknadBehandling(behandlingId, søknad.id))
        return lagretSøknad
    }

    fun kopierSøknad(
        forrigeBehandlingId: BehandlingId,
        nyBehandlingId: BehandlingId,
    ) {
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
