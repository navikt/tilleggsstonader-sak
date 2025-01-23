package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtaksstatistikkRepositoryV2 :
    RepositoryInterface<VedtaksstatistikkV2, UUID>,
    InsertUpdateRepository<VedtaksstatistikkV2>
