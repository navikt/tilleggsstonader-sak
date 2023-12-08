package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.StønadsperiodeValideringUtil.validerStønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.StønadsperiodeValideringUtil.validerStønadsperioder
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
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

    // TODO: Test feil kombinasjoner av målgruppe og aktivitet

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
    inner class ValiderStønadsperioderIkkeOppfyltePerioder {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 1, 7)

        val målgrupper = listOf(
            lagVilkårperiodeDto(
                fom = fom,
                tom = tom,
                type = MålgruppeType.AAP,
                vilkårsresultat = Vilkårsresultat.IKKE_OPPFYLT,
            ),
        )
        val aktiviteter = målgrupper.map { it.copy(type = AktivitetType.TILTAK) }

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
            lagVilkårperiodeDto(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 7),
                type = MålgruppeType.AAP,
            ),
            lagVilkårperiodeDto(
                fom = LocalDate.of(2023, 1, 8),
                tom = LocalDate.of(2023, 1, 18),
                type = MålgruppeType.AAP,
            ),
            lagVilkårperiodeDto(
                fom = LocalDate.of(2023, 1, 20),
                tom = LocalDate.of(2023, 1, 31),
                type = MålgruppeType.AAP,
            ),
        )

        val aktiviteter = målgrupper.map { it.copy(type = AktivitetType.TILTAK) }

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

    fun lagVilkårperiodeDto(
        fom: LocalDate,
        tom: LocalDate,
        type: VilkårperiodeType,
        vilkårsresultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    ) = VilkårperiodeDto(
        type = type,
        fom = fom,
        tom = tom,
        vilkår = lagVilkårDto(vilkårsresultat),
    )

    fun lagVilkårDto(vilkårsresultat: Vilkårsresultat) =
        vilkår(behandlingId = UUID.randomUUID(), resultat = vilkårsresultat).tilDto()
}
