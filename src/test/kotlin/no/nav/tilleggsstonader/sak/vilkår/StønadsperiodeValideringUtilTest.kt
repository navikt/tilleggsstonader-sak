package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.StønadsperiodeValideringUtil.validerStønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.StønadsperiodeValideringUtil.validerStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeDomainUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeDomainUtil.delvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeDomainUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.dto.tilDto
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class StønadsperiodeValideringUtilTest {
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
            validerStønadsperiode(
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
            validerStønadsperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining("Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig")
    }

    @Test
    internal fun `skal kaste feil om ingen periode for målgruppe matcher`() {
        val stønadsperiode = lagStønadsperiode(målgruppe = MålgruppeType.AAP_FERDIG_AVKLART)

        assertThatThrownBy {
            validerStønadsperiode(
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
            validerStønadsperiode(
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
            validerStønadsperiode(
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
            validerStønadsperiode(
                stønadsperiode,
                målgrupper,
                aktiviteter,
            )
        }.hasMessageContaining(feilmeldingIkkeOverlappendePeriode(stønadsperiode, stønadsperiode.aktivitet))
    }

    @Nested
    inner class ValiderStønadsperioderOverlapper {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 1, 7)

        @Test
        fun `skal kaste feil hvis stønadsperioder overlapper`() {
            val stønadsperiode = lagStønadsperiode(fom = fom, tom = tom)

            assertThatThrownBy {
                validerStønadsperioder(
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
                validerStønadsperioder(
                    listOf(stønadsperiode1, stønadsperiode2),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")

            assertThatThrownBy {
                validerStønadsperioder(
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
                validerStønadsperioder(
                    listOf(stønadsperiode1, stønadsperiode2),
                    mockk(),
                )
            }.hasMessageContaining("overlapper")

            assertThatThrownBy {
                validerStønadsperioder(
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
            aktivitet(
                fom = fom,
                tom = tom,
                resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
            ).tilDto(),
        )

        @Test
        fun `skal kaste feil hvis vilkårsresultat ikke er oppfylt`() {
            val stønadsperiode = lagStønadsperiode(fom = fom, tom = tom)

            assertThatThrownBy {
                validerStønadsperioder(
                    listOf(stønadsperiode),
                    Vilkårperioder(målgrupper, aktiviteter),
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
                type = MålgruppeType.AAP,
            ),
            målgruppe(
                fom = LocalDate.of(2023, 1, 20),
                tom = LocalDate.of(2023, 1, 31),
                type = MålgruppeType.AAP,
            ),
        ).map(Vilkårperiode::tilDto)

        val aktiviteter = målgrupper.map { it.copy(type = AktivitetType.TILTAK, detaljer = delvilkårAktivitet()) }

        @Test
        fun `skal godta stønadsperiode på tvers av 2 godkjente sammenhengende vilkårsperioder`() {
            val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))

            assertThatCode {
                validerStønadsperioder(
                    listOf(stønadsperiode),
                    Vilkårperioder(målgrupper, aktiviteter),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `skal ikke godta stønadsperiode på tvers av 2 godkjente, men ikke sammenhengende vilkårsperioder`() {
            val stønadsperiode = lagStønadsperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 21))

            assertThatThrownBy {
                validerStønadsperioder(
                    listOf(stønadsperiode),
                    Vilkårperioder(målgrupper, aktiviteter),
                )
            }.hasMessageContaining(feilmeldingIkkeOverlappendePeriode(stønadsperiode, stønadsperiode.målgruppe))
        }
    }

    private fun feilmeldingIkkeOverlappendePeriode(stønadsperiode: StønadsperiodeDto, type: VilkårperiodeType) =
        "Finnes ingen periode med oppfylte vilkår for $type i perioden " +
            "${stønadsperiode.fom.norskFormat()} - ${stønadsperiode.tom.norskFormat()}"

    private fun lagStønadsperiode(
        fom: LocalDate = LocalDate.of(2023, 1, 4),
        tom: LocalDate = LocalDate.of(2023, 1, 10),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ): StønadsperiodeDto {
        return StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet,
        )
    }
}
