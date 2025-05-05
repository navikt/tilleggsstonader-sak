package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak.MANUELT_OPPRETTET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param forrigeIverksatteBehandlingId forrige iverksatte behandling
 */
data class Behandling(
    @Id
    val id: BehandlingId = BehandlingId.random(),
    val fagsakId: FagsakId,
    val forrigeIverksatteBehandlingId: BehandlingId? = null,
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
    val henlagtBegrunnelse: String? = null,
    val vedtakstidspunkt: LocalDateTime? = null,
    val revurderFra: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    val nyeOpplysningerMetadata: NyeOpplysningerMetadata? = null,
) {
    fun kanHenlegges(): Boolean = !status.behandlingErLåstForVidereRedigering()

    fun erAvsluttet(): Boolean = status == BehandlingStatus.FERDIGSTILT

    fun vedtakstidspunktEllerFeil(): LocalDateTime = this.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt for behandling=$id")

    init {
        if (resultat == BehandlingResultat.HENLAGT) {
            feilHvis(henlagtÅrsak == null) { "Kan ikke henlegge behandling uten en årsak" }
        }
        feilHvis(revurderFra != null && type != BehandlingType.REVURDERING) {
            "Kan ikke sette revurder fra når behandlingen ikke er en revurdering"
        }
    }
}

enum class BehandlingKategori {
    EØS,
    NASJONAL,
}

/**
 * @property MANUELT_OPPRETTET brukes ved manuelt opprettede førstegangsbehandling via admin
 * @property MANUELT_OPPRETTET_UTEN_BREV brukes ved manuelt opprettede førstegangsbehandling
 * via admin der man ikke skal sende brev. Eks ved migrering der noe skal stanses i Arena og opprettes i TS
 */
enum class BehandlingÅrsak {
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    KORRIGERING_UTEN_BREV,
    PAPIRSØKNAD,
    SATSENDRING,
    MANUELT_OPPRETTET,
    MANUELT_OPPRETTET_UTEN_BREV,
    ;

    fun erSøknadEllerPapirsøknad() = this == SØKNAD || this == PAPIRSØKNAD
}

enum class BehandlingType(
    val visningsnavn: String,
) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
}

/**
 * Sjekkes sammen med vedtakstidspunkt i behandling_resultat_vedtakstidspunkt_check
 * // TODO: Legg i kontrakter.
 */

enum class BehandlingResultat(
    val displayName: String,
    val skalIverksettes: Boolean = false,
) {
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

    fun visningsnavn(): String =
        this.name
            .replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    fun behandlingErLåstForVidereRedigering(): Boolean = setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT, SATT_PÅ_VENT).contains(this)

    fun validerKanBehandlingRedigeres() {
        feilHvis(behandlingErLåstForVidereRedigering()) {
            genererFeiltekstForBehandlingsstatus()
        }
    }

    private fun genererFeiltekstForBehandlingsstatus(): String {
        val prefix = "Kan ikke gjøre endringer på denne behandlingen fordi"
        return when (this) {
            FATTER_VEDTAK -> "$prefix vedtak er sendt til beslutter."
            IVERKSETTER_VEDTAK -> "$prefix vedtak er alt fattet av beslutter."
            FERDIGSTILT -> "$prefix den er ferdigstilt."
            SATT_PÅ_VENT -> "$prefix den er satt på vent."
            else -> error("Burde kunne redigere en behandling med status=$this.")
        }
    }

    fun iverksetterEllerFerdigstilt() = this == IVERKSETTER_VEDTAK || this == FERDIGSTILT
}

@Table("behandling_ekstern")
data class EksternBehandlingId(
    @Id
    val id: Long = 0,
    val behandlingId: BehandlingId,
)
