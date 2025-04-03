package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Kommer oppdateres med faktisk målgruppe
 * TODO denne kan erstattes med VedtaksperiodeBeregning når den har tatt i bruk faktisk målgruppe
 */
data class VedtaksperiodeBeregningsgrunnlagLæremidler(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeBeregningsgrunnlagLæremidler> {
    init {
        validatePeriode()
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeBeregningsgrunnlagLæremidler = this.copy(fom = fom, tom = tom)
}
