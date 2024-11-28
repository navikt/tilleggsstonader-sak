package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate

data class LagreVilkårperiode(
    val behandlingId: BehandlingId,
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
    val faktaOgSvar: FaktaOgSvarDto? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaOgSvarMålgruppeDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(FaktaOgSvarAktivitetBarnetilsynDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(FaktaOgSvarAktivitetLæremidlerDto::class, name = "AKTIVITET_LÆREMIDLER"),
)
sealed class FaktaOgSvarDto

data class FaktaOgSvarMålgruppeDto(
    val svarMedlemskap: SvarJaNei? = null,
    val svarUtgifterDekketAvAnnetRegelverk: SvarJaNei? = null,
) : FaktaOgSvarDto()

data class FaktaOgSvarAktivitetBarnetilsynDto(
    val aktivitetsdager: Int? = null,
    val svarLønnet: SvarJaNei? = null,
) : FaktaOgSvarDto()

data class FaktaOgSvarAktivitetLæremidlerDto(
    val prosent: Int? = null,
    val svarHarUtgifter: SvarJaNei? = null,
) : FaktaOgSvarDto()
