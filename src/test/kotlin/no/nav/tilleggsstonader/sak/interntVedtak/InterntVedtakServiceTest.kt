package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetBarnetilsynFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetBoutgifterFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetLæremidlerFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.MålgruppeFaktaOgVurderingerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY

class InterntVedtakServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val søknadService = mockk<SøknadService>()
    private val faktaGrunnlagService = mockk<FaktaGrunnlagService>()
    private val barnService = mockk<BarnService>()
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()

    val service =
        InterntVedtakService(
            behandlingService = behandlingService,
            totrinnskontrollService = totrinnskontrollService,
            vilkårperiodeService = vilkårperiodeService,
            søknadService = søknadService,
            faktaGrunnlagService = faktaGrunnlagService,
            barnService = barnService,
            vilkårService = vilkårService,
            vedtakService = vedtakService,
        )

    @BeforeEach
    fun setUp() {
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns Testdata.totrinnskontroll
        every { søknadService.hentSøknadMetadata(behandlingId) } returns Testdata.søknadMetadata
    }

    @Nested
    inner class TilsynBarn {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.TilsynBarn.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.TilsynBarn.vilkårperioder
            every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns Testdata.TilsynBarn.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns Testdata.TilsynBarn.behandlingBarn
            every { vilkårService.hentVilkår(behandlingId) } returns Testdata.TilsynBarn.vilkår
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.TilsynBarn.vedtak
        }

        @Test
        fun `behandlingsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            with(interntVedtak.behandling) {
                assertThat(behandlingId).isEqualTo(Testdata.behandlingId)
                assertThat(eksternFagsakId).isEqualTo(1673L)
                assertThat(stønadstype).isEqualTo(Stønadstype.BARNETILSYN)
                assertThat(årsak).isEqualTo(Testdata.TilsynBarn.behandling.årsak)
                assertThat(ident).isEqualTo(Testdata.TilsynBarn.behandling.ident)
                assertThat(opprettetTidspunkt).isEqualTo(Testdata.TilsynBarn.behandling.opprettetTid)
                assertThat(resultat).isEqualTo(Testdata.TilsynBarn.behandling.resultat)
                assertThat(vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
                assertThat(saksbehandler).isEqualTo("saksbehandler")
                assertThat(beslutter).isEqualTo("saksbeh2")
            }
        }

        @Test
        fun `søknadsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.søknad!!.mottattTidspunkt).isEqualTo(Testdata.søknadMetadata.mottattTidspunkt)
        }

        @Test
        fun `målgruppefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertThat(interntVedtak.målgrupper).hasSize(3)

            val målgruppe =
                Testdata.TilsynBarn.vilkårperioder.målgrupper
                    .single { it.type == MålgruppeType.AAP }

            with(interntVedtak.målgrupper.single { it.type == MålgruppeType.AAP }) {
                assertThat(type).isEqualTo(MålgruppeType.AAP)
                assertThat(fom).isEqualTo(målgruppe.fom)
                assertThat(tom).isEqualTo(målgruppe.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(begrunnelse).isEqualTo("målgruppe aap")
                with((faktaOgVurderinger as MålgruppeFaktaOgVurderingerDto).medlemskap!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
            }
        }

        @Test
        fun `aktivitetsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertThat(interntVedtak.aktiviteter).hasSize(2)

            val aktivitet =
                Testdata.TilsynBarn.vilkårperioder.aktiviteter
                    .single { it.resultat != ResultatVilkårperiode.SLETTET }

            with(interntVedtak.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }) {
                assertThat(type).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(aktivitet.fom)
                assertThat(tom).isEqualTo(aktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(begrunnelse).isEqualTo("aktivitet abd")
                with((faktaOgVurderinger as AktivitetBarnetilsynFaktaOgVurderingerDto).lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                }
            }
            val aktivitetSlettet =
                Testdata.TilsynBarn.vilkårperioder.aktiviteter
                    .single { it.resultat == ResultatVilkårperiode.SLETTET }
            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }) {
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
                assertThat(slettetKommentar).isEqualTo(aktivitetSlettet.slettetKommentar)
            }
        }

        @Test
        fun `beregningsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            val forventet =
                Testdata.TilsynBarn.vedtak.data.beregningsresultat.perioder
                    .single()

            with(interntVedtak.beregningsresultat!!.tilsynBarn!!.single()) {
                assertThat(månedsbeløp).isEqualTo(forventet.månedsbeløp)
                assertThat(dagsats).isEqualTo(forventet.dagsats)
                assertThat(grunnlag.måned).isEqualTo(forventet.grunnlag.måned)
                assertThat(grunnlag.utgifterTotal).isEqualTo(forventet.grunnlag.utgifterTotal)
                assertThat(grunnlag.antallBarn).isEqualTo(forventet.grunnlag.antallBarn)
            }
        }

        @Test
        fun `vedtaksperiodefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.vedtaksperioder).hasSize(1)
            with(interntVedtak.vedtaksperioder.single()) {
                assertThat(målgruppe).isEqualTo(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)
                assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 1))
            }
        }
    }

    @Nested
    inner class Læremidler {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.Læremidler.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.Læremidler.vilkårperioder
            every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns Testdata.Læremidler.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.Læremidler.innvilgetVedtak
        }

        @Test
        fun `aktivitetsfelter spesifikke for læremidler skal mappes riktig`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.aktiviteter).hasSize(2)

            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.OPPFYLT }) {
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                with(faktaOgVurderinger as AktivitetLæremidlerFaktaOgVurderingerDto) {
                    assertThat(harUtgifter!!.svar).isEqualTo(SvarJaNei.JA)
                    assertThat(harUtgifter.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                    assertThat(harRettTilUtstyrsstipend!!.svar).isEqualTo(SvarJaNei.NEI)
                    assertThat(harRettTilUtstyrsstipend.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                    assertThat(prosent).isEqualTo(80)
                    assertThat(studienivå).isEqualTo(Studienivå.HØYERE_UTDANNING)
                }
            }
            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(type).isEqualTo(AktivitetType.UTDANNING)
                with(faktaOgVurderinger as AktivitetLæremidlerFaktaOgVurderingerDto) {
                    assertThat(harRettTilUtstyrsstipend!!.svar).isEqualTo(SvarJaNei.JA)
                    assertThat(harRettTilUtstyrsstipend.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                    assertThat(harUtgifter!!.svar).isEqualTo(SvarJaNei.JA)
                    assertThat(harUtgifter.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                    assertThat(prosent).isEqualTo(80)
                    assertThat(studienivå).isEqualTo(Studienivå.VIDEREGÅENDE)
                }
            }
        }

        @Test
        fun `beregningsfelter skal bli riktig mappet`() {
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.Læremidler.innvilgetVedtak

            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            val forventet =
                Testdata.Læremidler.beregningsresultat
                    .tilDto(null)
                    .perioder
                    .single()

            with(interntVedtak.beregningsresultat!!.læremidler!!.single()) {
                assertThat(fom).isEqualTo(forventet.fom)
                assertThat(tom).isEqualTo(forventet.tom)
                assertThat(antallMåneder).isEqualTo(forventet.antallMåneder)
                assertThat(stønadsbeløpPerMåned).isEqualTo(forventet.stønadsbeløpPerMåned)
                assertThat(stønadsbeløpForPeriode).isEqualTo(forventet.stønadsbeløpForPeriode)
                assertThat(utbetalingsdato).isEqualTo(forventet.utbetalingsdato)
            }
        }

        @Test
        fun `Vedtak avslag skal mappes riktig`() {
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.Læremidler.avslåttVedtak
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            val dummyAvslåttVedtak = Testdata.Læremidler.avslåttVedtak.data

            assertThat(interntVedtak.vedtak!!).isEqualTo(
                VedtakAvslagInternt(
                    årsakerAvslag = dummyAvslåttVedtak.årsaker,
                    avslagBegrunnelse = dummyAvslåttVedtak.begrunnelse,
                ),
            )
        }

        @Test
        fun `vedtaksperiodefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.vedtaksperioder).hasSize(1)
            with(interntVedtak.vedtaksperioder.single()) {
                assertThat(målgruppe).isEqualTo(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)
                assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
            }
        }
    }

    @Nested
    inner class Boutgifter {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.Boutgifter.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.Boutgifter.vilkårperioder
            every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns Testdata.Boutgifter.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
            every { vilkårService.hentVilkår(behandlingId) } returns Testdata.Boutgifter.vilkår
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.Boutgifter.innvilgetVedtak
        }

        @Test
        fun `behandlingsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId)

            with(interntVedtak.behandling) {
                assertThat(behandlingId).isEqualTo(Testdata.behandlingId)
                assertThat(eksternFagsakId).isEqualTo(1673L)
                assertThat(stønadstype).isEqualTo(Stønadstype.BOUTGIFTER)
                assertThat(årsak).isEqualTo(Testdata.Boutgifter.behandling.årsak)
                assertThat(ident).isEqualTo(Testdata.Boutgifter.behandling.ident)
                assertThat(opprettetTidspunkt).isEqualTo(Testdata.Boutgifter.behandling.opprettetTid)
                assertThat(resultat).isEqualTo(Testdata.Boutgifter.behandling.resultat)
                assertThat(vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
                assertThat(saksbehandler).isEqualTo("saksbehandler")
                assertThat(beslutter).isEqualTo("saksbeh2")
            }
        }

        @Test
        fun `søknadsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId)

            assertThat(interntVedtak.søknad!!.mottattTidspunkt).isEqualTo(Testdata.søknadMetadata.mottattTidspunkt)
        }

        @Test
        fun `målgruppefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertThat(interntVedtak.målgrupper).hasSize(3)

            val målgruppe =
                Testdata.Boutgifter.vilkårperioder.målgrupper
                    .single { it.type == MålgruppeType.AAP }

            with(interntVedtak.målgrupper.single { it.type == MålgruppeType.AAP }) {
                assertThat(type).isEqualTo(MålgruppeType.AAP)
                assertThat(fom).isEqualTo(målgruppe.fom)
                assertThat(tom).isEqualTo(målgruppe.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(begrunnelse).isEqualTo("målgruppe aap")
                with((faktaOgVurderinger as MålgruppeFaktaOgVurderingerDto).medlemskap!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
            }
        }

        @Test
        fun `aktivitetsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertThat(interntVedtak.aktiviteter).hasSize(2)

            val oppfyltAktivitet =
                Testdata.Boutgifter.vilkårperioder.aktiviteter
                    .single { it.resultat == ResultatVilkårperiode.OPPFYLT }

            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.OPPFYLT }) {
                assertThat(type).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(oppfyltAktivitet.fom)
                assertThat(tom).isEqualTo(oppfyltAktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(begrunnelse).isNull()
                with((faktaOgVurderinger as AktivitetBoutgifterFaktaOgVurderingerDto).lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.NEI)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
            }

            val ikkeOppfyltAktivitet =
                Testdata.Boutgifter.vilkårperioder.aktiviteter
                    .single { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }
            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
                assertThat(type).isEqualTo(AktivitetType.UTDANNING)
                assertThat(fom).isEqualTo(ikkeOppfyltAktivitet.fom)
                assertThat(tom).isEqualTo(ikkeOppfyltAktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(begrunnelse).isEqualTo("ikke oppfylt")
                with((faktaOgVurderinger as AktivitetBoutgifterFaktaOgVurderingerDto).lønnet) {
                    assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                }
            }
        }

        @Test
        fun `beregningsfelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.beregningsresultat!!.boutgifter!!.size).isEqualTo(2)

            val forventet =
                Testdata.Boutgifter.innvilgetVedtak.data.beregningsresultat.perioder
                    .first()

            with(interntVedtak.beregningsresultat.boutgifter.first()) {
                assertThat(fom).isEqualTo(forventet.fom)
                assertThat(tom).isEqualTo(forventet.tom)
                assertThat(målgruppe).isEqualTo(forventet.grunnlag.målgruppe)
                assertThat(aktivitet).isEqualTo(forventet.grunnlag.aktivitet)
            }
        }

        @Test
        fun `vedtaksperiodefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.vedtaksperioder).hasSize(1)
            with(interntVedtak.vedtaksperioder.single()) {
                assertThat(målgruppe).isEqualTo(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)
                assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(LocalDate.of(2024, JANUARY, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, FEBRUARY, 29))
            }
        }

        @Test
        fun `Vedtak avslag skal mappes riktig`() {
            val dummyAvslåttVedtak = Testdata.Boutgifter.avslåttVedtak
            every { vedtakService.hentVedtak(behandlingId) } returns dummyAvslåttVedtak
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.vedtak!!).isEqualTo(
                VedtakAvslagInternt(
                    årsakerAvslag = dummyAvslåttVedtak.data.årsaker,
                    avslagBegrunnelse = dummyAvslåttVedtak.data.begrunnelse,
                ),
            )
        }

        @Test
        fun `Opphør skal mappes riktig`() {
            val dummyOpphørtVedtak = Testdata.Boutgifter.opphørtVedtak
            every { vedtakService.hentVedtak(behandlingId) } returns dummyOpphørtVedtak
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.vedtak!!).isEqualTo(
                VedtakOpphørInternt(
                    årsakerOpphør = dummyOpphørtVedtak.data.årsaker,
                    opphørBegrunnelse = dummyOpphørtVedtak.data.begrunnelse,
                ),
            )
        }
    }
}
