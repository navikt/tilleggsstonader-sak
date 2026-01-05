package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class BeregningsresultatOffentligTransport(
    val reiser: List<BeregningsresultatForReise>,
) {
    fun sorterReiserOgPerioder(): BeregningsresultatOffentligTransport =
        BeregningsresultatOffentligTransport(
            reiser
                .sortedBy { reise -> reise.perioder.minOf { it.grunnlag.fom } }
                .map { reise ->
                    reise.copy(perioder = reise.perioder.sortedBy { it.grunnlag.fom })
                },
        )
}

data class BeregningsresultatForReise(
    val reiseId: ReiseId,
    val perioder: List<BeregningsresultatForPeriode>,
)

data class BeregningsresultatForPeriode(
    val grunnlag: BeregningsgrunnlagOffentligTransport,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val fraTidligereVedtak: Boolean = false,
)

data class BeregningsgrunnlagOffentligTransport(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val antallReisedagerPerUke: Int,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    val antallReisedager: Int,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val brukersNavKontor: String?,
) : Periode<LocalDate>

// Legger på Include.NON_NULL for å unngå at vi serialiserer "typeAktivitet": null" i JSON for TSO som er i produksjon
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VedtaksperiodeGrunnlag(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val typeAktivitet: TypeAktivitet?,
    val antallReisedagerIVedtaksperioden: Int,
) {
    constructor(vedtaksperiode: Vedtaksperiode, antallReisedager: Int) : this(
        id = vedtaksperiode.id,
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
        målgruppe = vedtaksperiode.målgruppe,
        aktivitet = vedtaksperiode.aktivitet,
        typeAktivitet = vedtaksperiode.typeAktivitet,
        antallReisedagerIVedtaksperioden = antallReisedager,
    )
}
