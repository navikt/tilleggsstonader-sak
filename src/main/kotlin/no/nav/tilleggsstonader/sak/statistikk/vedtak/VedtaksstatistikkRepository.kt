package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@Deprecated(message = "Slettes når team Spenn og Familie har tatt i bruk VedtaksstatstikkV2")
interface VedtaksstatistikkRepository :
    RepositoryInterface<Vedtaksstatistikk, UUID>,
    InsertUpdateRepository<Vedtaksstatistikk>
