package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * En vilkårsvurdering per type [VilkårType].
 * For noen typer så er det per [VilkårType] og [barnId], hvor man må vurdere vilkåret per barn til søkeren
 *
 * Hver vilkårsvurdering har delvilkår. Hvert delvilkår har vurderinger med svar, og kanskje begrunnelse.
 *
 * Husk at [opphavsvilkår] må tas stilling til når man kopierer denne
 */
@Table("vilkar")
data class Vilkår(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    val type: VilkårType,
    val barnId: UUID? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("delvilkar")
    val delvilkårwrapper: DelvilkårWrapper,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "opphavsvilkaar_")
    val opphavsvilkår: Opphavsvilkår?,
) {
    val delvilkårsett get() = delvilkårwrapper.delvilkårsett

    init {
        require(resultat.erIkkeDelvilkårsresultat()) // Verdien AUTOMATISK_OPPFYLT er kun forbeholdt delvilkår
    }

    /**
     * Brukes når man skal gjenbruke denne vilkårsvurderingen i en annan vilkårsvurdering
     */
    fun opprettOpphavsvilkår(): Opphavsvilkår =
        opphavsvilkår ?: Opphavsvilkår(behandlingId, sporbar.endret.endretTid)
}

fun List<Vilkår>.utledVurderinger(vilkårType: VilkårType, regelId: RegelId) =
    this.filter { it.type == vilkårType }.flatMap { it.delvilkårsett }
        .flatMap { it.vurderinger }
        .filter { it.regelId == regelId }

/**
 * Inneholder informasjon fra hvilken behandling dette vilkår ble gjenrukt fra
 * Hvis man gjenbruker et vilkår som allerede er gjenbrukt fra en annen behandling,
 * så skal man peke til den opprinnelige behandlingen. Dvs
 * Behandling A
 * Behandling B gjenbruker fra behandling A
 * Behandling C gjenbruker fra B, men peker mot A sitt vilkår
 */
data class Opphavsvilkår(
    val behandlingId: UUID,
    val vurderingstidspunkt: LocalDateTime,
)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårWrapper(val delvilkårsett: List<Delvilkår>)

data class Delvilkår(
    val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    val vurderinger: List<Vurdering>,
) {

    // regelId for første svaret er det samme som hovedregel
    val hovedregel = vurderinger.first().regelId
}

data class Vurdering(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

fun List<Vurdering>.harSvar(svarId: SvarId) = this.any { it.svar == svarId }

enum class Vilkårsresultat(val beskrivelse: String) {
    OPPFYLT("Vilkåret er oppfylt når alle delvilkår er oppfylte"),
    AUTOMATISK_OPPFYLT("Delvilkår er oppfylt med automatisk beregning"),
    IKKE_OPPFYLT("Vilkåret er ikke oppfylt hvis alle delvilkår er oppfylt eller ikke oppfylt, men minimum 1 ikke oppfylt"),
    IKKE_AKTUELL("Hvis søknaden/pdl data inneholder noe som gjør att delvilkåret ikke må besvares"),
    IKKE_TATT_STILLING_TIL("Init state, eller att brukeren ikke svaret på hele delvilkåret"),
    SKAL_IKKE_VURDERES("Saksbehandleren kan sette att ett delvilkår ikke skal vurderes"),
    ;

    fun oppfyltEllerIkkeOppfylt() = this == OPPFYLT || this == IKKE_OPPFYLT
    fun erIkkeDelvilkårsresultat() = this != AUTOMATISK_OPPFYLT
}

/**
 * @param gjelderStønader er for stønadsspesifike regler
 */
enum class VilkårType(val beskrivelse: String, val gjelderStønader: List<Stønadstype>) {
    EKSEMPEL("Eksempel", listOf()),
    EKSEMPEL2("Eksempel 2", listOf()),
    MÅLGRUPPE("Målgruppe", listOf(Stønadstype.BARNETILSYN)),
    AKTIVITET("Aktivitet", listOf(Stønadstype.BARNETILSYN)),

    // Barnetilsyn
    PASS_BARN("Pass av barn", listOf(Stønadstype.BARNETILSYN)),

    // Generelle regler for alle stønader
    // Målgrupper
    MÅLGRUPPE_AAP("AAP", listOf()),
    MÅLGRUPPE_AAP_FERDIG_AVKLART("AAP Ferdig avklart", listOf()),
    MÅLGRUPPE_DAGPENGER("Dagpenger", listOf()),
    MÅLGRUPPE_OMSTILLINGSSTØNAD("Omstillingsstønad", listOf()),
    MÅLGRUPPE_OVERGANGSSTØNAD("Rett til overgangsstønad", listOf()),
    MÅLGRUPPE_UFØRETRYGD("Uføretrygd", listOf()),

    // Aktiviteter
    AKTIVITET_TILTAK("Tiltak", listOf()),
    AKTIVITET_UTDANNING("Utdanning", listOf()),
    AKTIVITET_REEL_ARBEIDSSSØKER("Reel arbeidssøker", listOf()),
    ;

    fun gjelderFlereBarn(): Boolean = this == PASS_BARN

    fun gjelderMålgruppe(): Boolean = setOf(
        MÅLGRUPPE_AAP,
        MÅLGRUPPE_AAP_FERDIG_AVKLART,
        MÅLGRUPPE_DAGPENGER,
        MÅLGRUPPE_OMSTILLINGSSTØNAD,
        MÅLGRUPPE_OVERGANGSSTØNAD,
        MÅLGRUPPE_UFØRETRYGD,
    ).contains(this)

    fun gjelderAktivitet(): Boolean = setOf(
        AKTIVITET_TILTAK,
        AKTIVITET_UTDANNING,
        AKTIVITET_REEL_ARBEIDSSSØKER,
    ).contains(this)

    fun gjelderMålgruppeEllerAktivitet(): Boolean = gjelderMålgruppe() || gjelderAktivitet()

    companion object {

        fun hentVilkårForStønad(stønadstype: Stønadstype): List<VilkårType> = entries.filter {
            it.gjelderStønader.contains(stønadstype)
        }
    }
}
