package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.VilkårperiodeTypeDeserializer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

@Table("vilkar_periode")
data class Vilkårperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val kilde: KildeVilkårsperiode,
    @Column("forrige_vilkarperiode_id")
    val forrigeVilkårperiodeId: UUID? = null,

    override val fom: LocalDate,
    override val tom: LocalDate,
    val type: VilkårperiodeType,
    @Column("delvilkar")
    val delvilkår: DelvilkårVilkårperiode,
    val begrunnelse: String?,
    val resultat: ResultatVilkårperiode,
    val aktivitetsdager: Int?,

    val slettetKommentar: String? = null,

    val status: Vilkårstatus? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),

    val kildeId: String? = null,

) : Periode<LocalDate> {
    init {
        validatePeriode()
        validerAktivitetsdager()

        when {
            type is MålgruppeType && delvilkår is DelvilkårMålgruppe -> delvilkår.valider(begrunnelse)
            type is AktivitetType && delvilkår is DelvilkårAktivitet -> delvilkår.valider()
            else -> error("Ugyldig kombinasjon type=${type.javaClass.simpleName} detaljer=${delvilkår.javaClass.simpleName}")
        }

        validerPåkrevdBegrunnelse()
        validerSlettefelter()
    }

    private fun validerAktivitetsdager() {
        if (type is AktivitetType) {
            if (type == AktivitetType.INGEN_AKTIVITET) {
                brukerfeilHvis(aktivitetsdager != null) { "Kan ikke registrere aktivitetsdager på ingen aktivitet" }
            } else {
                brukerfeilHvis(aktivitetsdager !in 1..5) {
                    "Aktivitetsdager må være et heltall mellom 1 og 5"
                }
            }
        }

        if (type is MålgruppeType) {
            brukerfeilHvis(aktivitetsdager != null) { "Kan ikke registrere aktivitetsdager på målgruppe" }
        }
    }

    private fun validerPåkrevdBegrunnelse() {
        if (!begrunnelse.isNullOrBlank()) {
            return
        }
        when (type) {
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> "Mangler begrunnelse for nedsatt arbeidsevne"
            MålgruppeType.INGEN_MÅLGRUPPE -> "Mangler begrunnelse for ingen målgruppe"
            MålgruppeType.SYKEPENGER_100_PROSENT -> "Mangler begrunnelse for 100% sykepenger"
            AktivitetType.INGEN_AKTIVITET -> "Mangler begrunnelse for ingen aktivitet"
            else -> null
        }?.let { brukerfeil(it) }
    }

    private fun DelvilkårMålgruppe.valider(begrunnelse: String?) {
        brukerfeilHvis((medlemskap.svar != null && medlemskap.svar != SvarJaNei.JA_IMPLISITT) && begrunnelse.isNullOrBlank()) {
            "Mangler begrunnelse for vurdering av medlemskap"
        }

        brukerfeilHvis(dekketAvAnnetRegelverk.resultat == ResultatDelvilkårperiode.IKKE_OPPFYLT && manglerBegrunnelse()) {
            "Mangler begrunnelse for utgifter dekt av annet regelverk"
        }
    }

    private fun DelvilkårAktivitet.valider() {
        brukerfeilHvis(lønnet.resultat == ResultatDelvilkårperiode.IKKE_OPPFYLT && manglerBegrunnelse()) {
            "Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid"
        }
    }

    private fun manglerBegrunnelse() = begrunnelse.isNullOrBlank()

    private fun validerSlettefelter() {
        if (resultat == ResultatVilkårperiode.SLETTET) {
            feilHvis(kilde != KildeVilkårsperiode.MANUELL) {
                "Kan ikke slette når kilde=$kilde"
            }
            feilHvis(slettetKommentar.isNullOrBlank() && forrigeVilkårperiodeId != null) {
                "Mangler kommentar for resultat=$resultat"
            }
        } else {
            feilHvis(!slettetKommentar.isNullOrBlank()) {
                "Kan ikke ha slettetkommentar med resultat=$resultat"
            }
        }
    }

    fun kanSlettesPermanent() =
        this.forrigeVilkårperiodeId == null && this.kilde != KildeVilkårsperiode.SYSTEM

    fun kopierTilBehandling(nyBehandlingId: BehandlingId): Vilkårperiode {
        return copy(
            id = UUID.randomUUID(),
            behandlingId = nyBehandlingId,
            forrigeVilkårperiodeId = forrigeVilkårPeriodeIdForKopiertVilkår(),
            sporbar = Sporbar(),
            status = Vilkårstatus.UENDRET,
        )
    }

    /**
     * Liknende som [no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår.opprettOpphavsvilkår]
     */
    private fun forrigeVilkårPeriodeIdForKopiertVilkår(): UUID {
        return when (status) {
            Vilkårstatus.SLETTET -> error("Skal ikke kopiere vilkårperiode som er slettet")
            Vilkårstatus.UENDRET ->
                forrigeVilkårperiodeId
                    ?: error("Forventer at vilkårperiode med status=$status har forrigeVilkårperiodeId")

            null,
            Vilkårstatus.NY,
            Vilkårstatus.ENDRET,
            -> id
        }
    }
}

enum class KildeVilkårsperiode {
    MANUELL,
    SYSTEM,
}

enum class ResultatVilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    SLETTET,
}

@JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
sealed interface VilkårperiodeType {
    fun tilDbType(): String

    fun girIkkeRettPåStønadsperiode(): Boolean
}

val vilkårperiodetyper: Map<String, VilkårperiodeType> =
    listOf(MålgruppeType.entries, AktivitetType.entries).flatten().associateBy { it.name }

data class Vilkårperioder(
    val målgrupper: List<Vilkårperiode>,
    val aktiviteter: List<Vilkårperiode>,
)
