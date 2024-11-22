package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate

data class LagreVilkårperiodeNy(
    val behandlingId: BehandlingId,
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
    val faktaOgVurderinger: FaktaOgVurderingerDto,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaOgVurderingerMålgruppeDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(FaktaOgVurderingerAktivitetBarnetilsynDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(FaktaOgVurderingerAktivitetLæremidlerDto::class, name = "AKTIVITET_LÆREMIDLER"),
)
sealed class FaktaOgVurderingerDto

data class FaktaOgVurderingerMålgruppeDto(
    val svarMedlemskap: SvarJaNei? = null,
    val svarUtgifterDekketAvAnnetRegelverk: SvarJaNei? = null,
) : FaktaOgVurderingerDto()

data class FaktaOgVurderingerAktivitetBarnetilsynDto(
    val aktivitetsdager: Int? = null,
    val svarLønnet: SvarJaNei? = null,
) : FaktaOgVurderingerDto()

data class FaktaOgVurderingerAktivitetLæremidlerDto(
    val prosent: Int? = null,
    val svarHarUtgifter: SvarJaNei? = null,
) : FaktaOgVurderingerDto()
