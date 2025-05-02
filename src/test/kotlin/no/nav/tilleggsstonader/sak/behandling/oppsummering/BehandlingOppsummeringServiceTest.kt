package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.avslagVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BehandlingOppsummeringServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingOppsummeringService: BehandlingOppsummeringService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal returnere false på at det finnes data å oppsummere om behandling ikke inneholder noe data`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

        assertThat(behandlingOppsummering.finnesDataÅOppsummere).isFalse()
        assertThat(behandlingOppsummering.aktiviteter).isEmpty()
        assertThat(behandlingOppsummering.målgrupper).isEmpty()
        assertThat(behandlingOppsummering.vilkår).isEmpty()
    }

    @Nested
    inner class OppsummeringVilkårperioder {
        @Test
        fun `skal slå sammen sammenhengende perioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 12),
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.målgrupper).hasSize(1)
            assertThat(behandlingOppsummering.målgrupper[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
            assertThat(behandlingOppsummering.målgrupper[0].tom).isEqualTo(LocalDate.of(2025, 1, 31))
        }

        @Test
        fun `skal ikke slå sammen sammenhengende perioder med ulike typer eller resultat`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 12),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.målgrupper).hasSize(3)
        }
    }

    @Nested
    inner class OppsummeringStønadsvilkår {
        @Test
        fun `skal slå sammen sammenhengende vilkår med like verdier`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
            val barn1 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.vilkår).hasSize(1)
            assertThat(behandlingOppsummering.vilkår[0].barnId).isEqualTo(barn1)
            assertThat(behandlingOppsummering.vilkår[0].vilkår).hasSize(1)
        }

        @Test
        fun `skal ikke slå sammen vilkår for to ulike barn`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
            val barn1 = BarnId.random()
            val barn2 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn2,
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.vilkår).hasSize(2)
        }
    }

    @Nested
    inner class OppsummerVedtak {
        @Test
        fun `skal inneholde vedtaksperioder dersom det er en innvilgelse`() {
            val vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                        målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
                        aktivitet = AktivitetType.UTDANNING,
                    ),
                )
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vedtakRepository.insert(
                innvilgetVedtak(behandlingId = behandling.id, vedtaksperioder = vedtaksperioder),
            )

            val behandlingsoppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingsoppsummering.vedtak).isInstanceOf(OppsummertVedtakInnvilgelse::class.java)

            val oppsummertInnvilgelse = behandlingsoppsummering.vedtak as OppsummertVedtakInnvilgelse

            assertThat(oppsummertInnvilgelse.vedtaksperioder).isEqualTo(vedtaksperioder.map { it.tilDto() })
        }

        @Test
        fun `skal inneholde årsaker dersom det er et avslag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val årsakerAvslag = listOf(ÅrsakAvslag.INGEN_AKTIVITET, ÅrsakAvslag.IKKE_I_MÅLGRUPPE)

            vedtakRepository.insert(
                avslagVedtak(behandlingId = behandling.id, årsaker = årsakerAvslag, begrunnelse = "begrunnelse"),
            )

            val behandlingsoppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingsoppsummering.vedtak).isInstanceOf(OppsummertVedtakAvslag::class.java)
            assertThat((behandlingsoppsummering.vedtak as OppsummertVedtakAvslag).årsaker).isEqualTo(årsakerAvslag)
        }

        @Test
        fun `skal inneholde årsaker dersom det er et opphør`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_AKTIVITET)

            vedtakRepository.insert(
                opphørVedtak(behandlingId = behandling.id, årsaker = årsakerOpphør, begrunnelse = "begrunnelse"),
            )

            val behandlingsoppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingsoppsummering.vedtak).isInstanceOf(OppsummertVedtakOpphør::class.java)
            assertThat((behandlingsoppsummering.vedtak as OppsummertVedtakOpphør).årsaker).isEqualTo(årsakerOpphør)
        }
    }

    @Nested
    inner class AvkortVedRevurderFra {
        @Test
        fun `skal kutte perioder fra revurderFra datoen`() {
            val behandling = testoppsettService.lagBehandlingOgRevurdering(revurderFra = LocalDate.of(2025, 1, 1))
            vilkårperiodeRepository.insert(
                aktivitet(
                    behandlingId = behandling.id,
                    fom = LocalDate.of(2024, 8, 1),
                    tom = LocalDate.of(2025, 6, 30),
                ),
            )

            val oppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)
            assertThat(oppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(oppsummering.aktiviteter).hasSize(1)
            assertThat(oppsummering.aktiviteter[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
        }

        @Test
        fun `skal kutte vilkår av type PASSS_BARN fra første dag i samme måned som revurderFra`() {
            val revurderFra = LocalDate.of(2025, 1, 3)
            val behandling = testoppsettService.lagBehandlingOgRevurdering(revurderFra = revurderFra)
            val barn1 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                ),
            )

            val oppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)
            assertThat(oppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(oppsummering.vilkår).hasSize(1)
            assertThat(oppsummering.vilkår[0].vilkår[0].fom).isEqualTo(revurderFra.tilFørsteDagIMåneden())
        }

        @Test
        fun `skal kutte vilkår for boutgifter enten fra starten av måneden eller revurder fra`() {
            val revurderFra = LocalDate.of(2025, 1, 14)
            val behandling = testoppsettService.lagBehandlingOgRevurdering(revurderFra = revurderFra)
            val barn1 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.UTGIFTER_OVERNATTING,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 12),
                        tom = LocalDate.of(2025, 1, 15),
                    ),
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                ),
            )

            val oppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)
            assertThat(oppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(oppsummering.vilkår).hasSize(2)

            val overnattingVilkår = oppsummering.vilkår.find { it.type == VilkårType.UTGIFTER_OVERNATTING }
            val løpendeUtgifterEnBoligVilkår = oppsummering.vilkår.find { it.type == VilkårType.LØPENDE_UTGIFTER_EN_BOLIG }

            assertThat(overnattingVilkår!!.vilkår[0].fom).isEqualTo(revurderFra)
            assertThat(løpendeUtgifterEnBoligVilkår!!.vilkår[0].fom).isEqualTo(revurderFra.tilFørsteDagIMåneden())
        }
    }
}
