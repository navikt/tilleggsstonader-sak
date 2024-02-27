package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface AndelTilkjentYtelseRepository :
    RepositoryInterface<AndelTilkjentYtelse, UUID>, InsertUpdateRepository<AndelTilkjentYtelse> {

    @Query(
        """
        SELECT DISTINCT ty.behandling_id from andel_tilkjent_ytelse aty
        JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
        JOIN behandling b ON b.id = ty.behandling_id
        WHERE aty.tom <= :sisteDatoIMåned AND aty.status_iverksetting = 'UBEHANDLET'
        AND b.status = 'FERDIGSTILT' AND b.resultat IN ('INNVILGET', 'OPPHØRT')
        """,
    )
    fun finnBehandlingerForIverksetting(sisteDatoIMåned: LocalDate): List<UUID>
}
