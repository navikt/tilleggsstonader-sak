package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetBarnetilsynFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.MålgruppeFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InterntVedtakServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val stønadsperiodeService = mockk<StønadsperiodeService>()
    private val søknadService = mockk<SøknadService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val barnService = mockk<BarnService>()
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()

    val service = InterntVedtakService(
        behandlingService = behandlingService,
        totrinnskontrollService = totrinnskontrollService,
        vilkårperiodeService = vilkårperiodeService,
        stønadsperiodeService = stønadsperiodeService,
        søknadService = søknadService,
        grunnlagsdataService = grunnlagsdataService,
        barnService = barnService,
        vilkårService = vilkårService,
        vedtakService = vedtakService,
    )

    @BeforeEach
    fun setUp() {
        every { stønadsperiodeService.hentStønadsperioder(behandlingId) } returns Testdata.stønadsperioder
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns Testdata.totrinnskontroll
        every { søknadService.hentSøknadMetadata(behandlingId) } returns Testdata.søknadMetadata
    }

    @Nested
    inner class TilsynBarn {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.TilsynBarn.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.TilsynBarn.vilkårperioder
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns Testdata.TilsynBarn.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns Testdata.TilsynBarn.behandlingBarn
            every { vilkårService.hentVilkårsett(behandlingId) } returns Testdata.TilsynBarn.vilkår
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
            assertThat(interntVedtak.målgrupper).hasSize(2)

            val målgruppe =
                Testdata.TilsynBarn.vilkårperioder.målgrupper.single { it.type == MålgruppeType.AAP }

            with(interntVedtak.målgrupper.single { it.type == MålgruppeType.AAP }) {
                assertThat(type).isEqualTo(MålgruppeType.AAP)
                assertThat(fom).isEqualTo(målgruppe.fom)
                assertThat(tom).isEqualTo(målgruppe.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(begrunnelse).isEqualTo("målgruppe aap")
                with(delvilkår.medlemskap!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT.name)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
                assertThat(delvilkår.lønnet).isNull()
                with<VurderingDto, ObjectAssert<ResultatDelvilkårperiode?>?>((faktaOgVurderinger as MålgruppeFaktaOgVurderingerDto).medlemskap!!) {
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
                Testdata.TilsynBarn.vilkårperioder.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }

            with(interntVedtak.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }) {
                assertThat(type).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(aktivitet.fom)
                assertThat(tom).isEqualTo(aktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(begrunnelse).isEqualTo("aktivitet abd")
                with(delvilkår.lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA.name)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                }
                assertThat(delvilkår.medlemskap).isNull()
                with<VurderingDto, ObjectAssert<ResultatDelvilkårperiode?>?>((faktaOgVurderinger as AktivitetBarnetilsynFaktaOgVurderingerDto).lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                }
            }
            val aktivitetSlettet =
                Testdata.TilsynBarn.vilkårperioder.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }
            with(interntVedtak.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }) {
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
                assertThat(slettetKommentar).isEqualTo(aktivitetSlettet.slettetKommentar)
            }
        }

        @Test
        fun `stønadsperiodefelter skal bli riktig mappet`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)

            assertThat(interntVedtak.stønadsperioder).hasSize(2)
            with(interntVedtak.stønadsperioder.first()) {
                assertThat(målgruppe).isEqualTo(MålgruppeType.AAP)
                assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
            }
            with(interntVedtak.stønadsperioder.last()) {
                assertThat(målgruppe).isEqualTo(MålgruppeType.NEDSATT_ARBEIDSEVNE)
                assertThat(aktivitet).isEqualTo(AktivitetType.REELL_ARBEIDSSØKER)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
            }
        }

    }

    @Nested
    inner class Læremidler {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.TilsynBarn.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.TilsynBarn.vilkårperioder
            every { stønadsperiodeService.hentStønadsperioder(behandlingId) } returns Testdata.stønadsperioder
            every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns Testdata.totrinnskontroll
            every { søknadService.hentSøknadMetadata(behandlingId) } returns Testdata.søknadMetadata
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns Testdata.TilsynBarn.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns Testdata.TilsynBarn.behandlingBarn
            every { vilkårService.hentVilkårsett(behandlingId) } returns Testdata.TilsynBarn.vilkår
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.TilsynBarn.vedtak
        }

        @Test
        fun `felter skal bli riktig mappede`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertBehandling(interntVedtak.behandling)
            assertSøknad(interntVedtak.søknad)
            assertMålgrupper(interntVedtak.målgrupper)
            assertAktiviteter(interntVedtak.aktiviteter)
            assertStønadsperioder(interntVedtak.stønadsperioder)
        }

        private fun assertStønadsperioder(stønadsperioder: List<Stønadsperiode>) {
            assertThat(stønadsperioder).hasSize(2)
            with(stønadsperioder.first()) {
                assertThat(målgruppe).isEqualTo(MålgruppeType.AAP)
                assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
            }
            with(stønadsperioder.last()) {
                assertThat(målgruppe).isEqualTo(MålgruppeType.NEDSATT_ARBEIDSEVNE)
                assertThat(aktivitet).isEqualTo(AktivitetType.REELL_ARBEIDSSØKER)
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
            }
        }

        private fun assertBehandling(behandlinginfo: Behandlinginfo) {
            with(behandlinginfo) {
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

        private fun assertSøknad(søknad: Søknadsinformasjon?) {
            with(søknad!!) {
                assertThat(mottattTidspunkt).isEqualTo(søknad.mottattTidspunkt)
            }
        }

        private fun assertMålgrupper(målgrupper: List<VilkårperiodeInterntVedtak>) {
            assertThat(målgrupper).hasSize(2)

            val målgruppe =
                Testdata.TilsynBarn.vilkårperioder.målgrupper.single { it.type == MålgruppeType.AAP }
            with(målgrupper.single { it.type == MålgruppeType.AAP }) {
                assertThat(type).isEqualTo(MålgruppeType.AAP)
                assertThat(fom).isEqualTo(målgruppe.fom)
                assertThat(tom).isEqualTo(målgruppe.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
                assertThat(begrunnelse).isEqualTo("målgruppe aap")
                with(delvilkår.medlemskap!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT.name)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
                assertThat(delvilkår.lønnet).isNull()
                with((faktaOgVurderinger as MålgruppeFaktaOgVurderingerDto).medlemskap!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
                }
            }
        }

        private fun assertAktiviteter(aktiviteter: List<VilkårperiodeInterntVedtak>) {
            assertThat(aktiviteter).hasSize(2)
            val aktivitet =
                Testdata.TilsynBarn.vilkårperioder.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }
            with(aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }) {
                assertThat(type).isEqualTo(AktivitetType.TILTAK)
                assertThat(fom).isEqualTo(aktivitet.fom)
                assertThat(tom).isEqualTo(aktivitet.tom)
                assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                assertThat(begrunnelse).isEqualTo("aktivitet abd")
                with(delvilkår.lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA.name)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                }
                assertThat(delvilkår.medlemskap).isNull()
                with((faktaOgVurderinger as AktivitetBarnetilsynFaktaOgVurderingerDto).lønnet!!) {
                    assertThat(svar).isEqualTo(SvarJaNei.JA)
                    assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
                }
            }

            val aktivitetSlettet =
                Testdata.TilsynBarn.vilkårperioder.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }
            with(aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }) {
                assertThat(resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
                assertThat(slettetKommentar).isEqualTo(aktivitetSlettet.slettetKommentar)
            }
        }
    }
}
