package no.nav.tilleggsstonader.sak.opplysninger.søknad

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Søknad
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadLæremidler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadMetadata
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Alle repositories her henter data fra den samme tabellen, "soknad"
 * Men for at mapping skal virke med feltet [Søknad.data] så må(kan)
 * man ha ulike repositories for å hente ut søknaden
 */

@Repository
interface SøknadBarnetilsynRepository :
    RepositoryInterface<SøknadBarnetilsyn, UUID>,
    InsertUpdateRepository<SøknadBarnetilsyn>

/**
 * Brukes kun for å hente ut metadata fra [SøknadMetadata]
 */
@Repository
interface SøknadMetadataRepository : org.springframework.data.repository.Repository<SøknadMetadata, UUID> {
    @Query(
        """
        SELECT s.* FROM soknad s
        JOIN soknad_behandling sb ON sb.soknad_id = s.id
        WHERE sb.behandling_id = :behandlingId
    """,
    )
    fun finnForBehandling(behandlingId: BehandlingId): SøknadMetadata?
}

@Repository
interface SøknadLæremidlerRepository :
    RepositoryInterface<SøknadLæremidler, UUID>,
    InsertUpdateRepository<SøknadLæremidler>

@Repository
interface SøknadBoutgifterRepository :
    RepositoryInterface<SøknadBoutgifter, UUID>,
    InsertUpdateRepository<SøknadBoutgifter>

@Repository
interface SøknadBehandlingRepository :
    RepositoryInterface<SøknadBehandling, BehandlingId>,
    InsertUpdateRepository<SøknadBehandling>
