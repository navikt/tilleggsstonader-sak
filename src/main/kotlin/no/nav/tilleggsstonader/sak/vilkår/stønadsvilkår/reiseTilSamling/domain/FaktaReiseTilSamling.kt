package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import java.math.BigDecimal

sealed interface FaktaReiseTilSamling {
    val type: TypeReiseTilSamling
    val reiseId: ReiseId
    val adresse: String?

    fun mapTilVilkårFakta(): VilkårFakta
}

data class FaktaUbestemtType(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : FaktaReiseTilSamling {
    override val type = TypeReiseTilSamling.UBESTEMT

    override fun mapTilVilkårFakta() =
        FaktaReiseTilSamlingUbestemt(
            reiseId = reiseId,
            adresse = adresse,
        )
}

data class FaktaOffentligTransport(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val utgifterOffentligTransport: Int?,
) : FaktaReiseTilSamling {
    override val type = TypeReiseTilSamling.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseTilSamlingOffentligTransport(
            reiseId = reiseId,
            adresse = adresse,
            utgifterOffentligTransport = utgifterOffentligTransport,
        )

    private fun validerIngenNegativeUtgifter() {
        utgifterOffentligTransport?.let {
            brukerfeilHvis(it <= 0) {
                "Utgifter til offentlig transport kan ikke være negative"
            }
        }
    }
}

data class FaktaPrivatBil(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reiseavstand: BigDecimal?,
) : FaktaReiseTilSamling {
    override val type = TypeReiseTilSamling.PRIVAT_BIL

    init {
        validerIngenNegativReiseavstand()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseTilSamlingPrivatBil(
            reiseId = reiseId,
            adresse = adresse,
            reiseavstand = reiseavstand,
        )

    private fun validerIngenNegativReiseavstand() {
        reiseavstand?.let {
            brukerfeilHvis(it <= BigDecimal.ZERO) {
                "Reiseavstand må være større enn 0"
            }
        }
    }
}
