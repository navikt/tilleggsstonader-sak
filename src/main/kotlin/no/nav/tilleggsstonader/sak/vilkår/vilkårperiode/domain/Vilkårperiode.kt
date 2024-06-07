package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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
    val behandlingId: UUID,
    val kilde: KildeVilkårsperiode,

    val fom: LocalDate,
    val tom: LocalDate,
    val type: VilkårperiodeType,
    @Column("delvilkar")
    val delvilkår: DelvilkårVilkårperiode,
    val begrunnelse: String?,
    val resultat: ResultatVilkårperiode,
    val aktivitetsdager: Int?,

    val slettetKommentar: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }

        validerAktivitetsdager()

        when {
            type is MålgruppeType && delvilkår is DelvilkårMålgruppe -> delvilkår.valider(begrunnelse)
            type is AktivitetType && delvilkår is DelvilkårAktivitet -> delvilkår.valider()
            else -> error("Ugyldig kombinasjon type=${type.javaClass.simpleName} detaljer=${delvilkår.javaClass.simpleName}")
        }

        validerBegrunnelseNedsattArbeidsevne()
        validerBegrunnelseIngenAktivitetEllerMålgruppe()

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

    private fun validerBegrunnelseNedsattArbeidsevne() {
        if (type == MålgruppeType.NEDSATT_ARBEIDSEVNE) {
            brukerfeilHvis(begrunnelse.isNullOrBlank()) {
                "Mangler begrunnelse for nedsatt arbeidsevne"
            }
        }
    }

    private fun validerBegrunnelseIngenAktivitetEllerMålgruppe() {
        if (type == AktivitetType.INGEN_AKTIVITET) {
            brukerfeilHvis(begrunnelse.isNullOrBlank()) {
                "Mangler begrunnelse for ingen aktivitet"
            }
        }

        if (type == MålgruppeType.INGEN_MÅLGRUPPE) {
            brukerfeilHvis(begrunnelse.isNullOrBlank()) {
                "Mangler begrunnelse for ingen målgruppe"
            }
        }
        if (type == MålgruppeType.SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING) {
            brukerfeilHvis(begrunnelse.isNullOrBlank()) {
                "Mangler begrunnelse for 100% sykepenger"
            }
        }
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
            feilHvis(slettetKommentar.isNullOrBlank()) {
                "Mangler kommentar for resultat=$resultat"
            }
        } else {
            feilHvis(!slettetKommentar.isNullOrBlank()) {
                "Kan ikke ha slettetkommentar med resultat=$resultat"
            }
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppe::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitet::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiode {
    data class Vurdering(
        val svar: SvarJaNei?,
        val resultat: ResultatDelvilkårperiode,
    ) {
        init {
            feilHvis(resultat == ResultatDelvilkårperiode.IKKE_AKTUELT && (svar != null)) {
                "Ugyldig resultat=$resultat når svar=$svar"
            }
        }
    }
}

enum class ResultatDelvilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    IKKE_AKTUELT,
}

data class DelvilkårMålgruppe(
    val medlemskap: Vurdering,
    val dekketAvAnnetRegelverk: Vurdering,
) : DelvilkårVilkårperiode()

data class DelvilkårAktivitet(
    val lønnet: Vurdering,
) : DelvilkårVilkårperiode()

enum class SvarJaNei {
    JA,
    JA_IMPLISITT,
    NEI,
}

sealed interface VilkårperiodeType {
    fun tilDbType(): String

    fun girIkkeRettPåStønadsperiode(): Boolean
}

enum class MålgruppeType(val gyldigeAktiviter: Set<AktivitetType>) : VilkårperiodeType {
    AAP(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    DAGPENGER(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    OMSTILLINGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    OVERGANGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    NEDSATT_ARBEIDSEVNE(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    UFØRETRYGD(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING(emptySet()),
    INGEN_MÅLGRUPPE(emptySet()),
    ;

    override fun tilDbType(): String = this.name

    fun gjelderNedsattArbeidsevne() = this == NEDSATT_ARBEIDSEVNE || this == UFØRETRYGD || this == AAP

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_MÅLGRUPPE ||
            this == SYKEPENGER_100_PROSENT_FOR_FULLTIDSSTILLING
}

enum class AktivitetType : VilkårperiodeType {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    INGEN_AKTIVITET,
    ;

    override fun tilDbType(): String = this.name

    override fun girIkkeRettPåStønadsperiode() =
        this == INGEN_AKTIVITET
}

val vilkårperiodetyper: Map<String, VilkårperiodeType> =
    listOf(MålgruppeType.entries, AktivitetType.entries).flatten().associateBy { it.name }

data class Vilkårperioder(
    val målgrupper: List<Vilkårperiode>,
    val aktiviteter: List<Vilkårperiode>,
)

data class Aktivitet(
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitetsdager: Int,
) : Periode<LocalDate>

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> {
    return this.mapNotNull {
        if (it.type is AktivitetType) {
            Aktivitet(
                type = it.type,
                fom = it.fom,
                tom = it.tom,
                aktivitetsdager = it.aktivitetsdager ?: error("Aktivitetsdager mangler på periode ${it.id}"),
            )
        } else {
            null
        }
    }
}
