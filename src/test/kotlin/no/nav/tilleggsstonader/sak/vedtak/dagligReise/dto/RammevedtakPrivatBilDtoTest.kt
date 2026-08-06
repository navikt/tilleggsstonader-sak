package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RammevedtakPrivatBilDtoTest {
    @Test
    fun `skal kun inkludere reiser fra og med tidligste endring`() {
        val reiseFørEndring = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 9 januar 2026, tom = 9 januar 2026)
        val reiseLikTidligsteEndring =
            rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 10 januar 2026, tom = 10 januar 2026)
        val reiseEtterEndring =
            rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 11 januar 2026, tom = 11 januar 2026)
        val rammevedtak =
            RammevedtakPrivatBil(
                reiser = listOf(reiseFørEndring, reiseLikTidligsteEndring, reiseEtterEndring),
            )

        val dto =
            rammevedtak.tilDto(
                Beregningsplan(
                    omfang = Beregningsomfang.FRA_DATO,
                    fraDato = 10 januar 2026,
                    tidligsteEndring = 10 januar 2026,
                ),
            )

        assertThat(dto.reiser.map { it.reiseId }).containsExactlyInAnyOrder(
            reiseLikTidligsteEndring.reiseId,
            reiseEtterEndring.reiseId,
        )
    }

    @Test
    fun `skal inkludere alle reiser når tidligste endring er null`() {
        val reise1 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 9 januar 2026, tom = 9 januar 2026)
        val reise2 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 10 januar 2026, tom = 10 januar 2026)
        val reise3 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 11 januar 2026, tom = 11 januar 2026)
        val rammevedtak = RammevedtakPrivatBil(reiser = listOf(reise1, reise2, reise3))

        val dto =
            rammevedtak.tilDto(
                Beregningsplan(
                    omfang = Beregningsomfang.ALLE_PERIODER,
                ),
            )

        assertThat(dto.reiser.map { it.reiseId }).containsExactlyInAnyOrder(
            reise1.reiseId,
            reise2.reiseId,
            reise3.reiseId,
        )
    }
}
