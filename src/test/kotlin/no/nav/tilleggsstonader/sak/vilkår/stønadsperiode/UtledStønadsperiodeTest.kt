package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType.TILTAK
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType.UTDANNING
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType.AAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtledStønadsperiodeTest {

    val behandlingId = BehandlingId.random()

    /**
     * AAP          |-----------|
     * Utdanning  |-----|
     * Tiltak         |------|
     * Utdanning           |------|
     */
    @Test
    fun `tiltak skal trumfe utdanning`() {
        val stønadsperioder = UtledStønadsperiode.utled(
            behandlingId,
            vilkårperioder = listOf(
                målgruppe(fom = dato(2023, 3, 15), tom = dato(2023, 4, 27), type = AAP),

                aktivitet(fom = dato(2023, 2, 15), tom = dato(2023, 4, 20), type = UTDANNING),
                aktivitet(fom = dato(2023, 3, 20), tom = dato(2023, 4, 25), type = TILTAK),
                aktivitet(fom = dato(2023, 3, 21), tom = dato(2023, 4, 30), type = UTDANNING),
            ),
        ).map { it.forenklet() }.sortedBy { it.fom }
        assertThat(stønadsperioder).containsExactly(
            Periode(dato(2023, 3, 15), dato(2023, 3, 19), AAP, UTDANNING),
            Periode(dato(2023, 3, 20), dato(2023, 4, 25), AAP, TILTAK),
            Periode(dato(2023, 4, 26), dato(2023, 4, 27), AAP, UTDANNING),
        )
    }

    /**
     * AAP          |-----------|
     * OS         |---------------|
     * Utdanning  |-----|
     * Tiltak         |------|
     * Utdanning           |------|
     */
    @Test
    fun `AAP skal trumfe overgangsstønad`() {
        val stønadsperioder = UtledStønadsperiode.utled(
            behandlingId,
            vilkårperioder = listOf(
                målgruppe(fom = dato(2023, 2, 10), tom = dato(2023, 3, 20), type = OVERGANGSSTØNAD),
                målgruppe(fom = dato(2023, 3, 15), tom = dato(2023, 3, 15), type = AAP),
                målgruppe(fom = dato(2023, 3, 16), tom = dato(2023, 3, 16), type = AAP),

                aktivitet(fom = dato(2023, 2, 1), tom = dato(2023, 3, 17), type = UTDANNING),
                aktivitet(fom = dato(2023, 3, 19), tom = dato(2023, 3, 20), type = UTDANNING),
            ),
        ).map { it.forenklet() }.sortedBy { it.fom }
        assertThat(stønadsperioder).containsExactly(
            Periode(dato(2023, 2, 10), dato(2023, 3, 14), OVERGANGSSTØNAD, UTDANNING),
            Periode(dato(2023, 3, 15), dato(2023, 3, 16), AAP, UTDANNING),
            Periode(dato(2023, 3, 17), dato(2023, 3, 17), OVERGANGSSTØNAD, UTDANNING),
            Periode(dato(2023, 3, 19), dato(2023, 3, 20), OVERGANGSSTØNAD, UTDANNING),
        )
    }

    @Nested
    inner class AntallAktivitetsdager {

        @Test
        fun `kombinasjon med høyere antall aktivitetsdager skal trumfe en annan som har lavere antall aktivitetsdager`() {
            TODO("Not yet implemented")
        }

        @Test
        fun `2 tiltak som har høyere antall dager enn utdanning skal trume over utdanning`() {
            TODO("Not yet implemented")
        }
    }

    private fun Stønadsperiode.forenklet() = Periode(fom, tom, målgruppe, aktivitet)

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
    )

    private fun stønadsperiode(
        fom: LocalDate,
        tom: LocalDate,
        målgruppe: MålgruppeType,
        aktivitet: AktivitetType,
    ) = stønadsperiode(behandlingId, fom, tom, målgruppe, aktivitet)

    fun dato(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)
}
