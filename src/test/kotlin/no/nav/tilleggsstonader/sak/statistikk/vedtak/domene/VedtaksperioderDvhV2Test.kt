package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.EksempelRegel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class VedtaksperioderDvhV2Test {

    fun opprettVilkårsvurderinger(
        behandling: Behandling,
        barn: List<BehandlingBarn>,
        fom: LocalDate? = YearMonth.now().atDay(1),
        tom: LocalDate? = YearMonth.now().atEndOfMonth(),
        status: VilkårStatus = VilkårStatus.NY,
    ): List<Vilkår> {
        val hovedregelMetadata =
            HovedregelMetadata(
                barn = barn,
                behandling = mockk(),
            )
        val delvilkårsett = EksempelRegel().initiereDelvilkår(hovedregelMetadata)
        val vilkårsett = listOf(
            vilkår(
                fom = fom,
                tom = tom,
                status = status,
                resultat = Vilkårsresultat.OPPFYLT,
                type = VilkårType.PASS_BARN,
                behandlingId = behandling.id,
                barnId = barn.first().id,
                delvilkår = delvilkårsett,
            ),
        )
        return vilkårsett
    }

    fun List<String>.tilBehandlingBarn(behandling: Behandling) =
        this.map { behandlingBarn(behandlingId = behandling.id, personIdent = it) }


    @Test
    fun `fraDomene kan mappe for InnvilgelseTilsynBarn`() {

        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        val behandling = behandling()

        val barna = listOf("99999999999")
        val barnDetSøkesFor = barna.tilBehandlingBarn(behandling)

        val vilkår =
            opprettVilkårsvurderinger(behandling, barn = barnDetSøkesFor, fom = fom, tom = tom)

        val resultat = VedtaksperioderDvhV2.fraDomene(
            innvilgelseTilsynBarn(),
            vilkår = vilkår,
            barnFakta = barnDetSøkesFor,
        ).vedtaksperioder

        val forventetResultat = listOf(
            VedtaksperioderDvhV2(
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeTypeDvh.AAP,
                lovverketsMålgruppe = LovverketsMålgruppeDvh.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetTypeDvh.TILTAK,
                antallBarn = 1,
                barn = BarnDvh.JsonWrapper(barna.map { BarnDvh(it) }),
            )
        )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `fraDomene kan mappe for InnvilgelseLæremidler`() {
        val resultat = VedtaksperioderDvhV2.fraDomene(
            vedtak = innvilgelseLæremidler(),
            vilkår = emptyList(),
            barnFakta = emptyList()
        )

        val forventetResultat = VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder = listOf(
                VedtaksperioderDvhV2(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 7),
                    målgruppe = MålgruppeTypeDvh.AAP,
                    lovverketsMålgruppe = LovverketsMålgruppeDvh.NEDSATT_ARBEIDSEVNE,
                    studienivå = StudienivåDvh.HØYERE_UTDANNING
                )
            )
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
            vedtaksperioder = emptyList()
        )

        assertThat(resultat).isEqualTo(forventetResultat)
    }
}