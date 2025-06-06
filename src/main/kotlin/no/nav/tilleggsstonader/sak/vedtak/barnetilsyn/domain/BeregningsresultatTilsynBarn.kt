package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFaktaOrThrow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class BeregningsresultatTilsynBarn(
    val perioder: List<BeregningsresultatForMåned>,
)

data class BeregningsresultatForMåned(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: Beregningsgrunnlag,
    val beløpsperioder: List<Beløpsperiode>,
)

/**
 * @param dato tilsvarer fom datoen på en vedtaksperiode
 * og er den datoen hele beløpet samlet iversettes på
 */
data class Beløpsperiode(
    val dato: LocalDate,
    val beløp: Int,
    val målgruppe: FaktiskMålgruppe,
)

/**
 * @param makssats er snitt per måned
 */
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int,
    val vedtaksperiodeGrunnlag: List<VedtaksperiodeGrunnlag>,
    val utgifter: List<UtgiftBarn>,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

data class VedtaksperiodeGrunnlag(
    val vedtaksperiode: VedtaksperiodeBeregning,
    val aktiviteter: List<Aktivitet>,
    val antallDager: Int,
)

data class UtgiftBarn(
    val barnId: BarnId,
    val utgift: Int,
)

data class Aktivitet(
    val id: UUID?, // Må være null pga bakåtkompatibilitet
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitetsdager: Int,
) : Periode<LocalDate>

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> =
    this
        .ofType<AktivitetTilsynBarn>()
        .map {
            val fakta = it.faktaOgVurdering.fakta
            Aktivitet(
                id = it.id,
                type = it.faktaOgVurdering.type.vilkårperiodeType,
                fom = it.fom,
                tom = it.tom,
                aktivitetsdager = fakta.takeIfFaktaOrThrow<FaktaAktivitetsdager>().aktivitetsdager,
            )
        }
