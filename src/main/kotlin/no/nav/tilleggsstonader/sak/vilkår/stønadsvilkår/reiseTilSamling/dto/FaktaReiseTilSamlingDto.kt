package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaDrosje
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.SvarJaNei
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaReiseTilSamlingOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaReiseTilSamlingPrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaReiseTilSamlingDrosjeDto::class, name = "DROSJE"),
    JsonSubTypes.Type(FaktaReiseTilSamlingUbestemtDto::class, name = "UBESTEMT"),
)
sealed interface FaktaReiseTilSamlingDto {
    val type: TypeReiseTilSamling

    fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ): FaktaReiseTilSamling
}

data class FaktaReiseTilSamlingOffentligTransportDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String,
    val aktivitet: AktivitetInfoDto? = null,
) : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.OFFENTLIG_TRANSPORT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaOffentligTransport(
        reiseId = reiseId,
        adresse = adresse,
        nødvendigUtgift = nødvendigUtgift.tilDomain(),
        utgifterOffentligTransport = utgifterOffentligTransport,
        begrunnelse = begrunnelse,
        aktivitetId = aktivitet?.aktivitetId,
    )
}

data class FaktaReiseTilSamlingPrivatBilDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val reiseavstand: BigDecimal,
    val begrunnelse: String?,
    val aktivitet: AktivitetInfoDto? = null,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
) : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.PRIVAT_BIL

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaPrivatBil(
        reiseId = reiseId,
        adresse = adresse,
        nødvendigUtgift = nødvendigUtgift.tilDomain(),
        reiseavstand = reiseavstand,
        begrunnelse = begrunnelse?.ifBlank { null },
        aktivitetId = aktivitet?.aktivitetId,
        bompenger = bompenger,
        fergekostnad = fergekostnad,
        parkering = parkering,
        piggdekkavgift = piggdekkavgift,
    )
}

data class FaktaReiseTilSamlingDrosjeDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val utgifterDrosje: BigDecimal,
    val begrunnelse: String,
    val aktivitet: AktivitetInfoDto? = null,
) : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.DROSJE

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaDrosje(
        reiseId = reiseId,
        adresse = adresse,
        nødvendigUtgift = nødvendigUtgift.tilDomain(),
        utgifterDrosje = utgifterDrosje,
        begrunnelse = begrunnelse,
        aktivitetId = aktivitet?.aktivitetId,
    )
}

data class NødvendigUtgiftDto(
    val erUtgiftenNødvendig: SvarJaNei,
    val begrunnelse: String?,
) {
    fun tilDomain(): NødvendigUtgift {
        feilHvis(erUtgiftenNødvendig == SvarJaNei.GAMMEL_MANGLER_DATA) {
            "erNødvendig kan ikke settes til GAMMEL_MANGLER_DATA ved oppretting eller oppdatering"
        }

        return NødvendigUtgift(
            erUtgiftenNødvendig = erUtgiftenNødvendig,
            begrunnelse = begrunnelse?.ifBlank { null },
        )
    }
}

data object FaktaReiseTilSamlingUbestemtDto : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.UBESTEMT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaUbestemtType(
        reiseId = reiseId,
        adresse = adresse,
    )
}
