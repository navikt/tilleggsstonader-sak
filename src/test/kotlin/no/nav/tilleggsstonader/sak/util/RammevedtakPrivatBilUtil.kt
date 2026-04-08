package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.SatsForPeriodePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate

object RammevedtakPrivatBilUtil {
    fun rammevedtakPrivatBil(
        reiseId: ReiseId = ReiseId.random(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(7),
        reisedagerPerUke: Int = 5,
        reiseavstandEnVei: BigDecimal = 10.toBigDecimal(),
        ekstrakostnader: Ekstrakostnader = Ekstrakostnader(null, null),
        satser: List<SatsForPeriodePrivatBil> =
            listOf(
                satsForPeriodePrivatBil(
                    fom = fom,
                    tom = tom,
                    kilometersats = 2.94.toBigDecimal(),
                    dagsatsUtenParkering = 100.toBigDecimal(),
                ),
            ),
        vedtaksperioder: List<Vedtaksperiode> = listOf(vedtaksperiode(fom, tom)),
    ) = RammevedtakPrivatBil(
        reiser =
            listOf(
                rammeForReiseMedPrivatBil(reiseId, fom, tom, reisedagerPerUke, reiseavstandEnVei, ekstrakostnader, satser, vedtaksperioder),
            ),
    )

    fun rammeForReiseMedPrivatBil(
        reiseId: ReiseId = ReiseId.random(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(7),
        reisedagerPerUke: Int = 5,
        reiseavstandEnVei: BigDecimal = 10.toBigDecimal(),
        ekstrakostnader: Ekstrakostnader = Ekstrakostnader(null, null),
        satser: List<SatsForPeriodePrivatBil> =
            listOf(
                satsForPeriodePrivatBil(
                    fom = fom,
                    tom = tom,
                    kilometersats = 2.94.toBigDecimal(),
                    dagsatsUtenParkering = 100.toBigDecimal(),
                ),
            ),
        vedtaksperioder: List<Vedtaksperiode> = listOf(vedtaksperiode(fom, tom)),
    ): RammeForReiseMedPrivatBil =
        RammeForReiseMedPrivatBil(
            reiseId = reiseId,
            aktivitetsadresse = "aktivitetsadresse",
            aktivitetType = AktivitetType.TILTAK,
            typeAktivitet = TypeAktivitet.GRUPPEAMO,
            grunnlag =
                BeregningsgrunnlagForReiseMedPrivatBil(
                    fom = fom,
                    tom = tom,
                    reisedagerPerUke = reisedagerPerUke,
                    reiseavstandEnVei = reiseavstandEnVei,
                    ekstrakostnader = ekstrakostnader,
                    satser = satser,
                    vedtaksperioder = vedtaksperioder,
                ),
        )

    fun satsForPeriodePrivatBil(
        fom: LocalDate,
        tom: LocalDate,
        kilometersats: BigDecimal,
        dagsatsUtenParkering: BigDecimal,
    ): SatsForPeriodePrivatBil =
        SatsForPeriodePrivatBil(
            fom = fom,
            tom = tom,
            satsBekreftetVedVedtakstidspunkt = true,
            kilometersats = kilometersats,
            dagsatsUtenParkering = dagsatsUtenParkering,
        )
}
