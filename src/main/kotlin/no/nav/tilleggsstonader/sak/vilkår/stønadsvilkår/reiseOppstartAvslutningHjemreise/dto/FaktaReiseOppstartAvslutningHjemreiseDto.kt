package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreisePrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreiseUbestemtDto::class, name = "UBESTEMT"),
    JsonSubTypes.Type(FaktaMidlertidigBostedPeriodeDto::class, name = "MIDLERTIDIG_BOSTED_PERIODE"),
)
sealed interface FaktaReiseOppstartAvslutningHjemreiseDto {
    val type: TypeReiseOppstartAvslutningHjemreise
    val aktivitetId: VilkårperiodeGlobalId

    fun mapTilFakta(): FaktaReiseOppstartAvslutningHjemreise
}

sealed interface ReiseFaktaROAHDto : FaktaReiseOppstartAvslutningHjemreiseDto {
    val reiseId: ReiseId
    val typeReiseformål: TypeReiseformål
}

data class FaktaMidlertidigBostedPeriodeDto(
    override val aktivitetId: VilkårperiodeGlobalId,
    val midlertidigAdresse: String,
    val hjemstedAdresse: String,
) : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.MIDLERTIDIG_BOSTED_PERIODE

    override fun mapTilFakta() = FaktaMidlertidigBostedPeriode(aktivitetId, midlertidigAdresse, hjemstedAdresse)

    override fun toString() = "FaktaMidlertidigBostedPeriodeDto(aktivitetId=$aktivitetId, midlertidigAdresse=***, hjemstedAdresse=***)"
}

data class FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
    val nødvendigUtgift: NødvendigUtgiftDto,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : ReiseFaktaROAHDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta() =
        FaktaOffentligTransport(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = nødvendigUtgift.tilDomene(),
            utgifterOffentligTransport = utgifterOffentligTransport,
            begrunnelse = begrunnelse,
        )
}

data class FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
    val nødvendigUtgift: NødvendigUtgiftDto,
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : ReiseFaktaROAHDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

    override fun mapTilFakta() =
        FaktaPrivatBil(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = nødvendigUtgift.tilDomene(),
            reiseavstand = reiseavstand,
            bompenger = bompenger,
            fergekostnad = fergekostnad,
            parkering = parkering,
            piggdekkavgift = piggdekkavgift,
            begrunnelse = begrunnelse,
        )
}

data class FaktaReiseOppstartAvslutningHjemreiseUbestemtDto(
    override val reiseId: ReiseId,
    override val aktivitetId: VilkårperiodeGlobalId,
    override val typeReiseformål: TypeReiseformål,
) : ReiseFaktaROAHDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilFakta() = FaktaUbestemtType(reiseId, aktivitetId, typeReiseformål)
}
