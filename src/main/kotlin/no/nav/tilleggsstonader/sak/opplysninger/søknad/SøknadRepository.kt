package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SøknadBarnetilsynRepository :
    RepositoryInterface<SøknadBarnetilsyn, UUID>, InsertUpdateRepository<SøknadBarnetilsyn>

@Repository
interface SøknadBehandlingRepository :
    RepositoryInterface<SøknadBehandling, UUID>, InsertUpdateRepository<SøknadBehandling>
