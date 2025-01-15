package no.nav.tilleggsstonader.sak.statistikk.vedtak

import java.util.UUID
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface VedtaksstatistikkRepositoryV2 :
    RepositoryInterface<VedtaksstatistikkV2, UUID>,
    InsertUpdateRepository<VedtaksstatistikkV2>
