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

    fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ): VilkårFakta
}

data object FaktaUbestemtType : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaReiseOppstartAvslutningHjemreiseUbestemt(
        reiseId = reiseId,
        typeReiseformål = typeReiseformål,
        aktivitetId = aktivitetId,
    )
}

data class FaktaOffentligTransport(
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
    }

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
        reiseId = reiseId,
        typeReiseformål = typeReiseformål,
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
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : FaktaReiseOppstartAvslutningHjemreise {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

    init {
        validerIngenNegativReiseavstand()
        validerIngenNegativeEkstrakostnader()
    }

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaReiseOppstartAvslutningHjemreisePrivatBil(
        reiseId = reiseId,
        typeReiseformål = typeReiseformål,
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
