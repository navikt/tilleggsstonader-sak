package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SimuleringsresultatRepository :
    RepositoryInterface<Simuleringsresultat, UUID>,
    InsertUpdateRepository<Simuleringsresultat>
