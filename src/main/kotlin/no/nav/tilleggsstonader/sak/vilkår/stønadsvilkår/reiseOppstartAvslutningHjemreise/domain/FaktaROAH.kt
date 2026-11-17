package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal

enum class TypeFaktaROAH {
    OFFENTLIG_TRANSPORT,
    PRIVAT_BIL,
    MIDLERTIDIG_BOSTED,
    UBESTEMT,
}

sealed interface FaktaROAH {
    val type: TypeFaktaROAH
}

sealed interface FaktaReiseROAH : FaktaROAH {
    fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ): VilkårFakta
}

data object FaktaUbestemtType : FaktaReiseROAH {
    override val type = TypeFaktaROAH.UBESTEMT

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaROAHUbestemt(
        reiseId = reiseId,
        typeReiseformål = typeReiseformål,
        aktivitetId = aktivitetId,
    )
}

data class FaktaOffentligTransport(
    val nødvendigUtgift: NødvendigUtgift,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : FaktaReiseROAH {
    override val type = TypeFaktaROAH.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
    }

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaROAHOffentligTransport(
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
    val nødvendigUtgift: NødvendigUtgift,
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : FaktaReiseROAH {
    override val type = TypeFaktaROAH.PRIVAT_BIL

    init {
        validerIngenNegativReiseavstand()
        validerIngenNegativeEkstrakostnader()
    }

    override fun mapTilVilkårFakta(
        reiseId: ReiseId,
        aktivitetId: VilkårperiodeGlobalId,
        typeReiseformål: TypeReiseformål,
    ) = FaktaROAHPrivatBil(
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

data class FaktaMidlertidigBosted(
    val fraAdresse: String,
    val tilAdresse: String,
) : FaktaROAH {
    override val type = TypeFaktaROAH.MIDLERTIDIG_BOSTED

    init {
        brukerfeilHvis(fraAdresse.isBlank()) { "Fra adresse må fylles ut" }
        brukerfeilHvis(tilAdresse.isBlank()) { "Til adresse må fylles ut" }
    }

    fun mapTilVilkårFakta(aktivitetId: VilkårperiodeGlobalId) =
        FaktaROAHMidlertidigBosted(
            aktivitetId = aktivitetId,
            fraAdresse = fraAdresse,
            tilAdresse = tilAdresse,
        )
}
