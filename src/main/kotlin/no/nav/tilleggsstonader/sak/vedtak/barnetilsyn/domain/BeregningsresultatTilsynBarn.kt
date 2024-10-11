package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode as StønadsperiodeInngangsvilkår

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
 * @param dato tilsvarer fom datoen på en stønadsperiode
 * og er den datoen hele beløpet samlet iversettes på
 */
data class Beløpsperiode(
    val dato: LocalDate,
    val beløp: Int,
    val målgruppe: MålgruppeType,
)

/**
 * @param makssats er snitt per måned
 */
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int,
    val stønadsperioderGrunnlag: List<StønadsperiodeGrunnlag>,
    val utgifter: List<UtgiftBarn>,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

data class StønadsperiodeGrunnlag(
    val stønadsperiode: Stønadsperiode,
    val aktiviteter: List<Aktivitet>,
    val antallDager: Int,
)

data class Stønadsperiode(
    val id: UUID? = null,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

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

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> {
    return this.mapNotNull {
        if (it.type is AktivitetType) {
            Aktivitet(
                id = it.id,
                type = it.type,
                fom = it.fom,
                tom = it.tom,
                aktivitetsdager = it.aktivitetsdager ?: error("Aktivitetsdager mangler på periode ${it.id}"),
            )
        } else {
            null
        }
    }
}

fun StønadsperiodeDto.tilBeregningsgrunnlagStønadsperiode() = Stønadsperiode(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    målgruppe = this.målgruppe,
    aktivitet = this.aktivitet,
)

fun StønadsperiodeInngangsvilkår.tilGrunnlagStønadsperiode() = Stønadsperiode(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    målgruppe = this.målgruppe,
    aktivitet = this.aktivitet,
)

fun List<StønadsperiodeInngangsvilkår>.tilSortertGrunnlagStønadsperiode() = this.map { it.tilGrunnlagStønadsperiode() }.sortedBy { it.fom }
