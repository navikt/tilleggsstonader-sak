package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import java.time.LocalDate
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class VedtaksperioderDvhV2Test {

    @Test
    fun `fraDomene kan mappe for InnvilgelseTilsynBarn`() {
        val resultat = VedtaksperioderDvhV2.fraDomene(innvilgelseTilsynBarn())

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = listOf(
                VedtaksperioderDvhV2(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 7),
                    målgruppe = MålgruppeTypeDvh.AAP,
                    aktivitet = AktivitetTypeDvh.TILTAK,
                    antallBarn = 1
                )
            )
        )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `fraDomene kan mappe for InnvilgelseLæremidler`() {
        val resultat = VedtaksperioderDvhV2.fraDomene(innvilgelseLæremidler())

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = listOf(
                VedtaksperioderDvhV2(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 7),
                    målgruppe = MålgruppeTypeDvh.AAP,
                    aktivitet = AktivitetTypeDvh.UTDANNING,
                    studienivå = StudienivåDvh.HØYERE_UTDANNING
                )
            )
        )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `Avslag skal ikke ha vedtaksperioder`() {
        val avslag = GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.AVSLAG,
            data = AvslagLæremidler(
                årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND),
                begrunnelse = "Begrunelse for avslag",
            ),
        )

        val resultat = VedtaksperioderDvhV2.fraDomene(avslag)

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = emptyList()
        )

        assertThat(resultat).isEqualTo(forventetResultat)

    }

}