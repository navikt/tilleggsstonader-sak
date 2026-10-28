package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagFaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate

class InterntVedtakFlyttingMappingTest {
    private val behandlingId = BehandlingId.random()
    private val dato = LocalDate.of(2025, 1, 15)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `mapper flyttefakta og beregningsresultat for begge stønadstypene`(dokumentert: Boolean) {
        listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR).forEach { stønadstype ->
            val flyttebyråFakta =
                FaktaFlyttebyrå(
                    tilbud = listOf(FlyttebyråTilbud("Byrå A", 1200), FlyttebyråTilbud("Byrå B", 1000)),
                    erUtgifterDokumentert = true,
                    nødvendigUtgift = NødvendigUtgift(SvarJaNei.JA, null),
                    adresse = "Flytteveien 1",
                )
            val flytteSelvFakta =
                FaktaFlytteSelv(
                    erUtgifterDokumentert = dokumentert,
                    avstandEnVei = 100,
                    henger = 200,
                    bompenger = 300,
                    ferge = 400,
                    parkering = 500,
                    nødvendigUtgift = NødvendigUtgift(SvarJaNei.JA, null),
                    adresse = "Flytteveien 2",
                )
            val ubestemtFakta = FaktaFlyttingUbestemt(adresse = "Flytteveien 3")
            val beregningsresultat = beregningsresultat()
            val vedtak = innvilgelse(beregningsresultat)

            val flyttebyrå = lagInterntVedtak(stønadstype, flyttebyråFakta, vedtak)
            val flytteSelv = lagInterntVedtak(stønadstype, flytteSelvFakta, vedtak)
            val ubestemt = lagInterntVedtak(stønadstype, ubestemtFakta, vedtak)

            assertThat(flyttebyrå.vilkår.single().fakta)
                .isEqualTo(
                    VilkårFaktaFlyttebyråInterntVedtak(
                        tilbud = flyttebyråFakta.tilbud,
                        erUtgifterDokumentert = true,
                        nødvendigUtgift = flyttebyråFakta.nødvendigUtgift,
                        adresse = "Flytteveien 1",
                    ),
                )
            assertThat(flytteSelv.vilkår.single().fakta)
                .isEqualTo(
                    VilkårFaktaFlytteSelvInterntVedtak(
                        erUtgifterDokumentert = dokumentert,
                        avstandEnVei = 100,
                        henger = 200,
                        bompenger = 300,
                        ferge = 400,
                        parkering = 500,
                        nødvendigUtgift = flytteSelvFakta.nødvendigUtgift,
                        adresse = "Flytteveien 2",
                    ),
                )
            assertThat(ubestemt.vilkår.single().fakta)
                .isEqualTo(VilkårFaktaFlyttingUbestemtInterntVedtak(adresse = "Flytteveien 3"))

            val resultat =
                flyttebyrå.beregningsresultat
                    ?.flytting
                    ?.resultater
                    ?.single()
            assertThat(resultat?.fraTidligereVedtak).isTrue()
            assertThat(resultat?.fom).isEqualTo(dato)
            assertThat(resultat?.tom).isEqualTo(dato)
            assertThat(resultat?.beløp).isEqualByComparingTo("1000")
            assertThat(resultat?.målgruppe).isEqualTo(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)
            assertThat(resultat?.grunnlag).isEqualTo(beregningsresultat.resultater.single().grunnlag)
        }
    }

    @Test
    fun `mapper avslag og opphør for flytting`() {
        listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR).forEach { stønadstype ->
            val avslag =
                lagInterntVedtak(
                    stønadstype,
                    fakta = null,
                    vedtak =
                        GeneriskVedtak(
                            behandlingId = behandlingId,
                            data = AvslagFlytting(årsaker = listOf(ÅrsakAvslag.ANNET), begrunnelse = "Avslagsgrunn"),
                            gitVersjon = Applikasjonsversjon.versjon,
                            tidligsteEndring = null,
                        ),
                )
            val opphør =
                lagInterntVedtak(
                    stønadstype,
                    fakta = null,
                    vedtak =
                        GeneriskVedtak(
                            behandlingId = behandlingId,
                            data =
                                OpphørFlytting(
                                    vedtaksperioder = emptyList(),
                                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                                    beregningsresultat = BeregningsresultatFlytting(emptyList()),
                                    årsaker = listOf(ÅrsakOpphør.ANNET),
                                    begrunnelse = "Opphørsgrunn",
                                ),
                            gitVersjon = Applikasjonsversjon.versjon,
                            tidligsteEndring = null,
                        ),
                )

            assertThat(avslag.vedtak?.type).isEqualTo(TypeVedtak.AVSLAG)
            assertThat(opphør.vedtak?.type).isEqualTo(TypeVedtak.OPPHØR)
            assertThat(opphør.beregningsresultat?.flytting?.resultater).isEmpty()
        }
    }

    private fun lagInterntVedtak(
        stønadstype: Stønadstype,
        fakta: VilkårFakta?,
        vedtak: GeneriskVedtak<out Vedtaksdata>,
    ): InterntVedtak {
        val behandling =
            saksbehandling(fagsak = fagsak(stønadstype = stønadstype), id = behandlingId, resultat = BehandlingResultat.INNVILGET)
        val behandlingService = mockk<BehandlingService>()
        val barnService = mockk<BarnService>()
        val faktaGrunnlagService = mockk<FaktaGrunnlagService>()
        val totrinnskontrollService = mockk<TotrinnskontrollService>()
        val vilkårperiodeService = mockk<VilkårperiodeService>()
        val søknadService = mockk<SøknadService>()
        val vilkårService = mockk<VilkårService>()
        val vedtakService = mockk<VedtakService>()
        val behandlingVilkår =
            fakta?.let {
                listOf(
                    vilkår(
                        behandlingId = behandlingId,
                        type = VilkårType.FLYTTING,
                        resultat = Vilkårsresultat.OPPFYLT,
                        fom = dato,
                        tom = dato,
                        utgift = null,
                        fakta = it,
                    ),
                )
            } ?: emptyList()

        every { behandlingService.hentSaksbehandling(behandlingId) } returns behandling
        every { vedtakService.hentVedtak(behandlingId) } returns vedtak
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = emptyList()))
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns behandlingVilkår
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Vilkårperioder(emptyList(), emptyList())
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns null
        every { søknadService.hentSøknadMetadata(behandlingId) } returns null

        return InterntVedtakService(
            behandlingService = behandlingService,
            barnService = barnService,
            faktaGrunnlagService = faktaGrunnlagService,
            totrinnskontrollService = totrinnskontrollService,
            vilkårperiodeService = vilkårperiodeService,
            søknadService = søknadService,
            vilkårService = vilkårService,
            vedtakService = vedtakService,
        ).lagInterntVedtak(behandlingId)
    }

    private fun beregningsresultat() =
        BeregningsresultatFlytting(
            resultater =
                listOf(
                    BeregningsresultatFlyttevilkår(
                        fraTidligereVedtak = true,
                        fom = dato,
                        tom = dato,
                        beløp = BigDecimal("1000"),
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        grunnlag =
                            BeregningsgrunnlagFlyttebyrå(
                                tilbud =
                                    listOf(
                                        BeregningsgrunnlagFlyttebyråTilbud("Byrå A", BigDecimal("1200"), false),
                                        BeregningsgrunnlagFlyttebyråTilbud("Byrå B", BigDecimal("1000"), true),
                                    ),
                                erUtgifterDokumentert = true,
                            ),
                    ),
                ),
        )

    private fun innvilgelse(beregningsresultat: BeregningsresultatFlytting) =
        GeneriskVedtak(
            behandlingId = behandlingId,
            data =
                InnvilgelseFlytting(
                    vedtaksperioder = emptyList(),
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    beregningsresultat = beregningsresultat,
                    begrunnelse = "Innvilget",
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
        )
}
