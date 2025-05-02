package no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import kotlin.math.min

data class BeregningsresultatBoutgifter(
    val perioder: List<BeregningsresultatForLøpendeMåned>,
)

data class BeregningsresultatForLøpendeMåned(
    val grunnlag: Beregningsgrunnlag,
    val delAvTidligereUtbetaling: Boolean = false,
) : Periode<LocalDate>,
    KopierPeriode<BeregningsresultatForLøpendeMåned> {
    @get:JsonIgnore
    override val fom: LocalDate get() = grunnlag.fom

    @get:JsonIgnore
    override val tom: LocalDate get() = grunnlag.tom

    @JsonProperty("stønadsbeløp")
    val stønadsbeløp = summerUtgifter().begrensTilMakssats()

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): BeregningsresultatForLøpendeMåned = this.copy(grunnlag = this.grunnlag.copy(fom = fom, tom = tom))

    fun markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling: Boolean) =
        this.copy(delAvTidligereUtbetaling = delAvTidligereUtbetaling)

    fun summerUtgifter(): Int =
        grunnlag.utgifter.values
            .flatten()
            .sumOf { it.utgift }

    private fun Int.begrensTilMakssats() = min(this, grunnlag.makssats)
}

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
    val utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    val makssats: Int,
    val makssatsBekreftet: Boolean,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>
