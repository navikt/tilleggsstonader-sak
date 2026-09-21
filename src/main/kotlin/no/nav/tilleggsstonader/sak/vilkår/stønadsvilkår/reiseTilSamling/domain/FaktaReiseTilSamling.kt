package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
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
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : FaktaReiseTilSamling {
    override val type = TypeReiseTilSamling.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
        validerBegrunnelse()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseTilSamlingOffentligTransport(
            reiseId = reiseId,
            adresse = adresse,
            utgifterOffentligTransport = utgifterOffentligTransport,
            begrunnelse = begrunnelse,
            aktivitetId = aktivitetId,
        )

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(utgifterOffentligTransport <= 0.toBigDecimal()) {
            "Utgifter til offentlig transport kan ikke være negative"
        }
    }

    private fun validerBegrunnelse() {
        brukerfeilHvis(begrunnelse.isBlank()) {
            "Spesifikasjon av utgift må fylles ut"
        }
    }
}

data class FaktaPrivatBil(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reiseavstand: BigDecimal,
    val begrunnelse: String,
    val aktivitetId: VilkårperiodeGlobalId? = null,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
) : FaktaReiseTilSamling {
    override val type = TypeReiseTilSamling.PRIVAT_BIL

    companion object {
        val MAKS_BOMPENGER = BigDecimal(500)
        val MAKS_FERGEKOSTNAD = BigDecimal(900)
        val MAKS_PARKERING = BigDecimal(1000)
        val MAKS_PIGGDEKKAVGIFT = BigDecimal(1400)
    }

    init {
        validerIngenNegativReiseavstand()
        validerIngenNegativeUtgifter()
        validerMaksbeløpUtgifter()
        validerBegrunnelse()
    }

    override fun mapTilVilkårFakta() =
        FaktaReiseTilSamlingPrivatBil(
            reiseId = reiseId,
            adresse = adresse,
            reiseavstand = reiseavstand,
            begrunnelse = begrunnelse,
            aktivitetId = aktivitetId,
            bompenger = bompenger,
            fergekostnad = fergekostnad,
            parkering = parkering,
            piggdekkavgift = piggdekkavgift,
        )

    private fun validerIngenNegativReiseavstand() {
        reiseavstand.let {
            brukerfeilHvis(it <= 30.toBigDecimal()) {
                "Reiseavstand kan ikke være mindre enn 30 km"
            }
        }
    }

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(bompenger != null && bompenger < BigDecimal.ZERO) {
            "Bompenger kan ikke være negativt"
        }
        brukerfeilHvis(fergekostnad != null && fergekostnad < BigDecimal.ZERO) {
            "Fergekostnad kan ikke være negativ"
        }
        brukerfeilHvis(parkering != null && parkering < BigDecimal.ZERO) {
            "Parkering kan ikke være negativ"
        }
        brukerfeilHvis(piggdekkavgift != null && piggdekkavgift < BigDecimal.ZERO) {
            "Piggdekkavgift kan ikke være negativ"
        }
    }

    private fun validerMaksbeløpUtgifter() {
        brukerfeilHvis(bompenger != null && bompenger > MAKS_BOMPENGER) {
            "Skal du innvilge med bompenger høyere enn ${MAKS_BOMPENGER}kr må du ta kontakt med Tilleggsstønader-temet"
        }
        brukerfeilHvis(fergekostnad != null && fergekostnad > MAKS_FERGEKOSTNAD) {
            "Skal du innvilge med fergekostnad høyere enn ${MAKS_FERGEKOSTNAD}kr må du ta kontakt med Tilleggsstønader-temet"
        }
        brukerfeilHvis(parkering != null && parkering > MAKS_PARKERING) {
            "Skal du innvilge med parkering høyere enn ${MAKS_PARKERING}kr må du ta kontakt med Tilleggsstønader-temet"
        }
        brukerfeilHvis(piggdekkavgift != null && piggdekkavgift > MAKS_PIGGDEKKAVGIFT) {
            "Skal du innvilge med piggdekkavgift høyere enn ${MAKS_PIGGDEKKAVGIFT}kr må du ta kontakt med Tilleggsstønader-temet"
        }
    }

    private fun validerBegrunnelse() {
        brukerfeilHvis(begrunnelse.isBlank()) {
            "Spesifikasjon av utgift må fylles ut"
        }
    }
}
