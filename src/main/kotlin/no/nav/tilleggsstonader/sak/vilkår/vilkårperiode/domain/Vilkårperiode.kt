package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.mapFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.VilkårperiodeTypeDeserializer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.InsertOnlyProperty
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

sealed interface VilkårperiodeSI<FAKTA_VURDERING : FaktaOgVurdering> : Periode<LocalDate> {
    val faktaOgVurderingTypet: FAKTA_VURDERING

    override val fom: LocalDate get() = faktaOgVurderingTypet.fom
    override val tom: LocalDate get() = faktaOgVurderingTypet.tom
}

/**
 * @param type er ikke redigerbar men settes inn her for å kunne valideres sammen med de andre delene i [VilkårOgFakta]
 */
data class VilkårOgFakta(
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val begrunnelse: String?,
    @Column("delvilkar")
    val delvilkår: DelvilkårVilkårperiode,
    val aktivitetsdager: Int?,
) : Periode<LocalDate> {
    init {
        // validerPeriode? Burde Periode brukes her eller på Vilkårsvurdering?
        validerAktivitetsdager()

        when {
            type is MålgruppeType && delvilkår is DelvilkårMålgruppe -> delvilkår.valider(begrunnelse)
            type is AktivitetType && delvilkår is DelvilkårAktivitet -> delvilkår.valider()
            else -> error("Ugyldig kombinasjon type=${type.javaClass.simpleName} detaljer=${delvilkår.javaClass.simpleName}")
        }

        validerPåkrevdBegrunnelse()
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
}

typealias Vilkårperiode = VilkårperiodeOld<FaktaOgVurdering>

inline fun <reified T : FaktaOgVurdering> List<VilkårperiodeOld<*>>.ofType(): List<VilkårperiodeOld<T>> {
    @Suppress("UNCHECKED_CAST")
    return this.filter { it.faktaOgVurderingTypet is T } as List<VilkårperiodeOld<T>>
}

fun main() {
    val vilkårOgFakta = VilkårOgFakta(
        type = AktivitetType.TILTAK,
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        begrunnelse = "",
        delvilkår = DelvilkårAktivitet(
            lønnet = DelvilkårVilkårperiode.Vurdering(SvarJaNei.NEI, resultat = ResultatDelvilkårperiode.OPPFYLT),
        ),
        aktivitetsdager = 5,
    )
    val list = listOf(
        VilkårperiodeOld<FaktaOgVurdering>(
            id = UUID.randomUUID(),
            behandlingId = BehandlingId.random(),
            resultat = ResultatVilkårperiode.IKKE_VURDERT,
            vilkårOgFakta = vilkårOgFakta,
        ),
    )

    println(list.ofType<TiltakTilsynBarn>())
    println(list.ofType<UtdanningTilsynBarn>())
    println(list.ofType<MålgruppeTilsynBarn>())
}

@Table("vilkar_periode")
data class VilkårperiodeOld<T : FaktaOgVurdering>(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    @Column("forrige_vilkarperiode_id")
    val forrigeVilkårperiodeId: UUID? = null,

    val resultat: ResultatVilkårperiode,

    val slettetKommentar: String? = null,

    val status: Vilkårstatus? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val vilkårOgFakta: VilkårOgFakta,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),

    val kildeId: String? = null,

    // TODO kilde burde kunne fjernes, den brukes aldri til noe annet enn manuell. Må fjernes i frontend og.
    @InsertOnlyProperty
    val kilde: KildeVilkårsperiode = KildeVilkårsperiode.MANUELL,
) : VilkårperiodeSI<T> {
    init {
        validatePeriode()
        validerSlettefelter()
    }

    override val fom: LocalDate get() = this.vilkårOgFakta.fom
    override val tom: LocalDate get() = this.vilkårOgFakta.tom
    val type: VilkårperiodeType get() = this.vilkårOgFakta.type

    @Suppress("UNCHECKED_CAST")
    override val faktaOgVurderingTypet: T by lazy { mapFaktaOgVurdering(this) as T }

    private fun validerSlettefelter() {
        if (resultat == ResultatVilkårperiode.SLETTET) {
            feilHvis(slettetKommentar.isNullOrBlank() && forrigeVilkårperiodeId != null) {
                "Mangler kommentar for resultat=$resultat"
            }
        } else {
            feilHvis(!slettetKommentar.isNullOrBlank()) {
                "Kan ikke ha slettetkommentar med resultat=$resultat"
            }
        }
    }

    fun kanSlettesPermanent() = this.forrigeVilkårperiodeId == null

    fun markerSlettet(slettetKommentar: String?) = this.copy(
        resultat = ResultatVilkårperiode.SLETTET,
        slettetKommentar = slettetKommentar,
        status = Vilkårstatus.SLETTET,
    )

    fun kopierTilBehandling(nyBehandlingId: BehandlingId): VilkårperiodeOld<T> {
        return copy(
            id = UUID.randomUUID(),
            behandlingId = nyBehandlingId,
            forrigeVilkårperiodeId = forrigeVilkårPeriodeIdForKopiertVilkår(),
            sporbar = Sporbar(),
            status = Vilkårstatus.UENDRET,
        )
    }

    fun medVilkårOgVurdering(vilkårOgFakta: VilkårOgFakta, resultat: ResultatVilkårperiode): VilkårperiodeOld<T> {
        val nyStatus = if (status == Vilkårstatus.NY) {
            Vilkårstatus.NY
        } else {
            Vilkårstatus.ENDRET
        }
        return this.copy(
            vilkårOgFakta = vilkårOgFakta,
            status = nyStatus,
            resultat = resultat,
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
