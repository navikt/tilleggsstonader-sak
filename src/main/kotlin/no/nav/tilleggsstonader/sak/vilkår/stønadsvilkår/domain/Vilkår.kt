package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

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
    val id: VilkårId = VilkårId.random(),
    val behandlingId: BehandlingId,
    val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    val type: VilkårType,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val utgift: Int? = null,

    val barnId: BarnId? = null,
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
        if (fom != null || tom != null) {
            require(fom != null) { "Krever at fom er satt hvis tom er satt" }
            require(tom != null) { "Krever at tom er satt hvis fom er satt" }
            require(fom <= tom) { "Krever at fom <= tom" }
            require(utgift == null || utgift >= 0) { "Utgift må være positivt tall" }

            validerDataForType(fom, tom)
        }
    }

    private fun validerDataForType(fom: LocalDate, tom: LocalDate) {
        when (type) {
            VilkårType.PASS_BARN -> {
                validerFørsteOgSisteDagIValgtMåned(fom, tom)
                validerPåkrevdBeløpHvisOppfylt()
            }
            VilkårType.EKSEMPEL -> {
                // Dette er kun for tester foreløpig
            }
            else -> error("Må ta stilling til validering av fom/tom. Eks om vilkåret bruker dato eller månedsvelger")
        }
    }

    private fun validerPåkrevdBeløpHvisOppfylt() {
        if (resultat == Vilkårsresultat.OPPFYLT) {
            require(utgift != null) { "Utgift er påkrevd når resultat er oppfylt" }
        }
    }

    private fun validerFørsteOgSisteDagIValgtMåned(fom: LocalDate, tom: LocalDate) {
        require(fom.erFørsteDagIMåneden()) {
            "For vilkår=$type skal FOM være første dagen i måneden"
        }
        require(tom.erSisteDagIMåneden()) {
            "For vilkår=$type skal TOM være siste dagen i måneden"
        }
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
    val behandlingId: BehandlingId,
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

    // Barnetilsyn
    PASS_BARN("Pass av barn", listOf(Stønadstype.BARNETILSYN)),
    ;

    fun gjelderFlereBarn(): Boolean = this == PASS_BARN

    companion object {

        fun hentVilkårForStønad(stønadstype: Stønadstype): List<VilkårType> = entries.filter {
            it.gjelderStønader.contains(stønadstype)
        }
    }
}
