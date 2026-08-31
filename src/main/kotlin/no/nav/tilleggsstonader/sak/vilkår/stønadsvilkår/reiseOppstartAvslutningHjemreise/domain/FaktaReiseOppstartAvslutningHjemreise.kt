package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal

sealed interface FaktaReiseOppstartAvslutningHjemreise {
    val type: TypeReiseOppstartAvslutningHjemreise
    val typeReiseformål: TypeReiseformål
    val reiseId: ReiseId
    val adresse: String?

    fun mapTilVilkårFakta(): VilkårFakta
}

data class FaktaUbestemtType(
    override val typeReiseformål: TypeReiseformål,
    override val reiseId: ReiseId,
    override val adresse: String?,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreiseUbestemt(
            reiseId = reiseId,
            adresse = adresse,
            typeReiseformål = typeReiseformål,
        )
}

data class FaktaOffentligTransport(
    override val typeReiseformål: TypeReiseformål,
    override val reiseId: ReiseId,
    override val adresse: String?,
    val utgifterOffentligTransport: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
            reiseId = reiseId,
            adresse = adresse,
            typeReiseformål = typeReiseformål,
            utgifterOffentligTransport = utgifterOffentligTransport,
            aktivitetId = aktivitetId,
        )

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(utgifterOffentligTransport <= 0.toBigDecimal()) {
            "Utgifter til offentlig transport kan ikke være negative"
        }
    }
}

data class FaktaPrivatBil(
    override val typeReiseformål: TypeReiseformål,
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reiseavstand: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

    init {
        validerIngenNegativReiseavstand()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseOppstartAvslutningHjemreisePrivatBil(
            reiseId = reiseId,
            adresse = adresse,
            typeReiseformål = typeReiseformål,
            reiseavstand = reiseavstand,
            aktivitetId = aktivitetId,
        )

    private fun validerIngenNegativReiseavstand() {
        reiseavstand.let {
            brukerfeilHvis(it <= 0.toBigDecimal()) {
                "Reiseavstand kan ikke være negativ"
            }
        }
    }
}
