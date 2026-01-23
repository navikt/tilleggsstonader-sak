package no.nav.tilleggsstonader.sak.googlemaps

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KjøreavstandLoggRepository :
    RepositoryInterface<KjøreavstandLogg, UUID>,
    InsertUpdateRepository<KjøreavstandLogg>
