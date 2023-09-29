package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?

    // language=PostgreSQL
    @Query(
        """
        SELECT ty.*
        FROM tilkjent_ytelse ty
            JOIN behandling b ON b.id = ty.behandling_id
        WHERE b.fagsak_id = :fagsakId
         AND b.status = 'FERDIGSTILT'
         AND b.resultat IN ('OPPHØRT', 'INNVILGET') 
        """,
    )
    fun finnAlleIverksatteForFagsak(fagsakId: UUID): List<TilkjentYtelse>

    // language=PostgreSQL
    @Query(
        """
        SELECT ty.*
        FROM tilkjent_ytelse ty 
        WHERE ty.behandling_id IN (SELECT id FROM gjeldende_iverksatte_behandlinger WHERE stonadstype=:stønadstype) 
         AND EXISTS (SELECT 1 FROM andel_tilkjent_ytelse aty 
                        WHERE ty.id = aty.tilkjent_ytelse
                         AND aty.stonad_tom >= :datoForAvstemming
                         AND aty.belop > 0)
          """,
    )
    fun finnTilkjentYtelserTilKonsistensavstemming(
        stønadstype: Stønadstype,
        datoForAvstemming: LocalDate,
    ): List<TilkjentYtelse>
}
