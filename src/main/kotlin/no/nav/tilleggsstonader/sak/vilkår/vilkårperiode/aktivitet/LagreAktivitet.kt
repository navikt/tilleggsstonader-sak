package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate

data class LagreAktivitet(
    val behandlingId: BehandlingId,
    val type: AktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val faktaOgVurderinger: FaktaOgVurderingerAktivitetDto,
    val begrunnelse: String? = null,
    val kildeId: String? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaOgVurderingerAktivitetBarnetilsynDto::class, name = "AKTIVITET_BARNETILSYN"),
    JsonSubTypes.Type(FaktaOgVurderingerAktivitetLæremidlerDto::class, name = "AKTIVITET_LÆREMIDLER"),
)
sealed class FaktaOgVurderingerAktivitetDto;

data class FaktaOgVurderingerAktivitetBarnetilsynDto(
    val aktivitetsdager: Int? = null,
    val svarLønnet: SvarJaNei? = null,
) : FaktaOgVurderingerAktivitetDto()

data class FaktaOgVurderingerAktivitetLæremidlerDto(
    val prosent: Int? = null,
    val svarHarUtgifter: SvarJaNei? = null,
) : FaktaOgVurderingerAktivitetDto()