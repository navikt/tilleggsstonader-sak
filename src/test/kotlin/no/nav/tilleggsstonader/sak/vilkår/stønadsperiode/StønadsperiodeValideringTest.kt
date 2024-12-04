package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetBarnetilsynFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet as aktivitet1

internal class StønadsperiodeValideringTest {
    val målgrupper: Map<VilkårperiodeType, List<Datoperiode>> = mapOf(
        MålgruppeType.AAP to listOf(
            Datoperiode(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 31),
            ),
        ),
    )

    val aktiviteter: Map<VilkårperiodeType, List<Datoperiode>> = mapOf(
        AktivitetType.TILTAK to listOf(
            Datoperiode(
                fom = LocalDate.of(2023, 1, 4),
                tom = LocalDate.of(2023, 1, 10),
            ),
        ),
    )

    @Test
    internal fun `skal ikke kaste feil for gyldig stønadsperiode`() {
        val stønadsperiode = lagStønadsperiode()

        assertThatCode {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    internal fun `skal kaste feil om kombinasjon av målgruppe og aktivitet er ugyldig`() {
        val stønadsperiode =
            lagStønadsperiode(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.TILTAK)

        assertThatThrownBy {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining("Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig")
    }

    @Test
    internal fun `skal kaste feil om ingen periode for målgruppe matcher`() {
        val stønadsperiode = lagStønadsperiode(målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE)

        assertThatThrownBy {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
    }

    @Test
    internal fun `skal kaste feil om ingen periode for aktivitet matcher`() {
        val stønadsperiode = lagStønadsperiode(aktivitet = AktivitetType.UTDANNING)

        assertThatThrownBy {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining("Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt")
    }

    @Test
    internal fun `skal kaste feil om stønadsperiode er utenfor målgruppeperiode`() {
        val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2022, 12, 1))

        assertThatThrownBy {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining(feilmeldingIkkeOverlappendePeriode(stønadsperiode, stønadsperiode.målgruppe))
    }

    @Test
    internal fun `skal kaste feil om stønadsperiode er utenfor aktivitetsperiode`() {
        val stønadsperiode = lagStønadsperiode(tom = LocalDate.of(2023, 1, 11))

        assertThatThrownBy {
            validerEnkeltperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining(feilmeldingIkkeOverlappendePeriode(stønadsperiode, stønadsperiode.aktivitet))
    }

    @Test
    internal fun `skal ikke kaste feil dersom stønadsperiode går på tvers av to sammengengdende vilkårsperioder`() {
        val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 12))

        val målgrupper = listOf(
            målgruppe(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 31),
            ).tilDto(),
        )
        val aktiviteter = listOf(
            aktivitet1(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 10),
                faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
            ).tilDto(),
            aktivitet1(
                fom = LocalDate.of(2023, 1, 11),
                tom = LocalDate.of(2023, 1, 12),
                faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
            ).tilDto(),
        )

        assertThatCode {
            valider(
                listOf(stønadsperiode),
                VilkårperioderDto(målgrupper, aktiviteter),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    internal fun `skal ikke kaste feil dersom stønadsperiode går på tvers av to delvis overlappende vilkårsperioder`() {
        val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 12))

        val målgrupper = listOf(
            målgruppe(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 31),
            ).tilDto(),
        )

        val aktiviteter = listOf(
            aktivitet1(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 10),
                faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
            ).tilDto(),
            aktivitet1(
                fom = LocalDate.of(2023, 1, 7),
                tom = LocalDate.of(2023, 1, 12),
                faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
            ).tilDto(),
        )

        assertThatCode {
            valider(
                listOf(stønadsperiode),
                VilkårperioderDto(målgrupper, aktiviteter),
            )
        }.doesNotThrowAnyException()
    }

    @Nested
    inner class ValideringAvFødselsdato {

        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2025, 12, 31)
        val stønadsperioder = listOf(
            lagStønadsperiode(målgruppe = MålgruppeType.AAP, aktivitet = AktivitetType.TILTAK, fom = fom, tom = tom),
        )
        val målgrupper = listOf(målgruppe(fom = fom, tom = tom).tilDto())
        val aktiviteter = listOf(aktivitet1(fom = fom, tom = tom).tilDto())
        val vilkårperioder = VilkårperioderDto(målgrupper, aktiviteter)

        val dato18årGammel = fom.minusYears(18)
        val dato67årGammel = tom.minusYears(67)

        @Test
        fun `skal kaste feil dersom nedsatt arbeidsevne og personen er under 18 år`() {
            assertThatThrownBy {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato18årGammel.plusDays(1))
            }.hasMessageContaining("Periode kan ikke begynne før søker fyller 18 år")
        }

        @Test
        fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er over 18 år`() {
            assertThatCode {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato18årGammel)
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato18årGammel.minusDays(1))
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal kaste feil dersom nedsatt arbeidsevne og personen er over 67 år`() {
            assertThatThrownBy {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato67årGammel)
            }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")
            assertThatThrownBy {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato67årGammel.minusDays(1))
            }.hasMessageContaining("Periode kan ikke slutte etter søker fylt 67 år")
        }

        @Test
        fun `skal ikke kaste feil dersom nedsatt arbeidsevne og personen er under 67 år`() {
            assertThatCode {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato67årGammel.plusDays(1))
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke kaste feil dersom overgangsstønad og under 18 år eller over 67 år`() {
            val stønadsperioder = stønadsperioder.map {
                it.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD, aktivitet = AktivitetType.UTDANNING)
            }
            val vilkårperioder = vilkårperioder.copy(
                målgrupper = målgrupper.map { it.copy(type = MålgruppeType.OVERGANGSSTØNAD) },
                aktiviteter = målgrupper.map { it.copy(type = AktivitetType.UTDANNING) },
            )

            assertThatCode {
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato18årGammel.minusYears(1))
                valider(stønadsperioder, vilkårperioder, fødselsdato = dato67årGammel.plusYears(1))
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class ValiderStønadsperioderOverlapper {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 1, 7)

        @Test
        fun `skal kaste feil hvis stønadsperioder overlapper`() {
            val stønadsperiode = lagStønadsperiode(fom = fom, tom = tom)

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode, stønadsperiode),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `skal kaste feil hvis stønadsperioder overlapper med en dag, uavhengig sortering`() {
            val stønadsperiode1 = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 5))
            val stønadsperiode2 = lagStønadsperiode(fom = LocalDate.of(2023, 1, 5), tom = LocalDate.of(2023, 1, 10))

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode1, stønadsperiode2),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode2, stønadsperiode1),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `skal kaste feil hvis en stønadsperiode inneholder en annen`() {
            val stønadsperiode1 = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))
            val stønadsperiode2 = lagStønadsperiode(fom = LocalDate.of(2023, 1, 5), tom = LocalDate.of(2023, 1, 5))

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode1, stønadsperiode2),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode2, stønadsperiode1),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")
        }
    }

    @Nested
    inner class ValiderStønadsperioderIkkeOppfyltePerioder {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 1, 7)

        val målgrupper = listOf(
            målgruppe(
                fom = fom,
                tom = tom,
                resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
            ).tilDto(),
        )
        val aktiviteter = listOf(
            aktivitet1(
                fom = fom,
                tom = tom,
                resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
            ).tilDto(),
        )

        @Test
        fun `skal kaste feil hvis vilkårsresultat ikke er oppfylt`() {
            val stønadsperiode = lagStønadsperiode(fom = fom, tom = tom)

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode),
                    VilkårperioderDto(målgrupper, aktiviteter),
                )
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
        }
    }

    @Nested
    inner class ValiderStønadsperioderMergeSammenhengende {

        val målgrupper = listOf(
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
        ).map(Vilkårperiode::tilDto)

        val aktiviteter = målgrupper.map {
            it.copy(
                type = AktivitetType.TILTAK,
                faktaOgVurderinger = AktivitetBarnetilsynFaktaOgVurderingerDto(lønnet = VurderingDto(SvarJaNei.NEI)),
            )
        }

        @Test
        fun `skal godta stønadsperiode på tvers av 2 godkjente sammenhengende vilkårsperioder`() {
            val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))

            assertThatCode {
                valider(
                    listOf(stønadsperiode),
                    VilkårperioderDto(målgrupper, aktiviteter),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta stønadsperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 21))

            assertThatThrownBy {
                valider(
                    listOf(stønadsperiode),
                    VilkårperioderDto(målgrupper, aktiviteter),
                )
            }.hasMessageContaining(feilmeldingIkkeOverlappendePeriode(stønadsperiode, stønadsperiode.målgruppe))
        }
    }

    @Nested
    inner class OverlappMedPeriodeSomIkkeGirRettPåStønad {

        val jan = YearMonth.of(2024, 1)
        val fom = jan.atDay(10)
        val tom = jan.atDay(20)

        @Test
        fun `kan ha stønadsperiode før og etter periode som ikke gir rett på stønad`() {
            val målgrupper = listOf(
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                    fom = jan.atDay(1),
                    tom = jan.atDay(9),
                ),
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.SYKEPENGER_100_PROSENT),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "asd",
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                ),
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "asd",
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                ),
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                    fom = jan.atDay(21),
                    tom = jan.atDay(31),
                ),
            ).map { it.tilDto() }

            val aktiviteter = listOf(
                aktivitet1(
                    faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
                    fom = jan.atDay(1),
                    tom = jan.atDay(9),
                ),
                aktivitet1(
                    faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.INGEN_AKTIVITET),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "asd",
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                ),
                aktivitet1(
                    faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
                    fom = jan.atDay(21),
                    tom = jan.atDay(31),
                ),

            ).map { it.tilDto() }

            assertThatCode {
                valider(
                    listOf(
                        lagStønadsperiode(fom = jan.atDay(1), tom = jan.atDay(9)),
                        lagStønadsperiode(fom = jan.atDay(21), tom = jan.atDay(31)),
                    ),
                    VilkårperioderDto(målgrupper, aktiviteter),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal kaste feil hvis en stønadsperiode overlapper med 100 prosent sykemelding`() {
            val målgrupper = listOf(
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.SYKEPENGER_100_PROSENT),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "a",
                ),
            ).map { it.tilDto() }

            assertThatThrownBy {
                valider(
                    stønadsperioder = listOf(lagStønadsperiode(fom = jan.atDay(1), tom = jan.atEndOfMonth())),
                    vilkårperioder = VilkårperioderDto(målgrupper, emptyList()),
                )
            }.hasMessage(
                "Stønadsperiode 01.01.2024 - 31.01.2024 overlapper med SYKEPENGER_100_PROSENT(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal kaste feil hvis en stønadsperiode overlapper med INGEN_MÅLGRUPPE`() {
            val målgrupper = listOf(
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "a",
                ),
            ).map { it.tilDto() }

            assertThatThrownBy {
                valider(
                    stønadsperioder = listOf(lagStønadsperiode(fom = jan.atDay(1), tom = jan.atDay(15))),
                    vilkårperioder = VilkårperioderDto(målgrupper, emptyList()),
                )
            }.hasMessage(
                "Stønadsperiode 01.01.2024 - 15.01.2024 overlapper med INGEN_MÅLGRUPPE(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal kaste feil hvis en stønadsperiode overlapper med INGEN_AKTIVITET`() {
            val aktiviteter = listOf(
                aktivitet1(
                    faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.INGEN_AKTIVITET),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "a",
                ),
            ).map { it.tilDto() }
            assertThatThrownBy {
                valider(
                    stønadsperioder = listOf(lagStønadsperiode(fom = jan.atDay(15), tom = jan.atDay(15))),
                    vilkårperioder = VilkårperioderDto(emptyList(), aktiviteter),
                )
            }.hasMessage(
                "Stønadsperiode 15.01.2024 - 15.01.2024 overlapper med INGEN_AKTIVITET(10.01.2024 - 20.01.2024) som ikke gir rett på stønad",
            )
        }

        @Test
        fun `skal ikke kaste feil om stønadsperiode overlapper med slettet vilkårperiode uten rett til stønad`() {
            val behandlingId = BehandlingId.random()

            val målgrupper = listOf(
                målgruppe(
                    behandlingId = behandlingId,
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                    fom = fom,
                    tom = tom,
                    begrunnelse = "a",
                    resultat = ResultatVilkårperiode.SLETTET,
                    slettetKommentar = "slettet",
                ),
                målgruppe(
                    behandlingId = behandlingId,
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                    fom = fom,
                    tom = tom,
                ),
            ).map { it.tilDto() }

            val aktiviteter = listOf(
                aktivitet1(
                    behandlingId = behandlingId,
                    faktaOgVurdering = faktaOgVurderingAktivitet(type = AktivitetType.TILTAK),
                    fom = fom,
                    tom = tom,
                ),
            ).map { it.tilDto() }

            val stønadsperioder = listOf(
                stønadsperiode(
                    behandlingId = behandlingId,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = MålgruppeType.AAP,
                    fom = fom,
                    tom = tom,
                ).tilDto(),
            )

            assertThatCode {
                valider(
                    stønadsperioder = stønadsperioder,
                    vilkårperioder = VilkårperioderDto(aktiviteter = aktiviteter, målgrupper = målgrupper),
                )
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class MergeSammenhengendeOgOppfylteVilkårperioder {

        @Test
        fun `overlapper med lik fom-dato`() {
            val perioder = listOf(
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 8),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 2),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
            )

            assertThat(perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!.first()).isEqualTo(
                Datoperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)),
            )
        }

        @Test
        fun `sammenhengende uten overlapp`() {
            val perioder = listOf(
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 5),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
            )

            assertThat(perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!.first()).isEqualTo(
                Datoperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)),
            )
        }

        @Test
        fun `to sammenhengende grupper`() {
            val perioder = listOf(
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 5),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 12),
                    tom = LocalDate.of(2023, 1, 20),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 16),
                    tom = LocalDate.of(2023, 1, 24),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(2)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ),
            )
            assertThat(mergetPerioder[1]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 12),
                    tom = LocalDate.of(2023, 1, 24),
                ),
            )
        }

        @Test
        fun `skal fjerne ikke oppfylt periode`() {
            val perioder = listOf(
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 3),
                    tom = LocalDate.of(2023, 1, 10),
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 20),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(2)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ),
            )
            assertThat(mergetPerioder[1]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 20),
                ),
            )
        }

        @Test
        fun `fullstendig omsluttende`() {
            val perioder = listOf(
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
                aktivitet1(
                    fom = LocalDate.of(2023, 1, 3),
                    tom = LocalDate.of(2023, 1, 8),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(1)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ),
            )
        }
    }

    fun valider(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
        fødselsdato: LocalDate? = null,
    ) = StønadsperiodeValidering.valider(
        stønadsperioder = stønadsperioder,
        vilkårperioder = vilkårperioder,
        fødselsdato = fødselsdato,
    )

    fun validerEnkeltperiode(
        stønadsperiode: StønadsperiodeDto,
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        fødselsdato: LocalDate? = null,
    ) = StønadsperiodeValidering.validerEnkeltperiode(
        stønadsperiode = stønadsperiode,
        målgruppePerioderPerType = målgruppePerioderPerType,
        aktivitetPerioderPerType = aktivitetPerioderPerType,
        fødselsdato = fødselsdato,
    )

    private fun feilmeldingIkkeOverlappendePeriode(stønadsperiode: StønadsperiodeDto, type: VilkårperiodeType) =
        "Finnes ingen periode med oppfylte vilkår for $type i perioden " +
            "${stønadsperiode.fom.norskFormat()} - ${stønadsperiode.tom.norskFormat()}"

    private fun lagStønadsperiode(
        fom: LocalDate = LocalDate.of(2023, 1, 4),
        tom: LocalDate = LocalDate.of(2023, 1, 10),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
        status: StønadsperiodeStatus = StønadsperiodeStatus.NY,
    ): StønadsperiodeDto {
        return StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet,
            status = status,
        )
    }
}
