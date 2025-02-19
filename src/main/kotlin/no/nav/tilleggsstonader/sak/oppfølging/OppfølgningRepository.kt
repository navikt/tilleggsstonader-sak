package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
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
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OppfølgningRepository :
    RepositoryInterface<Oppfølging, UUID>,
    InsertUpdateRepository<Oppfølging> {
    @Modifying
    @Query("update oppfolging SET aktiv = false, version=version + 1 WHERE aktiv = true")
    fun markerAlleAktiveSomIkkeAktive()

    fun findByAktivIsTrue(): List<Oppfølging>
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
    val behandlingId: BehandlingId,
    val perioderTilKontroll: List<PeriodeForKontroll>,
)

data class Kontrollert(
    val tidspunkt: LocalDateTime = SporbarUtils.now(),
    val saksbehandler: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val kommentar: String?,
)

data class PeriodeForKontroll(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val endringAktivitet: List<Kontroll>,
    val endringMålgruppe: List<Kontroll>,
) {
    fun trengerKontroll(): Boolean = (endringAktivitet + endringMålgruppe).any { it.årsak.trengerKontroll }
}

data class Kontroll(
    val årsak: ÅrsakKontroll,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

enum class ÅrsakKontroll(
    val trengerKontroll: Boolean = true,
) {
    SKAL_IKKE_KONTROLLERES(trengerKontroll = false),
    INGEN_ENDRING(trengerKontroll = false),

    INGEN_TREFF,
    FOM_TOM_ENDRET,
    FOM_ENDRET,
    TOM_ENDRET,
    TREFF_MEN_FEIL_TYPE,
}
