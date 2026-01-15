package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface AndelTilkjentYtelseRepository :
    RepositoryInterface<AndelTilkjentYtelse, UUID>,
    InsertUpdateRepository<AndelTilkjentYtelse> {
    @Query(
        """
        SELECT DISTINCT ty.behandling_id from andel_tilkjent_ytelse aty
        JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse_id = ty.id
        JOIN behandling b ON b.id = ty.behandling_id
        WHERE aty.utbetalingsdato <= :utbetalingsdato AND aty.status_iverksetting = 'UBEHANDLET'
        AND b.status = 'FERDIGSTILT' AND b.resultat IN ('INNVILGET', 'OPPHØRT')
        """,
    )
    fun finnBehandlingerForIverksetting(utbetalingsdato: LocalDate): List<BehandlingId>

    @Query(
        """
            select distinct b.id from behandling b
                join tilkjent_ytelse ty on b.id = ty.behandling_id
                join andel_tilkjent_ytelse aty on aty.tilkjent_ytelse_id = ty.id
            where aty.iverksetting_id = :iverksettingId
        """,
    )
    fun finnBehandlingIdForIverksettingId(iverksettingId: UUID): BehandlingId

    fun findByIverksettingIverksettingId(iverksettingId: UUID): List<AndelTilkjentYtelse>

    fun findAndelTilkjentYtelsesByKildeBehandlingId(behandlingId: BehandlingId): List<AndelTilkjentYtelse>

    fun findAllByStatusIverksettingIn(status: List<StatusIverksetting>): List<AndelTilkjentYtelse>
}
