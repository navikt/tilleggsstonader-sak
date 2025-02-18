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
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
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

class TilsynBarnVedtaksperiodeValidingerUtilsTest {
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val tilsynBarnVedtaksperiodeValidingerService =
        TilsynBarnVedtaksperiodeValidingerService(
            vilkårperiodeService = vilkårperiodeService,
            grunnlagsdataService = grunnlagsdataService,
        )

    val behandlingId = BehandlingId.random()

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
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `skal kaste feil om kombinasjon av målgruppe og aktivitet er ugyldig`() {
        val vedtaksperiode =
            lagVedtaksperiode(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.TILTAK)

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
            )
        }.hasMessageContaining("Kombinasjonen av OVERGANGSSTØNAD og TILTAK er ikke gyldig")
    }

    @Test
    fun `skal kaste feil om ingen periode for målgruppe matcher`() {
        val vedtaksperiode = lagVedtaksperiode(målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE)

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for NEDSATT_ARBEIDSEVNE er oppfylt")
    }

    @Test
    fun `skal kaste feil om ingen periode for aktivitet matcher`() {
        val vedtaksperiode = lagVedtaksperiode(aktivitet = AktivitetType.UTDANNING)

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for UTDANNING er oppfylt")
    }

    @Test
    fun `skal kaste feil om vedtaksperiode er utenfor målgruppeperiode`() {
        val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2024, 12, 1))

        val utgifter: Map<BarnId, List<UtgiftBeregning>> =
            mapOf(
                BarnId.random() to
                    listOf(
                        UtgiftBeregning(
                            fom = YearMonth.of(2024, 12),
                            tom = YearMonth.of(2025, 2),
                            utgift = 1000,
                        ),
                    ),
            )

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
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

        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteter,
            )

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
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

        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteter,
            )

        assertThatCode {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
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

        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteter,
            )

        assertThatCode {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
                utgifter,
            )
        }.doesNotThrowAnyException()
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

        @BeforeEach
        fun setup() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )
        }

        @Test
        fun `skal godta stønadsperiode på tvers av 2 godkjente sammenhengende vilkårsperioder`() {
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 10))

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    listOf(
                        vedtaksperiode,
                    ),
                    behandlingId,
                    utgifter,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta stønadsperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 21))

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    listOf(
                        vedtaksperiode,
                    ),
                    behandlingId,
                    utgifter,
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for AAP i perioden 01.01.2025 - 21.01.2025",
            )
        }
    }

    @Nested
    inner class ValiderIngenOverlapp {
        val vedtaksperiodeJan =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
        val vedtaksperiodeFeb =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
        val vedtaksperiodeJanFeb =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )

        @Test
        fun `kaster feil hvis vedtaksperioderDto overlapper`() {
            val feil =
                assertThrows<ApiFeil> {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        listOf(
                            vedtaksperiodeJan,
                            vedtaksperiodeJanFeb,
                        ),
                        behandlingId,
                        utgifter,
                    )
                }
            assertThat(feil.feil).contains("Vedtaksperioder kan ikke overlappe")
        }

        @Test
        fun `kaster ikke feil hvis vedtaksperioderDto ikke overlapper`() {
            assertDoesNotThrow {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    listOf(
                        vedtaksperiodeJan,
                        vedtaksperiodeFeb,
                    ),
                    behandlingId,
                    utgifter,
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

            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder =
                        listOf(
                            lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atDay(9)),
                            lagVedtaksperiode(fom = jan.atDay(21), tom = jan.atDay(31)),
                        ),
                    behandlingId = behandlingId,
                    utgifter,
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
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = listOf(tilltakJan),
                )
            assertThatThrownBy {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atEndOfMonth())),
                    behandlingId = behandlingId,
                    utgifter,
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

            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = listOf(tilltakJan),
                )

            assertThatThrownBy {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(1), tom = jan.atDay(15))),
                    behandlingId = behandlingId,
                    utgifter,
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

            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = listOf(aapJan),
                    aktiviteter = aktiviteter,
                )

            assertThatThrownBy {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(15), tom = jan.atDay(15))),
                    behandlingId = behandlingId,
                    utgifter,
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

            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                )

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(10), tom = jan.atDay(20))),
                    behandlingId = behandlingId,
                    utgifter,
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

            @BeforeEach
            fun setup() {
                every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(målgrupper, aktiviteter)
            }

            @Test
            fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er over 18 år`() {
                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns lagGrunnlagsdata(dato18årGammel)
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.doesNotThrowAnyException()

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato18årGammel.minusDays(
                            1,
                        ),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `skal kaste feil dersom nedsatt arbeidsevne og personen er under 18 år`() {
                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato18årGammel.plusDays(
                            1,
                        ),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.hasMessageContaining("Periode kan ikke begynne før søker fyller 18 år")
            }

            @Test
            fun `skal kaste feil dersom nedsatt arbeidsevne og personen er over 67 år`() {
                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel,
                    )
                assertThatThrownBy {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel.minusDays(1),
                    )
                assertThatThrownBy {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")
            }

            @Test
            fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er under 67 år`() {
                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel.plusDays(1),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `skal ikke kaste feil dersom overgangsstønad og under 18 år eller over 67 år`() {
                val vedtaksperioder =
                    lagVedtaksperiode(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.UTDANNING)

                val vilkårperioder =
                    Vilkårperioder(
                        målgrupper =
                            listOf(
                                målgruppe(
                                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                                    fom = LocalDate.of(2025, 1, 1),
                                    tom = LocalDate.of(2025, 2, 28),
                                ),
                            ),
                        aktiviteter =
                            listOf(
                                aktivitet(
                                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.UTDANNING),
                                    fom = LocalDate.of(2025, 1, 1),
                                    tom = LocalDate.of(2025, 2, 28),
                                ),
                            ),
                    )

                every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato18årGammel.minusYears(1),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }.doesNotThrowAnyException()

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel.plusYears(1),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperioder),
                        behandlingId = behandlingId,
                        utgifter,
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
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperiode),
                        behandlingId = behandlingId,
                        utgifter,
                    )
                }
            }

            @Test
            fun `kaster feil når det ikke finnes utgifter hele vedtaksperioden`() {
                val feil =
                    assertThrows<ApiFeil> {
                        tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                            vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                            behandlingId = behandlingId,
                            utgifter,
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
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                        behandlingId = behandlingId,
                        utgifter,
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
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 2, 28))),
                        behandlingId = behandlingId,
                        utgifter,
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
                        tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                            vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 3, 31))),
                            behandlingId = behandlingId,
                            utgifter,
                        )
                    }
                assertThat(feil.feil).contains("Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden")
            }
        }
    }

    private fun lagGrunnlagsdata(fødeslsdato: LocalDate = LocalDate.of(1990, 1, 1)) =
        Grunnlagsdata(
            behandlingId = behandlingId,
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
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
