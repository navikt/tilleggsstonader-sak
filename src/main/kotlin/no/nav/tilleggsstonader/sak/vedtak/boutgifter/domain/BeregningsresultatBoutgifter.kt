package no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class BeregningsresultatBoutgifter(
    val perioder: List<BeregningsresultatForLøpendeMåned>,
)

data class BeregningsresultatForLøpendeMåned(
    val grunnlag: Beregningsgrunnlag,
    val stønadsbeløp: Int,
) : Periode<LocalDate>,
    KopierPeriode<BeregningsresultatForLøpendeMåned> {
    @get:JsonIgnore
    override val fom: LocalDate get() = grunnlag.fom

    @get:JsonIgnore
    override val tom: LocalDate get() = grunnlag.tom

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): BeregningsresultatForLøpendeMåned = this.copy(grunnlag = this.grunnlag.copy(fom = fom, tom = tom))
}

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    val makssats: Int,
    val makssatsBekreftet: Boolean,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>
