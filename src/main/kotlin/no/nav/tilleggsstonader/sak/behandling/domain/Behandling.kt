package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.util.enumTilVisningsnavn
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
    override val id: BehandlingId = BehandlingId.random(),
    override val fagsakId: FagsakId,
    override val forrigeIverksatteBehandlingId: BehandlingId? = null,
    // @Version ?
    val versjon: Int = 0,
    override val type: BehandlingType,
    @Column("behandling_metode")
    override val behandlingMetode: BehandlingMetode,
    override val status: BehandlingStatus,
    override val steg: StegType,
    override val kategori: BehandlingKategori,
    @Column("arsak")
    override val årsak: BehandlingÅrsak,
    override val kravMottatt: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    override val resultat: BehandlingResultat,
    @Column("henlagt_arsak")
    override val henlagtÅrsak: HenlagtÅrsak? = null,
    override val henlagtBegrunnelse: String? = null,
    override val vedtakstidspunkt: LocalDateTime? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    override val årsakMetadata: ÅrsakMetadata? = null,
) : ErBehandling {
    init {
        if (erHenlagt()) {
            feilHvis(henlagtÅrsak == null) { "Kan ikke henlegge behandling uten en årsak" }
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
    KJØRELISTE,
    REGISTRER_KJØRELISTE_FOR_BRUKER,
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    KORRIGERING_UTEN_BREV,
    PAPIRSØKNAD,
    SATSENDRING,
    MANUELT_OPPRETTET,
    MANUELT_OPPRETTET_UTEN_BREV,
    OMGJØRING_ETTER_KLAGE,
    ;

    fun erSøknadEllerPapirsøknad() = this == SØKNAD || this == PAPIRSØKNAD
}

enum class BehandlingType(
    val visningsnavn: String,
) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    KJØRELISTE("Kjøreliste"),
}

/**
 * Sjekkes sammen med vedtakstidspunkt i behandling_resultat_vedtakstidspunkt_check
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

enum class BehandlingMetode {
    MANUELL,
    AUTOMATISK,
    BATCH,
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
    SATT_PÅ_VENT,

    ;

    fun visningsnavn(): String = enumTilVisningsnavn(name)

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
