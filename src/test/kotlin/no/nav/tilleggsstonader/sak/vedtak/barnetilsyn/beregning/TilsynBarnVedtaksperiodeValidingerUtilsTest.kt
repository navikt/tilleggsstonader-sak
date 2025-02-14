package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Fødsel
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Navn
import no.nav.tilleggsstonader.sak.util.norskFormat
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
            )
        }.hasMessageContaining("Kombinasjonen av ${vedtaksperiode.målgruppe} og ${vedtaksperiode.aktivitet} er ikke gyldig")
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
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for ${vedtaksperiode.målgruppe} er oppfylt")
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
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for ${vedtaksperiode.aktivitet} er oppfylt")
    }

    @Test
    fun `skal kaste feil om vedtaksperiode er utenfor målgruppeperiode`() {
        val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2022, 12, 1))

        assertThatThrownBy {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                listOf(
                    vedtaksperiode,
                ),
                behandlingId,
            )
        }.hasMessageContaining(
            "Finnes ingen periode med oppfylte vilkår for ${vedtaksperiode.målgruppe} i perioden " +
                "${vedtaksperiode.fom.norskFormat()} - ${vedtaksperiode.tom.norskFormat()}",
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
            )
        }.hasMessageContaining(
            "Finnes ingen periode med oppfylte vilkår for ${vedtaksperiode.aktivitet} i perioden " +
                "${vedtaksperiode.fom.norskFormat()} - ${vedtaksperiode.tom.norskFormat()}",
        )
    }

    @Test
    fun `skal ikke kaste feil dersom vedtaksperiode går på tvers av to sammengengdende vilkårsperioder`() {
        val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 12))

        val målgrupper =
            listOf(
                målgruppe(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                ),
            )
        val aktiviteter =
            listOf(
                aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                ),
                aktivitet(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 12),
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
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `skal ikke kaste feil dersom vedtaksperiode går på tvers av to delvis overlappende vilkårsperioder`() {
        val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 12))

        val målgrupper =
            listOf(
                målgruppe(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 31),
                ),
            )

        val aktiviteter =
            listOf(
                aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                ),
                aktivitet(
                    fom = LocalDate.of(2023, 1, 7),
                    tom = LocalDate.of(2023, 1, 12),
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
            )
        }.doesNotThrowAnyException()
    }

    @Nested
    inner class ValiderVedtaksperioderMedMergeSammenhengendeVilkårperioder {
        val målgrupper =
            listOf(
                målgruppe(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 7),
                ),
                målgruppe(
                    fom = LocalDate.of(2023, 1, 8),
                    tom = LocalDate.of(2023, 1, 18),
                ),
                målgruppe(
                    fom = LocalDate.of(2023, 1, 20),
                    tom = LocalDate.of(2023, 1, 31),
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
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    listOf(
                        vedtaksperiode,
                    ),
                    behandlingId,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta stønadsperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 21))

            assertThatCode {
                tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                    listOf(
                        vedtaksperiode,
                    ),
                    behandlingId,
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for ${vedtaksperiode.målgruppe} i perioden " +
                    "${vedtaksperiode.fom.norskFormat()} - ${vedtaksperiode.tom.norskFormat()}",
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
                )
            }
        }
    }

    @Nested
    inner class OverlappMedPeriodeSomIkkeGirRettPåStønad {
        val jan = YearMonth.of(2024, 1)
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
                )
            }.hasMessage(
                "Vedtaksperiode 01.01.2024 - 31.01.2024 overlapper med SYKEPENGER_100_PROSENT(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
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
                )
            }.hasMessage(
                "Vedtaksperiode 01.01.2024 - 15.01.2024 overlapper med INGEN_MÅLGRUPPE(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
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
                )
            }.hasMessage(
                "Vedtaksperiode 15.01.2024 - 15.01.2024 overlapper med INGEN_AKTIVITET(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
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
                )
            }.doesNotThrowAnyException()
        }

        @Nested
        inner class ValideringAvFødselsdato {
            val fom = LocalDate.of(2024, 1, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val vedtaksperioder =
                listOf(
                    lagVedtaksperiode(
                        målgruppe = MålgruppeType.AAP,
                        aktivitet = AktivitetType.TILTAK,
                        fom = fom,
                        tom = tom,
                    ),
                )
            val målgrupper = listOf(målgruppe(fom = fom, tom = tom))
            val aktiviteter = listOf(aktivitet(fom = fom, tom = tom))
            val vilkårperioder = Vilkårperioder(målgrupper, aktiviteter)

            val dato18årGammel = fom.minusYears(18)
            val dato67årGammel = tom.minusYears(67)

            @BeforeEach
            fun setup() {
                every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
            }

            @Test
            fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er over 18 år`() {
                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns lagGrunnlagsdata(dato18årGammel)
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
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
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
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
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
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
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
                    )
                }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel.minusDays(1),
                    )
                assertThatThrownBy {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
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
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `skal ikke kaste feil dersom overgangsstønad og under 18 år eller over 67 år`() {
                val vedtaksperioder =
                    vedtaksperioder.map {
                        it.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.UTDANNING)
                    }
                val vilkårperioder =
                    vilkårperioder.copy(
                        målgrupper =
                            målgrupper.map {
                                målgruppe(
                                    fom = it.fom,
                                    tom = it.tom,
                                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                                )
                            },
                        aktiviteter =
                            aktiviteter.map {
                                aktivitet(
                                    fom = it.fom,
                                    tom = it.tom,
                                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.UTDANNING),
                                )
                            },
                    )
                every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato18årGammel.minusYears(1),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
                    )
                }.doesNotThrowAnyException()

                every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns
                    lagGrunnlagsdata(
                        dato67årGammel.plusYears(1),
                    )
                assertThatCode {
                    tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                        vedtaksperioder = vedtaksperioder,
                        behandlingId = behandlingId,
                    )
                }.doesNotThrowAnyException()
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
