package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OppfølgingRepository :
    RepositoryInterface<Oppfølging, UUID>,
    InsertUpdateRepository<Oppfølging> {
    @Modifying
    @Query("update oppfolging SET aktiv = false, version=version + 1 WHERE aktiv = true")
    fun markerAlleAktiveSomIkkeAktive()

    @Query(
        """
        SELECT o.* 
        FROM oppfolging o
        WHERE o.behandling_id 
            IN (SELECT id FROM behandling WHERE fagsak_id = (SELECT fagsak_id FROM behandling where id = :behandlingId))
        ORDER BY o.opprettet_tidspunkt DESC
        LIMIT 1
    """,
    )
    fun finnSisteForFagsak(behandlingId: BehandlingId): Oppfølging?

    @Query(
        """
        SELECT 
        o.*,
        fe.id AS saksnummer,
        f.stonadstype,
        f.fagsak_person_id,
        b.vedtakstidspunkt,
        (select count(*) > 0 from behandling b where b.forrige_behandling_id = o.behandling_id) har_nyere_behandling
        FROM oppfolging o
        JOIN behandling b ON b.id = o.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
        WHERE o.aktiv=true
    """,
    )
    fun finnAktiveMedDetaljer(): List<OppfølgingMedDetaljer>

    @Query(
        """
        SELECT 
        o.*,
        fe.id AS saksnummer,
        f.stonadstype,
        f.fagsak_person_id,
        b.vedtakstidspunkt,
        (select count(*) > 0 from behandling b where b.forrige_behandling_id = o.behandling_id) har_nyere_behandling
        FROM oppfolging o 
        JOIN behandling b ON b.id = o.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
        WHERE o.aktiv=true
        AND o.behandling_id = :behandlingId
    """,
    )
    fun finnAktivMedDetaljer(behandlingId: BehandlingId): OppfølgingMedDetaljer
}

@Table("oppfolging")
data class Oppfølging(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    @Version
    val version: Int = 0,
    val aktiv: Boolean = true,
    val opprettetTidspunkt: LocalDateTime = SporbarUtils.now(),
    val data: OppfølgingData,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "kontrollert_")
    val kontrollert: Kontrollert? = null,
)

data class OppfølgingData(
    val perioderTilKontroll: List<PeriodeForKontroll>,
)

data class Kontrollert(
    val tidspunkt: LocalDateTime = SporbarUtils.now(),
    val saksbehandler: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val utfall: KontrollertUtfall,
    val kommentar: String?,
)

enum class KontrollertUtfall {
    HÅNDTERT,
    IGNORERES,
    UTSETTES,
    UNDER_ARBEID,
}

data class PeriodeForKontroll(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val endringAktivitet: List<Kontroll>,
    val endringMålgruppe: List<Kontroll>,
) {
    fun trengerKontroll(): Boolean = endringAktivitet.isNotEmpty() || endringMålgruppe.isNotEmpty()
}

data class Kontroll(
    val årsak: ÅrsakKontroll,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

enum class ÅrsakKontroll {
    INGEN_TREFF,
    FOM_ENDRET,
    TOM_ENDRET,
    TREFF_MEN_FEIL_TYPE,
    FINNER_IKKE_REGISTERAKTIVITET,
}

/**
 * Brukes for henting av aktiv behandling som joiner med andre tabeller også
 * @param harNyereBehandling er true hvis det er en behandling som er opprettet etter denne behandlingen
 */
data class OppfølgingMedDetaljer(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val version: Int = 0,
    val opprettetTidspunkt: LocalDateTime = SporbarUtils.now(),
    val data: OppfølgingData,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "kontrollert_")
    val kontrollert: Kontrollert? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    val behandlingsdetaljer: Behandlingsdetaljer,
)

data class Behandlingsdetaljer(
    val saksnummer: Long,
    val fagsakPersonId: FagsakPersonId,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val vedtakstidspunkt: LocalDateTime,
    val harNyereBehandling: Boolean,
)
