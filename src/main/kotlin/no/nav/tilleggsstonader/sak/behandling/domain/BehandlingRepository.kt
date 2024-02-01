package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

/**
 * TODO: Alle metoder burde gåes igjennom senere
 */
@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    fun findByFagsakIdAndStatus(fagsakId: UUID, status: BehandlingStatus): List<Behandling>

    fun existsByFagsakId(fagsakId: UUID): Boolean

    // language=PostgreSQL
    @Query(
        """SELECT b.*, be.id AS eksternid_id         
                     FROM behandling b         
                     JOIN behandling_ekstern be 
                     ON be.behandling_id = b.id         
                     WHERE be.id = :eksternId""",
    )
    fun finnMedEksternId(eksternId: Long): Behandling?

    // language=PostgreSQL
    @Query(
        """SELECT pi.ident FROM fagsak f
                    JOIN behandling b ON f.id = b.fagsak_id
                    JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
                    WHERE b.id = :behandlingId
                    ORDER BY pi.endret_tid DESC 
                    LIMIT 1
                    """,
    )
    fun finnAktivIdent(behandlingId: UUID): String

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              be.id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.kategori,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.vedtakstidspunkt,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_av,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              fe.id AS ekstern_fagsak_id,
              f.stonadstype
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             JOIN behandling_ekstern be ON be.behandling_id = b.id         
             JOIN fagsak_ekstern fe ON f.id = fe.fagsak_id         
             WHERE b.id = :behandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """,
    )
    fun finnSaksbehandling(behandlingId: UUID): Saksbehandling

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              be.id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.kategori,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.vedtakstidspunkt,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_av,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              fe.id AS ekstern_fagsak_id,
              f.stonadstype
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             JOIN behandling_ekstern be ON be.behandling_id = b.id         
             JOIN fagsak_ekstern fe ON f.id = fe.fagsak_id         
             WHERE be.id = :eksternBehandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """,
    )
    fun finnSaksbehandling(eksternBehandlingId: Long): Saksbehandling

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        WHERE b.fagsak_id = :fagsakId
         AND b.resultat IN ('OPPHØRT', 'INNVILGET')
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.vedtakstidspunkt DESC
        LIMIT 1
    """,
    )
    fun finnSisteIverksatteBehandling(fagsakId: UUID): Behandling?

    @Query(
        """
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        JOIN fagsak f on b.fagsak_id = f.id 
        WHERE f.fagsak_person_id = :fagsakPersonId
         AND b.resultat IN ('OPPHØRT', 'INNVILGET', 'AVSLÅTT', 'IKKE_SATT')
         AND b.status NOT IN ('OPPRETTET')
         AND b.arsak != 'MIGRERING'
    """,
    )
    fun finnBehandlingerForGjenbrukAvVilkår(fagsakPersonId: UUID): List<Behandling>

    fun existsByFagsakIdAndStatusIsNot(fagsakId: UUID, behandlingStatus: BehandlingStatus): Boolean

    fun existsByFagsakIdAndStatusIsNotIn(fagsakId: UUID, behandlingStatus: List<BehandlingStatus>): Boolean

    // language=PostgreSQL
    @Query(
        """
        SELECT b.id behandling_id, be.id ekstern_behandling_id, fe.id ekstern_fagsak_id
        FROM behandling b
            JOIN behandling_ekstern be ON b.id = be.behandling_id
            JOIN fagsak_ekstern fe ON b.fagsak_id = fe.fagsak_id 
        WHERE behandling_id IN (:behandlingId)
        """,
    )
    fun finnEksterneIder(behandlingId: Set<UUID>): Set<EksternId>

    // language=PostgreSQL
    @Query(
        """
        SELECT pi.ident AS first, gib.id AS second 
        FROM gjeldende_iverksatte_behandlinger gib 
            JOIN person_ident pi ON gib.fagsak_person_id=pi.fagsak_person_id
        WHERE pi.ident IN (:personidenter)
            AND gib.stonadstype=:stønadstype
    """,
    )
    fun finnSisteIverksatteBehandlingerForPersonIdenter(
        personidenter: Collection<String>,
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    ): List<Pair<String, UUID>>

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*, f.stonadstype
        FROM behandling b
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN oppgave o on b.id = o.behandling_id
        WHERE NOT b.status = 'FERDIGSTILT'
        AND o.ferdigstilt = false
        AND o.type in ('BehandleSak', 'GodkjenneVedtak', 'BehandleUnderkjentVedtak')
        AND o.opprettet_tid < :opprettetTidFør
        AND f.stonadstype=:stønadstype
        """,
    )
    fun hentUferdigeBehandlingerOpprettetFørDato(
        stønadstype: Stønadstype,
        opprettetTidFør: LocalDateTime,
    ): List<Behandling>
}
