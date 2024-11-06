package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.osloNow
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
import java.util.UUID

typealias MålgruppeEllerAktivitet = TypeVilkårperiode<*, *>

sealed interface TypeVilkårperiode<Type, TypeDelvilkår> : Periode<LocalDate>
        where Type : VilkårperiodeType,
              TypeDelvilkår : DelvilkårVilkårperiode {

    val id: UUID
    val behandlingId: BehandlingId
    val kilde: KildeVilkårsperiode
    val forrigeVilkårperiodeId: UUID?

    override val fom: LocalDate
    override val tom: LocalDate

    val type: Type
    val delvilkår: TypeDelvilkår

    val begrunnelse: String?
    val resultat: ResultatVilkårperiode
    val aktivitetsdager: Int?

    val slettetKommentar: String?
    val status: Vilkårstatus?
    val sporbar: Sporbar

    val kildeId: String?

    fun medNyttDato(fom: LocalDate, tom: LocalDate): TypeVilkårperiode<Type, TypeDelvilkår>
    fun kopierTilBehandling(nyBehandlingId: BehandlingId): TypeVilkårperiode<Type, TypeDelvilkår>

    fun oppdater(
        begrunnelse: String?,
        fom: LocalDate,
        tom: LocalDate,
        delvilkår: TypeDelvilkår,
        aktivitetsdager: Int?,
        resultat: ResultatVilkårperiode,
        status: Vilkårstatus
    ): TypeVilkårperiode<Type, TypeDelvilkår>

    fun kanSlettesPermanent(): Boolean
    fun markerSlettet(slettetKommentar: String?): TypeVilkårperiode<Type, TypeDelvilkår>
}

typealias VilkårperiodeMålgruppe = TypeVilkårperiode<MålgruppeType, DelvilkårMålgruppe>
typealias VilkårperiodeAktivitet = TypeVilkårperiode<AktivitetType, DelvilkårAktivitet>

fun TypeVilkårperiode<*, *>.ifMålgruppe(): VilkårperiodeMålgruppe? {
    if (this.type is MålgruppeType && this.delvilkår is DelvilkårMålgruppe) {
        @Suppress("UNCHECKED_CAST")
        return this as VilkårperiodeMålgruppe
    }
    return null
}

fun TypeVilkårperiode<*, *>.ifAktivitet(): VilkårperiodeAktivitet? {
    if (this.type is AktivitetType && this.delvilkår is DelvilkårAktivitet) {
        @Suppress("UNCHECKED_CAST")
        return this as VilkårperiodeAktivitet
    }
    return null
}

@Table("vilkar_periode")
data class Vilkårperiode(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val behandlingId: BehandlingId,
    override val kilde: KildeVilkårsperiode,
    @Column("forrige_vilkarperiode_id")
    override val forrigeVilkårperiodeId: UUID? = null,

    override val fom: LocalDate,
    override val tom: LocalDate,
    override val type: VilkårperiodeType,
    @Column("delvilkar")
    override val delvilkår: DelvilkårVilkårperiode,
    override val begrunnelse: String?,
    override val resultat: ResultatVilkårperiode,
    override val aktivitetsdager: Int?,

    override val slettetKommentar: String? = null,

    override val status: Vilkårstatus? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    override val sporbar: Sporbar = Sporbar(),

    override val kildeId: String? = null,

    ) : Periode<LocalDate>, TypeVilkårperiode<VilkårperiodeType, DelvilkårVilkårperiode> {

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

    override fun medNyttDato(
        fom: LocalDate,
        tom: LocalDate
    ): TypeVilkårperiode<VilkårperiodeType, DelvilkårVilkårperiode> {
        return this.copy(fom = fom, tom = tom)
    }

    override fun oppdater(
        begrunnelse: String?,
        fom: LocalDate,
        tom: LocalDate,
        delvilkår: DelvilkårVilkårperiode,
        aktivitetsdager: Int?,
        resultat: ResultatVilkårperiode,
        status: Vilkårstatus
    ): Vilkårperiode {
        return this.copy(
            begrunnelse = begrunnelse,
            fom = fom,
            tom = tom,
            delvilkår = delvilkår,
            aktivitetsdager = aktivitetsdager,
            resultat = resultat,
            status = status
        )
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

    override fun kanSlettesPermanent() =
        this.forrigeVilkårperiodeId == null && this.kilde != KildeVilkårsperiode.SYSTEM

    override fun markerSlettet(slettetKommentar: String?): Vilkårperiode {
        return this.copy(
            resultat = ResultatVilkårperiode.SLETTET,
            slettetKommentar = slettetKommentar,
            status = Vilkårstatus.SLETTET,
        )
    }

    override fun kopierTilBehandling(nyBehandlingId: BehandlingId): Vilkårperiode {
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
    val målgrupper: List<VilkårperiodeMålgruppe>,
    val aktiviteter: List<VilkårperiodeAktivitet>,
)
