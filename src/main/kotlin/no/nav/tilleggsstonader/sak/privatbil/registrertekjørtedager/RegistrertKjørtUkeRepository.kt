package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RegistrertKjørtUkeRepository :
    RepositoryInterface<RegistrertKjørtUke, UUID>,
    InsertUpdateRepository<RegistrertKjørtUke> {
    fun findByBehandlingId(behandlingId: BehandlingId): List<RegistrertKjørtUke>
}
