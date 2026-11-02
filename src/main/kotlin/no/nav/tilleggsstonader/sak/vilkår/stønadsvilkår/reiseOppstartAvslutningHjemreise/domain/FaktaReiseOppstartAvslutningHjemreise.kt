package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal

sealed interface FaktaReiseOppstartAvslutningHjemreise {
    val type: TypeReiseOppstartAvslutningHjemreise
    val aktivitetId: VilkårperiodeGlobalId

    fun mapTilVilkårFakta(): VilkårFakta
}

sealed interface ReiseFaktaROAH : FaktaReiseOppstartAvslutningHjemreise {
    val reiseId: ReiseId
    val typeReiseformål: TypeReiseformål
}

data class FaktaMidlertidigBostedPeriode(
    override val aktivitetId: VilkårperiodeGlobalId,
    val midlertidigAdresse: String,
    val hjemstedAdresse: String,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.MIDLERTIDIG_BOSTED_PERIODE

    init {
        brukerfeilHvis(midlertidigAdresse.isBlank()) { "Midlertidig adresse må fylles ut" }
        brukerfeilHvis(hjemstedAdresse.isBlank()) { "Hjemstedsadresse må fylles ut" }
    }

    override fun mapTilVilkårFakta() = FaktaROAHMidlertidigBostedPeriode(aktivitetId, midlertidigAdresse, hjemstedAdresse)

    override fun toString() = "FaktaMidlertidigBostedPeriode(aktivitetId=$aktivitetId, midlertidigAdresse=***, hjemstedAdresse=***)"
}

data class FaktaUbestemtType(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
) : ReiseFaktaROAH {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreiseUbestemt(
            reiseId = reiseId,
            typeReiseformål = typeReiseformål,
            aktivitetId = aktivitetId,
        )
}

data class FaktaOffentligTransport(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
    val nødvendigUtgift: NødvendigUtgift,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : ReiseFaktaROAH {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
            reiseId = reiseId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = nødvendigUtgift,
            utgifterOffentligTransport = utgifterOffentligTransport,
            aktivitetId = aktivitetId,
            begrunnelse = begrunnelse,
        )

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(utgifterOffentligTransport <= 0.toBigDecimal()) {
            "Utgifter til offentlig transport kan ikke være negative"
        }
    }
}

data class FaktaPrivatBil(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
    val nødvendigUtgift: NødvendigUtgift,
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : ReiseFaktaROAH {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

    init {
        validerIngenNegativReiseavstand()
        validerIngenNegativeEkstrakostnader()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreisePrivatBil(
            reiseId = reiseId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = nødvendigUtgift,
            reiseavstand = reiseavstand,
            aktivitetId = aktivitetId,
            bompenger = bompenger,
            fergekostnad = fergekostnad,
            parkering = parkering,
            piggdekkavgift = piggdekkavgift,
            begrunnelse = begrunnelse,
        )

    private fun validerIngenNegativReiseavstand() {
        reiseavstand.let {
            brukerfeilHvis(it <= 0.toBigDecimal()) {
                "Reiseavstand kan ikke være negativ"
            }
        }
    }

    private fun validerIngenNegativeEkstrakostnader() {
        brukerfeilHvis(bompenger != null && bompenger < BigDecimal.ZERO) {
            "Bompenger må være mer enn 0kr"
        }
        brukerfeilHvis(fergekostnad != null && fergekostnad < BigDecimal.ZERO) {
            "Fergekostnad må være mer enn 0kr"
        }
        brukerfeilHvis(parkering != null && parkering < BigDecimal.ZERO) {
            "Parkering må være mer enn 0kr"
        }
        brukerfeilHvis(piggdekkavgift != null && piggdekkavgift < BigDecimal.ZERO) {
            "Piggdekkavgift må være mer enn 0kr"
        }
    }
}
