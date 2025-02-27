package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Fødsel
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Navn
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValideringUtils.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.mergeSammenhengendeOppfylteVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TilsynBarnVedtaksperiodeValidingerUtilsTest {
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val tilsynBarnVedtaksperiodeValidingerService =
        TilsynBarnVedtaksperiodeValidingerService(
            vilkårperiodeService = vilkårperiodeService,
            grunnlagsdataService = grunnlagsdataService,
            vedtakRepository = vedtakRepository,
        )

    val behandling = saksbehandling()

    val målgrupper =
        listOf(
            målgruppe(
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )
    val aktiviteter =
        listOf(
            aktivitet(
                faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )

    val utgifter: Map<BarnId, List<UtgiftBeregning>> =
        mapOf(
            BarnId.random() to
                listOf(
                    UtgiftBeregning(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 2),
                        utgift = 1000,
                    ),
                ),
        )

    @Nested
    inner class TilsynBarnVedtaksperiodeValidingerService {
        @BeforeEach
        fun setup() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns lagGrunnlagsdata()
        }

        @Test
        fun `skal ikke kaste feil for gyldig vedtaksperiode`() {
            val vedtaksperiode = lagVedtaksperiode()

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder = listOf(vedtaksperiode),
                    behandling = behandling,
                    utgifter = utgifter,
                )
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class ValiderEnkeltperiode {
        @Test
        fun `skal kaste feil om kombinasjon av målgruppe og aktivitet er ugyldig`() {
            val vedtaksperiode =
                lagVedtaksperiode(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.TILTAK)

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining("Kombinasjonen av OVERGANGSSTØNAD og TILTAK er ikke gyldig")
        }

        @Test
        fun `skal kaste feil om ingen periode for målgruppe matcher`() {
            val vedtaksperiode = lagVedtaksperiode(målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE)

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for NEDSATT_ARBEIDSEVNE er oppfylt")
        }

        @Test
        fun `skal kaste feil om ingen periode for aktivitet matcher`() {
            val vedtaksperiode = lagVedtaksperiode(aktivitet = AktivitetType.UTDANNING)

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for UTDANNING er oppfylt")
        }

        @Test
        fun `skal kaste feil om vedtaksperiode er utenfor målgruppeperiode`() {
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2024, 12, 1))

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for AAP i perioden 01.12.2024 - 31.01.2025",
            )
        }

        @Test
        fun `skal kaste feil om vedtaksperiode er utenfor aktivitetsperiode`() {
            val vedtaksperiode = lagVedtaksperiode()

            val aktiviteter =
                listOf(
                    aktivitet(
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 15),
                    ),
                )

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for TILTAK i perioden 01.01.2025 - 31.01.2025",
            )
        }

        @Test
        fun `skal ikke kaste feil dersom vedtaksperiode går på tvers av to sammengengdende vilkårsperioder`() {
            val vedtaksperiode = lagVedtaksperiode()

            val aktiviteter =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 10),
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                    ),
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 11),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                    ),
                )

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke kaste feil dersom vedtaksperiode går på tvers av to delvis overlappende vilkårsperioder`() {
            val vedtaksperiode = lagVedtaksperiode()
            val aktiviteter =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 10),
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                    ),
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 7),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                    ),
                )

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class ValiderVedtaksperioderMedMergeSammenhengendeVilkårperioder {
        val målgrupper =
            listOf(
                målgruppe(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 7),
                ),
                målgruppe(
                    fom = LocalDate.of(2025, 1, 8),
                    tom = LocalDate.of(2025, 1, 18),
                ),
                målgruppe(
                    fom = LocalDate.of(2025, 1, 20),
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )

        val aktiviteter =
            målgrupper.map {
                aktivitet(
                    fom = it.fom,
                    tom = it.tom,
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetTilsynBarn(
                            type = AktivitetType.TILTAK,
                            lønnet = VurderingLønnet(SvarJaNei.NEI),
                        ),
                )
            }

        @Test
        fun `skal godta stønadsperiode på tvers av 2 godkjente sammenhengende vilkårsperioder`() {
            val vedtaksperiode =
                lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 10))

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta stønadsperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val vedtaksperiode =
                lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 21))

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                    fødselsdato = null,
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for AAP i perioden 01.01.2025 - 21.01.2025",
            )
        }
    }

    @Nested
    inner class ValiderIngenOverlapp {
        val vedtaksperiodeJan =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
            )
        val vedtaksperiodeFeb =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 28),
            )
        val vedtaksperiodeJanFeb =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            )

        @Test
        fun `kaster feil hvis vedtaksperioderDto overlapper`() {
            val feil =
                assertThrows<ApiFeil> {
                    TilsynBarnVedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder(
                        listOf(
                            vedtaksperiodeJan,
                            vedtaksperiodeJanFeb,
                        ),
                    )
                }
            assertThat(feil.feil).contains("Vedtaksperioder kan ikke overlappe")
        }

        @Test
        fun `kaster ikke feil hvis vedtaksperioderDto ikke overlapper`() {
            assertDoesNotThrow {
                TilsynBarnVedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder(
                    listOf(
                        vedtaksperiodeJan,
                        vedtaksperiodeFeb,
                    ),
                )
            }
        }
    }

    @Nested
    inner class OverlappMedPeriodeSomIkkeGirRettPåStønad {
        val jan = YearMonth.of(2025, 1)
        val tilltakJan =
            aktivitet(
                faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                fom = jan.atDay(1),
                tom = jan.atDay(31),
            )
        val aapJan =
            målgruppe(
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                fom = jan.atDay(1),
                tom = jan.atDay(31),
            )

        @Test
        fun `kan ha vedtaksperiode før og etter periode som ikke gir rett på stønad`() {
            val målgrupper =
                listOf(
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                        fom = jan.atDay(1),
                        tom = jan.atDay(9),
                    ),
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.SYKEPENGER_100_PROSENT),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "asd",
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    ),
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "asd",
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    ),
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                        fom = jan.atDay(21),
                        tom = jan.atDay(31),
                    ),
                )

            val aktiviteter =
                listOf(
                    aktivitet(
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                        fom = jan.atDay(1),
                        tom = jan.atDay(9),
                    ),
                    aktivitet(
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.INGEN_AKTIVITET),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "asd",
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    ),
                    aktivitet(
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                        fom = jan.atDay(21),
                        tom = jan.atDay(31),
                    ),
                )

            val vilkårperioder =
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder =
                        listOf(
                            lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atDay(9)),
                            lagVedtaksperiode(fom = jan.atDay(21), tom = jan.atDay(31)),
                        ),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal kaste feil hvis en vedtaksperiode overlapper med 100 prosent sykemelding`() {
            val målgrupper =
                listOf(
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.SYKEPENGER_100_PROSENT),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "a",
                    ),
                )
            val vilkårperioder =
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = listOf(tilltakJan),
                )
            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atEndOfMonth())),
                )
            }.hasMessage(
                "Vedtaksperiode 01.01.2025 - 31.01.2025 overlapper med SYKEPENGER_100_PROSENT(10.01.2025 - 20.01.2025) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal kaste feil hvis en vedtaksperiode overlapper med INGEN_MÅLGRUPPE`() {
            val målgrupper =
                listOf(
                    målgruppe(
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "a",
                    ),
                )

            val vilkårperioder =
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = listOf(tilltakJan),
                )

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atDay(15))),
                )
            }.hasMessage(
                "Vedtaksperiode 01.01.2025 - 15.01.2025 overlapper med INGEN_MÅLGRUPPE(10.01.2025 - 20.01.2025) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal kaste feil hvis en vedtaksperiode overlapper med INGEN_AKTIVITET`() {
            val aktiviteter =
                listOf(
                    aktivitet(
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.INGEN_AKTIVITET),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "a",
                    ),
                )

            val vilkårperioder =
                Vilkårperioder(
                    målgrupper = listOf(aapJan),
                    aktiviteter = aktiviteter,
                )

            assertThatThrownBy {
                TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(15), tom = jan.atDay(15))),
                )
            }.hasMessage(
                "Vedtaksperiode 15.01.2025 - 15.01.2025 overlapper med INGEN_AKTIVITET(10.01.2025 - 20.01.2025) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal ikke kaste feil om vedtaksperiode overlapper med slettet vilkårperiode uten rett til stønad`() {
            val behandlingId = BehandlingId.random()

            val målgrupper =
                listOf(
                    målgruppe(
                        behandlingId = behandlingId,
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                        begrunnelse = "a",
                        resultat = ResultatVilkårperiode.SLETTET,
                        slettetKommentar = "slettet",
                    ),
                    målgruppe(
                        behandlingId = behandlingId,
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                    ),
                )

            val aktiviteter =
                listOf(
                    aktivitet(
                        behandlingId = behandlingId,
                        faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                        fom = jan.atDay(10),
                        tom = jan.atDay(20),
                    ),
                )

            val vilkårperioder =
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )

            assertThatCode {
                TilsynBarnVedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(10), tom = jan.atDay(20))),
                )
            }.doesNotThrowAnyException()
        }

        @Nested
        inner class ValideringAvFødselsdato {
            val fom = LocalDate.of(2025, 1, 1)
            val tom = LocalDate.of(2025, 1, 31)
            val vedtaksperioder = lagVedtaksperiode()

            val dato18årGammel = fom.minusYears(18)
            val dato67årGammel = tom.minusYears(67)

            @Test
            fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er over 18 år`() {
                assertThatCode {
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato18årGammel,
                    )
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato18årGammel.minusDays(1),
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `skal kaste feil dersom nedsatt arbeidsevne og personen er under 18 år`() {
                assertThatCode {
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato18årGammel.plusDays(1),
                    )
                }.hasMessageContaining("Periode kan ikke begynne før søker fyller 18 år")
            }

            @Test
            fun `skal kaste feil dersom nedsatt arbeidsevne og personen er over 67 år`() {
                assertThatThrownBy {
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato67årGammel,
                    )
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato67årGammel.minusDays(1),
                    )
                }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")
            }

            @Test
            fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er under 67 år`() {
                assertThatCode {
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato67årGammel.plusDays(1),
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `skal ikke kaste feil dersom overgangsstønad og under 18 år eller over 67 år`() {
                val vedtaksperioder =
                    lagVedtaksperiode(
                        målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                        aktivitet = AktivitetType.UTDANNING,
                    )

                val målgrupper =
                    listOf(
                        målgruppe(
                            faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 2, 28),
                        ),
                    )
                val aktiviteter =
                    listOf(
                        aktivitet(
                            faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.UTDANNING),
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 2, 28),
                        ),
                    )

                assertThatCode {
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato18årGammel.minusYears(1),
                    )
                    TilsynBarnVedtaksperiodeValideringUtils.validerEnkeltperiode(
                        vedtaksperioder,
                        målgrupper.mergeSammenhengendeOppfylteVilkårperioder(),
                        aktiviteter.mergeSammenhengendeOppfylteVilkårperioder(),
                        dato67årGammel.plusYears(1),
                    )
                }.doesNotThrowAnyException()
            }
        }

        @Nested
        inner class ValiderUtgifterHeleVedtaksperioden {
            val vedtaksperiode = lagVedtaksperiode()

            val utgifter: Map<BarnId, List<UtgiftBeregning>> =
                mapOf(
                    BarnId.random() to
                        listOf(
                            UtgiftBeregning(
                                fom = YearMonth.of(2025, 1),
                                tom = YearMonth.of(2025, 1),
                                utgift = 1000,
                            ),
                        ),
                )

            @Test
            fun `kaster ikke feil når utgift hele vedtaksperioden`() {
                assertDoesNotThrow {
                    TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden(
                        vedtaksperioder = listOf(vedtaksperiode),
                        utgifter = utgifter,
                    )
                }
            }

            @Test
            fun `kaster feil når det ikke finnes utgifter hele vedtaksperioden`() {
                val feil =
                    assertThrows<ApiFeil> {
                        TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden(
                            vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                            utgifter = utgifter,
                        )
                    }
                assertThat(feil.feil).contains("Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden")
            }

            @Test
            fun `kaster ikke feil når det utgifter hele vedtaksperioden fordelt i flere perioder`() {
                val utgifter: Map<BarnId, List<UtgiftBeregning>> =
                    mapOf(
                        BarnId.random() to
                            listOf(
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 1),
                                    tom = YearMonth.of(2025, 1),
                                    utgift = 1000,
                                ),
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 2),
                                    tom = YearMonth.of(2025, 2),
                                    utgift = 1000,
                                ),
                            ),
                    )

                Assertions.assertDoesNotThrow {
                    TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden(
                        vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                        utgifter = utgifter,
                    )
                }
            }

            @Test
            fun `kaster ikke feil når det utgifter hele vedtaksperioden fordelt på flere barn`() {
                val utgifter: Map<BarnId, List<UtgiftBeregning>> =
                    mapOf(
                        BarnId.random() to
                            listOf(
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 1),
                                    tom = YearMonth.of(2025, 1),
                                    utgift = 1000,
                                ),
                            ),
                        BarnId.random() to
                            listOf(
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 2),
                                    tom = YearMonth.of(2025, 2),
                                    utgift = 1000,
                                ),
                            ),
                    )

                Assertions.assertDoesNotThrow {
                    TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden(
                        vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                        utgifter = utgifter,
                    )
                }
            }

            @Test
            fun `kaster feil når det ikke finnes utgifter hele vedtaksperioden pga pause mellom utgiftene`() {
                val utgifter: Map<BarnId, List<UtgiftBeregning>> =
                    mapOf(
                        BarnId.random() to
                            listOf(
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 1),
                                    tom = YearMonth.of(2025, 1),
                                    utgift = 1000,
                                ),
                                UtgiftBeregning(
                                    fom = YearMonth.of(2025, 3),
                                    tom = YearMonth.of(2025, 3),
                                    utgift = 1000,
                                ),
                            ),
                    )
                val feil =
                    assertThrows<ApiFeil> {
                        TilsynBarnVedtaksperiodeValideringUtils.validerUtgiftHeleVedtaksperioden(
                            vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 3, 31))),
                            utgifter = utgifter,
                        )
                    }
                assertThat(feil.feil).contains("Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden")
            }
        }
    }

    @Nested
    inner class ValiderIngenEndringerFørRevurderFra {
        val vedtaksperiodeJanFeb =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            )

        val vedtaksperiodeMars =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 3, 1),
                tom = LocalDate.of(2025, 3, 31),
            )

        val vedtaksperiodeApril =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 4, 1),
                tom = LocalDate.of(2025, 4, 30),
            )

        val vedtaksperioderJanFeb = listOf(vedtaksperiodeJanFeb)
        val vedtaksperioderJanMars = listOf(vedtaksperiodeJanFeb, vedtaksperiodeMars)
        val førsteMars: LocalDate = LocalDate.of(2025, 3, 1)
        val femtendeMars: LocalDate = LocalDate.of(2025, 3, 15)
        val førsteApril: LocalDate = LocalDate.of(2025, 4, 1)

        @Test
        fun `kaster ikke feil ved ingen revurder fra og ingen gamle perioder (førstegangsbehandling)`() {
            assertDoesNotThrow {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioder = vedtaksperioderJanMars,
                    vedtaksperioderForrigeBehandling = emptyList(),
                    revurderFra = null,
                )
            }
        }

        @Nested
        inner class NyPeriode {
            @Test
            fun `kaster ikke feil ved ny periode som starter etter revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanMars,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved ny periode med fom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved ny periode med fom og tom før revuder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }
        }

        @Test
        fun `kaster feil ved ny periode som er lik eksisterende periode lagt til før revuder fra`() {
            val nyeVedtaksperioder =
                listOf(
                    vedtaksperiodeJanFeb,
                    vedtaksperiodeJanFeb.copy(id = UUID.randomUUID()),
                )

            val feil =
                assertThrows<ApiFeil> {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = nyeVedtaksperioder,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                        revurderFra = førsteMars,
                    )
                }
            assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
        }

        @Test
        fun `kaster feil ved nye perioder før revurder fra etter opphør med ingen eksisterende vedtaksperioder`() {
            val feil =
                assertThrows<ApiFeil> {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanMars,
                        vedtaksperioderForrigeBehandling = emptyList(),
                        revurderFra = førsteMars,
                    )
                }
            assertThat(feil).hasMessage("Det er ikke tillat å legge til nye perioder før revurder fra dato")
        }

        @Nested
        inner class EndretPeriode {
            @Test
            fun `kaster ikke feil ved fom før revurder fra og tom etter revurder fra, der tom flyttet fremover i tid`() {
                val nyeVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeMars.copy(tom = LocalDate.of(2025, 4, 10)),
                    )

                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = nyeVedtaksperioder,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                        revurderFra = femtendeMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved tom flyttet til før revurder fra`() {
                val nyeVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeMars.copy(tom = LocalDate.of(2025, 3, 10)),
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = nyeVedtaksperioder,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom flyttet til før revurder fra`() {
                val gamleVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeApril,
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = gamleVedtaksperioder,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der tom flyttes fremover i tid, men fortsatt før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 3, 31))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der tom flyttes fremover i tid forbi revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 5, 31))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der fom flyttes fremover i tid, men fortsatt før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(fom = LocalDate.of(2025, 1, 3))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster ikke feil når forlenger fom som er dagen før revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 3, 31))),
                        vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil når avkorter fom som er dagen før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(fom = LocalDate.of(2025, 2, 27))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster ikke feil når avkorter tom til dagen før revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 2, 28))),
                        vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 3, 31))),
                        revurderFra = førsteMars,
                    )
                }
            }
        }

        @Nested
        inner class SlettetPeriode {
            @Test
            fun `kaster ikke feil ved slettet perioder etter revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanFeb,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved slettet periode med fom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanFeb,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved slettet periode med fom og tom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanFeb,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }
        }
    }

    private fun lagGrunnlagsdata(fødeslsdato: LocalDate = LocalDate.of(1990, 1, 1)) =
        Grunnlagsdata(
            behandlingId = behandling.id,
            grunnlag =
                Grunnlag(
                    navn = Navn("fornavn", "mellomnavn", "etternavn"),
                    fødsel = Fødsel(fødeslsdato, fødeslsdato.year),
                    barn = emptyList(),
                    arena = null,
                ),
        )

    private fun lagVedtaksperiode(
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = UUID.randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
