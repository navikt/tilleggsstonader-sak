package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2.Companion.finnBarnasFødselsnumre
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2.Companion.finnOverlappendeVilkårperioder
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.VilkårTestUtils.opprettVilkårsvurderinger
import no.nav.tilleggsstonader.sak.vilkår.VilkårTestUtils.tilBehandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class VedtaksperioderDvhV2Test {
    val fom = LocalDate.of(2024, 1, 1)
    val tom = LocalDate.of(2024, 1, 31)

    val behandling = behandling()

    val barn1 = listOf("99999999999").tilBehandlingBarn(behandling)
    val barn2 = listOf("11111111111").tilBehandlingBarn(behandling)
    val alleBarn = barn1 + barn2

    @Test
    fun `fraDomene kan mappe for InnvilgelseTilsynBarn`() {
        val vilkår =
            opprettVilkårsvurderinger(behandling, barn = barn1, fom = fom, tom = tom)

        val resultat = VedtaksperioderDvhV2.fraDomene(
            innvilgelseTilsynBarn(),
            vilkår = vilkår,
            barnFakta = barn1,
        ).vedtaksperioder

        val forventetResultat = listOf(
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
        val resultat = VedtaksperioderDvhV2.fraDomene(
            vedtak = innvilgelseLæremidler(),
            vilkår = emptyList(),
            barnFakta = emptyList(),
        )

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = listOf(
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
        val avslag = GeneriskVedtak(
            behandlingId = behandling().id,
            type = TypeVedtak.AVSLAG,
            data = AvslagLæremidler(
                årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND),
                begrunnelse = "Begrunelse for avslag",
            ),
        )

        val resultat = VedtaksperioderDvhV2.fraDomene(vedtak = avslag, vilkår = emptyList(), barnFakta = emptyList())

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = emptyList(),
        )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Nested
    inner class FinnFødselsnumreIVedtaksperiode {
        @Test
        fun `finnBarnFnr skal finne fødselsnummeret til barn i vilkåret når det er ett barn`() {
            val vilkår = opprettVilkårsvurderinger(behandling, barn = barn1, fom = fom, tom = tom)

            val vedtaksperiode = VedtaksperiodeTilsynBarn(
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                antallBarn = 1,
            )

            val resultat = vedtaksperiode.finnOverlappendeVilkårperioder(vilkår).finnBarnasFødselsnumre(alleBarn)

            assertThat(resultat).isEqualTo(barn1.map { it.ident })
        }

        @Test
        fun `finnBarnFnr skal finne fødselsnummeret til barn i vilkåret når det er flere barn`() {
            val vilkårBarn1 = opprettVilkårsvurderinger(behandling, barn = barn1, fom = fom, tom = tom)
            val vilkårBarn2 = opprettVilkårsvurderinger(behandling, barn = barn2, fom = fom, tom = tom)
            val vilkår = vilkårBarn1 + vilkårBarn2

            val vedtaksperiode = VedtaksperiodeTilsynBarn(
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                antallBarn = 2,
            )

            val resultat = vedtaksperiode
                .finnOverlappendeVilkårperioder(vilkår)
                .finnBarnasFødselsnumre(alleBarn)

            assertThat(resultat).isEqualTo(alleBarn.map { it.ident })
        }
    }
}
