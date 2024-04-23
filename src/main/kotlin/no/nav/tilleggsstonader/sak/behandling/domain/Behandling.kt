package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * @param forrigeBehandlingId forrige iverksatte behandling
 */
data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val forrigeBehandlingId: UUID? = null,
    // @Version ?
    val versjon: Int = 0,

    val type: BehandlingType,
    val status: BehandlingStatus,
    val steg: StegType,
    val kategori: BehandlingKategori,
    @Column("arsak")
    val årsak: BehandlingÅrsak,
    val kravMottatt: LocalDate? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val resultat: BehandlingResultat,
    @Column("henlagt_arsak")
    val henlagtÅrsak: HenlagtÅrsak? = null,
    val vedtakstidspunkt: LocalDateTime? = null,
) {

    fun kanHenlegges(): Boolean = !status.behandlingErLåstForVidereRedigering()

    fun erMigrering(): Boolean = årsak == BehandlingÅrsak.MIGRERING

    fun erAvsluttet(): Boolean = status == BehandlingStatus.FERDIGSTILT

    fun vedtakstidspunktEllerFeil(): LocalDateTime =
        this.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt for behandling=$id")

    init {
        if (resultat == BehandlingResultat.HENLAGT) {
            brukerfeilHvis(henlagtÅrsak == null) { "Kan ikke henlegge behandling uten en årsak" }
        }
    }
}

enum class BehandlingKategori {
    EØS,
    NASJONAL,
}

enum class BehandlingÅrsak {
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    MIGRERING,
    KORRIGERING_UTEN_BREV,
    PAPIRSØKNAD,
    SATSENDRING,
    MANUELT_OPPRETTET,
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
}

/**
 * Sjekkes sammen med vedtakstidspunkt i [behandling_resultat_vedtakstidspunkt_check]
 */
enum class BehandlingResultat(val displayName: String, val skalIverksettes: Boolean = false) {
    INNVILGET(displayName = "Innvilget", true),
    OPPHØRT(displayName = "Opphørt", true),
    AVSLÅTT(displayName = "Avslått"),
    IKKE_SATT(displayName = "Ikke satt"),
    HENLAGT(displayName = "Henlagt"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
    SATT_PÅ_VENT,

    ;

    fun visningsnavn(): String {
        return this.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    fun behandlingErLåstForVidereRedigering(): Boolean =
        setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT, SATT_PÅ_VENT).contains(this)
}

@Table("behandling_ekstern")
data class EksternBehandlingId(
    @Id
    val id: Long = 0,
    val behandlingId: UUID,
)
