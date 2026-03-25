package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Delperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

object RammevedtakPrivatBilUtil {
    // Oppdatert rammeForReiseMedPrivatBil for å støtte flere delperioder
    fun rammeForReiseMedPrivatBil(
        reiseId: ReiseId = ReiseId.random(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(7),
        reisedagerPerUke: Int = 5,
        reiseavstandEnVei: BigDecimal = 10.toBigDecimal(),
        ekstrakostnader: Ekstrakostnader = Ekstrakostnader(null, null),
        satsBekreftetVedVedtakstidspunkt: Boolean = true,
        kilometersats: BigDecimal = 2.94.toBigDecimal(),
        dagsatsUtenParkering: BigDecimal = 100.toBigDecimal(),
        vedtaksperioder: List<Vedtaksperiode> = listOf(vedtaksperiode(fom, tom)),
        delperioder: List<Delperiode>? = null,
    ): RammeForReiseMedPrivatBil =
        RammeForReiseMedPrivatBil(
            reiseId = reiseId,
            aktivitetsadresse = "aktivitetsadresse",
            grunnlag =
                BeregningsgrunnlagForReiseMedPrivatBil(
                    fom = fom,
                    tom = tom,
                    delPerioder =
                        delperioder ?: listOf(
                            Delperiode(
                                fom = fom,
                                tom = tom,
                                reisedagerPerUke = reisedagerPerUke,
                                ekstrakostnader = ekstrakostnader,
                                satsBekreftetVedVedtakstidspunkt = satsBekreftetVedVedtakstidspunkt,
                                kilometersats = kilometersats,
                                dagsatsUtenParkering = dagsatsUtenParkering,
                            ),
                        ),
                    reiseavstandEnVei = reiseavstandEnVei,
                    vedtaksperioder = vedtaksperioder,
                ),
        )

    // Oppdatert rammevedtakPrivatBil for å støtte flere delperioder
    fun rammevedtakPrivatBil(
        reiseId: ReiseId = ReiseId.random(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(7),
        reisedagerPerUke: Int = 5,
        reiseavstandEnVei: BigDecimal = 10.toBigDecimal(),
        ekstrakostnader: Ekstrakostnader = Ekstrakostnader(null, null),
        satsBekreftetVedVedtakstidspunkt: Boolean = true,
        kilometersats: BigDecimal = 2.94.toBigDecimal(),
        dagsatsUtenParkering: BigDecimal = 100.toBigDecimal(),
        vedtaksperioder: List<Vedtaksperiode> = listOf(vedtaksperiode(fom, tom)),
        delperioder: List<Delperiode>? = null,
    ) = RammevedtakPrivatBil(
        reiser =
            listOf(
                rammeForReiseMedPrivatBil(
                    reiseId,
                    fom,
                    tom,
                    reisedagerPerUke,
                    reiseavstandEnVei,
                    ekstrakostnader,
                    satsBekreftetVedVedtakstidspunkt,
                    kilometersats,
                    dagsatsUtenParkering,
                    vedtaksperioder,
                    delperioder,
                ),
            ),
    )
}
