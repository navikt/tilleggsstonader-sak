package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository

@Repository
interface FagsakRepository :
    RepositoryInterface<FagsakDomain, FagsakId>,
    InsertUpdateRepository<FagsakDomain> {
    // language=PostgreSQL
    @Query(
        """SELECT DISTINCT f.*, fe.id AS eksternid_id
                    FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
                    WHERE pi.ident IN (:personIdenter)
                    AND f.stonadstype = :stønadstype""",
    )
    fun findBySøkerIdent(
        personIdenter: Set<String>,
        stønadstype: Stønadstype,
    ): FagsakDomain?

    fun findByFagsakPersonIdAndStønadstype(
        fagsakPersonId: FagsakPersonId,
        stønadstype: Stønadstype,
    ): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT f.*, fe.id AS eksternid_id
                    FROM fagsak f
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""",
    )
    fun finnFagsakTilBehandling(behandlingId: BehandlingId): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT DISTINCT f.*, fe.id AS eksternid_id FROM fagsak f 
                JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
              WHERE ident IN (:personIdenter)""",
    )
    fun findBySøkerIdent(personIdenter: Set<String>): List<FagsakDomain>

    fun findByFagsakPersonId(fagsakPersonId: FagsakPersonId): List<FagsakDomain>

    // language=PostgreSQL
    @Query(
        """SELECT f.*, fe.id AS eksternid_id         
                    FROM fagsak f         
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id       
                    WHERE fe.id = :eksternId""",
    )
    fun finnMedEksternId(eksternId: Long): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT pi.ident FROM fagsak f
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
              WHERE f.id=:fagsakId
              ORDER BY pi.endret_tid DESC
              LIMIT 1""",
    )
    fun finnAktivIdent(fagsakId: FagsakId): String

    // language=PostgreSQL
    @Query(
        """SELECT COUNT(*) > 0 FROM gjeldende_iverksatte_behandlinger b 
              JOIN tilkjent_ytelse ty
              ON b.id = ty.behandling_id
              JOIN andel_tilkjent_ytelse aty 
              ON ty.id = aty.tilkjent_ytelse_id
              AND aty.fom >= CURRENT_DATE 
              WHERE b.fagsak_id = :fagsakId
              LIMIT 1""",
    )
    fun harLøpendeUtbetaling(fagsakId: FagsakId): Boolean

    @Query(
        """
        SELECT DISTINCT
         f.id, 
         fe.id AS ekstern_fagsak_id,
         f.stonadstype,
         FIRST_VALUE(ident) OVER (PARTITION BY pi.fagsak_person_id ORDER BY pi.endret_tid DESC) AS ident
         FROM fagsak f
         JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
         JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
         WHERE f.id IN (:fagsakIder)
    """,
    )
    fun hentFagsakMetadata(fagsakIder: Set<FagsakId>): List<FagsakMetadata>
}
