package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AndelTilkjentYtelseRepository :
    RepositoryInterface<AndelTilkjentYtelse, UUID>, InsertUpdateRepository<AndelTilkjentYtelse>
