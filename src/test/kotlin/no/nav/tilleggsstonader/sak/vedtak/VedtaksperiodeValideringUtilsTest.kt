package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerIngenOverlappMellomVedtaksperioder
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtaksperiodeValideringUtilsTest {
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
                    validerIngenOverlappMellomVedtaksperioder(
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
                validerIngenOverlappMellomVedtaksperioder(
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
                validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
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
                validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
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
                validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
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
                validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
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
                validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
                    vilkårperioder = vilkårperioder,
                    vedtaksperioder = listOf(lagVedtaksperiode(fom = jan.atDay(10), tom = jan.atDay(20))),
                )
            }.doesNotThrowAnyException()
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
                    validerUtgiftHeleVedtaksperioden(
                        vedtaksperioder = listOf(vedtaksperiode),
                        utgifter = utgifter,
                    )
                }
            }

            @Test
            fun `kaster feil når det ikke finnes utgifter hele vedtaksperioden`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerUtgiftHeleVedtaksperioden(
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
                    validerUtgiftHeleVedtaksperioden(
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
                    validerUtgiftHeleVedtaksperioden(
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
                        validerUtgiftHeleVedtaksperioden(
                            vedtaksperioder = listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 3, 31))),
                            utgifter = utgifter,
                        )
                    }
                assertThat(feil.feil).contains("Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden")
            }
        }
    }

    @Nested
    inner class ValiderEnkeltperiode {
        @Test
        fun `skal kaste feil om kombinasjon av målgruppe og aktivitet er ugyldig`() {
            val vedtaksperiode =
                lagVedtaksperiode(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.TILTAK)

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining("Kombinasjonen av OVERGANGSSTØNAD og TILTAK er ikke gyldig")
        }

        @Test
        fun `skal kaste feil om ingen periode for målgruppe matcher`() {
            val vedtaksperiode = lagVedtaksperiode(målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE)

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for NEDSATT_ARBEIDSEVNE er oppfylt")
        }

        @Test
        fun `skal kaste feil om ingen periode for aktivitet matcher`() {
            val vedtaksperiode = lagVedtaksperiode(aktivitet = AktivitetType.UTDANNING)

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for UTDANNING er oppfylt")
        }

        @Test
        fun `skal kaste feil om vedtaksperiode er utenfor målgruppeperiode`() {
            val vedtaksperiode = lagVedtaksperiode(fom = LocalDate.of(2024, 12, 1))

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
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
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
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
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
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
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
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
        fun `skal godta vedtaksperiode på tvers av 2 godkjente sammenhengende vilkårsperioder`() {
            val vedtaksperiode =
                lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 10))

            assertThatCode {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta vedtaksperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val vedtaksperiode =
                lagVedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 21))

            assertThatCode {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for AAP i perioden 01.01.2025 - 21.01.2025",
            )
        }
    }
}

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
