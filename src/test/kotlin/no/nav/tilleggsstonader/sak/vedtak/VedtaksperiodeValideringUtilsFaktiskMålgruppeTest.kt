package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringUtils.validerEnkeltperiode
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteFaktiskeMålgrupper
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VedtaksperiodeValideringUtilsFaktiskMålgruppeTest {
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
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 2, 28),
                        utgift = 1000,
                    ),
                ),
        )

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
    }

    @Nested
    inner class ValiderEnkeltperiode {
        @Test
        fun `skal kaste feil om kombinasjon av målgruppe og aktivitet er ugyldig`() {
            val vedtaksperiode =
                lagVedtaksperiode(målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER, aktivitet = AktivitetType.TILTAK)

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining("Kombinasjonen av ENSLIG_FORSØRGER og TILTAK er ikke gyldig")
        }

        @Test
        fun `skal kaste feil om ingen periode for målgruppe matcher`() {
            val vedtaksperiode = lagVedtaksperiode(målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)

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

            assertThatThrownBy {
                validerEnkeltperiode(
                    vedtaksperiode = vedtaksperiode,
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for NEDSATT_ARBEIDSEVNE i perioden 01.12.2024 - 31.01.2025",
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
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
                    målgruppePerioderPerType = målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper(),
                    aktivitetPerioderPerType = aktiviteter.mergeSammenhengendeOppfylteAktiviteter(),
                )
            }.hasMessageContaining(
                "Finnes ingen periode med oppfylte vilkår for NEDSATT_ARBEIDSEVNE i perioden 01.01.2025 - 21.01.2025",
            )
        }
    }
}

private fun lagVedtaksperiode(
    fom: LocalDate = LocalDate.of(2025, 1, 1),
    tom: LocalDate = LocalDate.of(2025, 1, 31),
    målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    aktivitet: AktivitetType = AktivitetType.TILTAK,
) = vedtaksperiode(
    fom = fom,
    tom = tom,
    målgruppe = målgruppe,
    aktivitet = aktivitet,
)
