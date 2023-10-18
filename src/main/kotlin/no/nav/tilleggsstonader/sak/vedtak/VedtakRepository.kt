package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.repository.NoRepositoryBean
import java.util.UUID

@NoRepositoryBean
interface VedtakRepository<T> : RepositoryInterface<T, UUID>, InsertUpdateRepository<T>
