package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MellomlagerBrevRepository : RepositoryInterface<MellomlagretBrev, UUID>, InsertUpdateRepository<MellomlagretBrev>
