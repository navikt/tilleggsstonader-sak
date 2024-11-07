package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.TakeIfUtil.takeIfType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårFaktaMapper.mapTilVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.VilkårperiodeTypeDeserializer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.InsertOnlyProperty
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

interface IVilkårperiode<FAKTA_VURDERING : FaktaOgVurdering> : Periode<LocalDate> {
    val faktaOgVurdering: FAKTA_VURDERING
}

/**
 * @param type er ikke redigerbar men settes inn her for å kunne valideres sammen med de andre delene i [VilkårOgFakta]
 */
data class VilkårOgFakta(
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val begrunnelse: String?,
    @Column("delvilkar")
    val delvilkår: DelvilkårVilkårperiode,
    val aktivitetsdager: Int?,
)

typealias Vilkårperiode = GeneriskVilkårperiode<out FaktaOgVurdering>

/**
 *
 */
@Table("vilkar_periode")
data class GeneriskVilkårperiode<T : FaktaOgVurdering>(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    @Column("forrige_vilkarperiode_id")
    val forrigeVilkårperiodeId: UUID? = null,

    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val faktaOgVurdering: T,
    val begrunnelse: String?,

    val resultat: ResultatVilkårperiode,
    val slettetKommentar: String? = null,
    val status: Vilkårstatus? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),

    val kildeId: String? = null,

    // TODO kilde burde kunne fjernes, den brukes aldri til noe annet enn manuell. Må fjernes i frontend og.
    @InsertOnlyProperty
    val kilde: KildeVilkårsperiode = KildeVilkårsperiode.MANUELL,
) : IVilkårperiode<T> {
    init {
        // TODO valider kombinasjon av type og type og FaktaOgVurdering.type
        validatePeriode()
        validerSlettefelter()

        validerBegrunnelse()

        feilHvis(faktaOgVurdering.type.vilkårperiodeType != type) {
            "Ugyldig kombinasjon - type($type) må være lik faktaOgVurdering($faktaOgVurdering)"
        }

        // TODO endre aktivitetsdager til value class og legg inn sjekk der
        faktaOgVurdering.vurderinger.takeIfType<FaktaAktivitetsdager>()?.let {
            brukerfeilHvis(it.aktivitetsdager !in 1..5) {
                "Aktivitetsdager må være et heltall mellom 1 og 5"
            }
        }
    }

    private fun validerBegrunnelse() {
        if (!manglerBegrunnelse()) {
            return
        }
        validerPåkrevdBegrunnelseForType()
        // TODO burde vi ha en valideringsmetode i vurderinger som tar inn begrunnelse?
        faktaOgVurdering.vurderinger.takeIfType<LønnetVurdering>()?.let {
            brukerfeilHvis(it.lønnet.resultat == ResultatDelvilkårperiode.IKKE_OPPFYLT && manglerBegrunnelse()) {
                "Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid"
            }
        }

        faktaOgVurdering.vurderinger.takeIfType<DekketAvAnnetRegelverkVurdering>()?.let {
            brukerfeilHvis(it.dekketAvAnnetRegelverk.resultat == ResultatDelvilkårperiode.IKKE_OPPFYLT) {
                "Mangler begrunnelse for utgifter dekt av annet regelverk"
            }
        }
        faktaOgVurdering.vurderinger.takeIfType<MedlemskapVurdering>()?.let {
            brukerfeilHvis(it.medlemskap.svar?.harVurdert() == true) {
                "Mangler begrunnelse for vurdering av medlemskap"
            }
        }
    }

    private fun manglerBegrunnelse() = begrunnelse.isNullOrBlank()

    private fun validerPåkrevdBegrunnelseForType() {
        when (type) {
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> "Mangler begrunnelse for nedsatt arbeidsevne"
            MålgruppeType.INGEN_MÅLGRUPPE -> "Mangler begrunnelse for ingen målgruppe"
            MålgruppeType.SYKEPENGER_100_PROSENT -> "Mangler begrunnelse for 100% sykepenger"
            AktivitetType.INGEN_AKTIVITET -> "Mangler begrunnelse for ingen aktivitet"
            else -> null
        }?.let { brukerfeil(it) }
    }

    val vilkårOgFakta: VilkårOgFakta by lazy { mapTilVilkårFakta() }

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

    fun kopierTilBehandling(nyBehandlingId: BehandlingId): GeneriskVilkårperiode<T> {
        return copy(
            id = UUID.randomUUID(),
            behandlingId = nyBehandlingId,
            forrigeVilkårperiodeId = forrigeVilkårPeriodeIdForKopiertVilkår(),
            sporbar = Sporbar(),
            status = Vilkårstatus.UENDRET,
        )
    }

    fun medVilkårOgVurdering(
        fom: LocalDate,
        tom: LocalDate,
        begrunnelse: String?,
        faktaOgVurdering: FaktaOgVurdering,
        resultat: ResultatVilkårperiode,
    ): GeneriskVilkårperiode<T> {
        val nyStatus = if (status == Vilkårstatus.NY) {
            Vilkårstatus.NY
        } else {
            Vilkårstatus.ENDRET
        }
        @Suppress("UNCHECKED_CAST")
        return this.copy(
            faktaOgVurdering = faktaOgVurdering as T,
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
