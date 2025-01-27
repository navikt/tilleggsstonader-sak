package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2.Companion.finnFødselsnumre
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBarn1
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBarn2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBehandling
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class VedtaksperioderDvhV2Test {
    val fom: LocalDate = LocalDate.of(2024, 1, 1)
    val tom: LocalDate = LocalDate.of(2024, 1, 31)

    val behandling = defaultBehandling

    val barn1 = listOf(defaultBarn1)
    val barn2 = listOf(defaultBarn2)
    val alleBarn = barn1 + barn2

    @Test
    fun `fraDomene kan mappe for InnvilgelseTilsynBarn`() {
        val resultat =
            VedtaksperioderDvhV2
                .fraDomene(
                    innvilgelseTilsynBarn(),
                    barn = barn1,
                ).vedtaksperioder

        val forventetResultat =
            listOf(
                VedtaksperioderDvhV2(
                    fom = fom,
                    tom = tom,
                    målgruppe = MålgruppeTypeDvh.AAP,
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
            VedtaksperioderDvhV2.fraDomene(
                vedtak = innvilgelseLæremidler(),
                barn = emptyList(),
            )

        val forventetResultat =
            VedtaksperioderDvhV2.JsonWrapper(
                vedtaksperioder =
                    listOf(
                        VedtaksperioderDvhV2(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 7),
                            målgruppe = MålgruppeTypeDvh.AAP,
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
            GeneriskVedtak(
                behandlingId = behandling().id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagLæremidler(
                        årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND),
                        begrunnelse = "Begrunelse for avslag",
                    ),
            )

        val resultat = VedtaksperioderDvhV2.fraDomene(vedtak = avslag, barn = emptyList())

        val forventetResultat =
            VedtaksperioderDvhV2.JsonWrapper(
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
