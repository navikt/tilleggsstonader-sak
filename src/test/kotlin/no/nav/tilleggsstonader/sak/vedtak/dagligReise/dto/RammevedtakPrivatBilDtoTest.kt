package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RammevedtakPrivatBilDtoTest {
    @Test
    fun `skal kun ekskludere reiser som er avsluttet før tidligste endring`() {
        val reiseFørEndring = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 januar 2026, tom = 31 januar 2026)
        val reiseSomDekkerTidligsteEndring =
            rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 20 januar 2026, tom = 8 februar 2026)
        val reiseLikTidligsteEndring =
            rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 februar 2026, tom = 28 februar 2026)
        val reiseEtterEndring =
            rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 mars 2026, tom = 31 mars 2026)
        val rammevedtak =
            RammevedtakPrivatBil(
                reiser = listOf(reiseFørEndring, reiseSomDekkerTidligsteEndring, reiseLikTidligsteEndring, reiseEtterEndring),
            )

        val dto =
            rammevedtak.tilDto(
                Beregningsplan(
                    omfang = Beregningsomfang.FRA_DATO,
                    fraDato = 1 februar 2026,
                    tidligsteEndring = 1 februar 2026,
                ),
            )

        assertThat(dto.reiser.map { it.reiseId }).containsExactlyInAnyOrder(
            reiseSomDekkerTidligsteEndring.reiseId,
            reiseLikTidligsteEndring.reiseId,
            reiseEtterEndring.reiseId,
        )
    }

    @Test
    fun `skal inkludere alle reiser når tidligste endring er null`() {
        val reise1 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 januar 2026, tom = 31 januar 2026)
        val reise2 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 februar 2026, tom = 28 februar 2026)
        val reise3 = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 mars 2026, tom = 31 mars 2026)
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
