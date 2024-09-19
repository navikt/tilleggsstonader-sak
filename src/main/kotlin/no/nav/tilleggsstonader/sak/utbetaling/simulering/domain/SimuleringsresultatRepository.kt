package no.nav.tilleggsstonader.sak.utbetaling.simulering.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository

@Repository
interface SimuleringsresultatRepository :
    RepositoryInterface<Simuleringsresultat, BehandlingId>,
    InsertUpdateRepository<Simuleringsresultat>
