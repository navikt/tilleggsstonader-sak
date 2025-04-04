package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvh.Companion.finnFødselsnumre
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBarn1
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBarn2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBehandling
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.avslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class VedtaksperioderDvhV2Test {
    val fom: LocalDate = LocalDate.of(2025, 1, 1)
    val tom: LocalDate = LocalDate.of(2025, 1, 31)

    val behandling = defaultBehandling

    val barn1 = listOf(defaultBarn1)
    val barn2 = listOf(defaultBarn2)
    val alleBarn = barn1 + barn2

    @Test
    fun `fraDomene kan mappe for InnvilgelseTilsynBarn`() {
        val resultat =
            VedtaksperioderDvh
                .fraDomene(
                    innvilgelseTilsynBarn(),
                    barn = barn1,
                ).vedtaksperioder

        val forventetResultat =
            listOf(
                VedtaksperioderDvh(
                    fom = fom,
                    tom = tom,
                    lovverketsMålgruppe = LovverketsMålgruppeDvh.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetTypeDvh.TILTAK,
                    antallBarn = 1,
                    barn = BarnDvh.JsonWrapper(barn1.map { BarnDvh(it.ident) }),
                ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `fraDomene kan mappe for InnvilgelseLæremidler`() {
        val resultat =
            VedtaksperioderDvh.fraDomene(
                vedtak = innvilgelseLæremidler(),
                barn = emptyList(),
            )

        val forventetResultat =
            VedtaksperioderDvh.JsonWrapper(
                vedtaksperioder =
                    listOf(
                        VedtaksperioderDvh(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 7),
                            aktivitet = AktivitetTypeDvh.TILTAK,
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.NEDSATT_ARBEIDSEVNE,
                            studienivå = StudienivåDvh.HØYERE_UTDANNING,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `Avslag skal ikke ha vedtaksperioder`() {
        val avslag =
            avslag(
                årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND),
                begrunnelse = "Begrunelse for avslag",
            )

        val resultat = VedtaksperioderDvh.fraDomene(vedtak = avslag, barn = emptyList())

        val forventetResultat =
            VedtaksperioderDvh.JsonWrapper(
                vedtaksperioder = emptyList(),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Nested
    inner class FinnFødselsnumre {
        @Test
        fun `finnBarnasFødselsnumre skal finne fødselsnummeret til barn når det er ett barn`() {
            val resultat = barn1.map { it.id }.finnFødselsnumre(alleBarn)

            assertThat(resultat).isEqualTo(barn1.map { it.ident })
        }

        @Test
        fun `finnBarnasFødselsnumre skal finne fødselsnummeret til barn når det er flere barn`() {
            val resultat = alleBarn.map { it.id }.finnFødselsnumre(alleBarn)

            assertThat(resultat).isEqualTo(alleBarn.map { it.ident })
        }
    }
}
