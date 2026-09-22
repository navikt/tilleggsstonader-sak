package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdagerNullable
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktaOgVurderingMapperTest {
    private val behandlingId = BehandlingId.random()

    private fun lagreVilkårperiodeDagligReiseTsr(aktivitetsdager: Int?) =
        LagreVilkårperiode(
            behandlingId = behandlingId,
            type = AktivitetType.TILTAK,
            tiltaksvariant = TypeAktivitet.GRUPPEAMO,
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            faktaOgSvar =
                FaktaOgSvarAktivitetDagligReiseTsrDto(
                    svarHarUtgifter = SvarJaNei.JA,
                    aktivitetsdager = aktivitetsdager,
                ),
        )

    @Nested
    inner class DagligReiseTsr {
        @Test
        fun `skal feile hvis aktivitetsdager mangler for ny aktivitet`() {
            assertThatThrownBy {
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTsr(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    fødselFaktaGrunnlag = null,
                )
            }.hasMessageContaining("Mangler data: aktivitetsdager må være satt og være et heltall mellom 1 og 5")
        }

        @Test
        fun `skal ikke kreve aktivitetsdager ved oppdatering hvis det ikke var satt fra før`() {
            val faktaOgVurdering =
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTsr(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = false,
                )

            val aktivitetsdager =
                (faktaOgVurdering as AktivitetDagligReiseTsr)
                    .fakta
                    .takeIfFakta<FaktaAktivitetsdagerNullable>()
                    ?.aktivitetsdager

            assertThat(aktivitetsdager).isNull()
        }

        @Test
        fun `skal fortsatt kreve aktivitetsdager ved oppdatering hvis det var satt fra før`() {
            assertThatThrownBy {
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTsr(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = true,
                )
            }.hasMessageContaining("Mangler data: aktivitetsdager må være satt og være et heltall mellom 1 og 5")
        }

        @Test
        fun `skal validere aktivitetsdager selv om det ikke kreves, hvis en verdi er sendt med`() {
            assertThatThrownBy {
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTsr(aktivitetsdager = 0),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = false,
                )
            }.hasMessageContaining("Mangler data: aktivitetsdager må være satt og være et heltall mellom 1 og 5")
        }

        @Test
        fun `skal godta gyldig aktivitetsdager`() {
            val faktaOgVurdering =
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTsr(aktivitetsdager = 3),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = false,
                )

            val aktivitetsdager =
                (faktaOgVurdering as AktivitetDagligReiseTsr)
                    .fakta
                    .takeIfFakta<FaktaAktivitetsdagerNullable>()
                    ?.aktivitetsdager

            assertThat(aktivitetsdager).isEqualTo(3)
        }
    }

    @Nested
    inner class DagligReiseTso {
        private fun lagreVilkårperiodeDagligReiseTso(aktivitetsdager: Int?) =
            LagreVilkårperiode(
                behandlingId = behandlingId,
                type = AktivitetType.UTDANNING,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsoDto(
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = aktivitetsdager,
                    ),
            )

        @Test
        fun `skal feile hvis aktivitetsdager mangler for ny aktivitet`() {
            assertThatThrownBy {
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTso(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    fødselFaktaGrunnlag = null,
                )
            }.hasMessageContaining("Mangler data: aktivitetsdager må være satt og være et heltall mellom 1 og 5")
        }

        @Test
        fun `skal ikke kreve aktivitetsdager ved oppdatering hvis det ikke var satt fra før`() {
            val faktaOgVurdering =
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTso(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = false,
                )

            val aktivitetsdager =
                (faktaOgVurdering as AktivitetDagligReiseTso)
                    .fakta
                    .takeIfFakta<FaktaAktivitetsdagerNullable>()
                    ?.aktivitetsdager

            assertThat(aktivitetsdager).isNull()
        }

        @Test
        fun `skal fortsatt kreve aktivitetsdager ved oppdatering hvis det var satt fra før`() {
            assertThatThrownBy {
                mapFaktaOgSvarDto(
                    vilkårperiode = lagreVilkårperiodeDagligReiseTso(aktivitetsdager = null),
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    fødselFaktaGrunnlag = null,
                    kreverAktivitetsdager = true,
                )
            }.hasMessageContaining("Mangler data: aktivitetsdager må være satt og være et heltall mellom 1 og 5")
        }
    }
}
