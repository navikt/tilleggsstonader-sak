package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juli
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagFødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AldersvilkårVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ArbeidssøkerFlyttingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdagerNullable
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFlyttingTsrType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OpphørtOmstillingsstønadFlyttingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OpphørtOvergangsstønadFlyttingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktaOgVurderingMapperTest {
    private val behandlingId = BehandlingId.random()

    @Test
    fun `opphørt stønad mappes til målgruppe uten vurderinger for flytting TSO`() {
        val types =
            listOf(
                MålgruppeType.OPPHØRT_OMSTILLINGSSTØNAD to OpphørtOmstillingsstønadFlyttingTso,
                MålgruppeType.OPPHØRT_OVERGANGSSTØNAD to OpphørtOvergangsstønadFlyttingTso,
            )

        types.forEach { (type, forventetFaktaOgVurdering) ->
            val vilkårperiode =
                LagreVilkårperiode(
                    behandlingId = behandlingId,
                    type = type,
                    fom = 1 januar 2025,
                    tom = 1 juli 2025,
                    faktaOgSvar = FaktaOgSvarMålgruppeDto(),
                )

            val faktisk =
                mapFaktaOgSvarDto(
                    vilkårperiode = vilkårperiode,
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    fødselFaktaGrunnlag = null,
                )

            assertThat(faktisk).isEqualTo(forventetFaktaOgVurdering)
            assertThat(faktisk.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }

    @Nested
    inner class LønnetFlyttingTso {
        private val målgrupperMedLønnet =
            listOf(
                MålgruppeType.AAP,
                MålgruppeType.UFØRETRYGD,
                MålgruppeType.NEDSATT_ARBEIDSEVNE,
                MålgruppeType.AKTIVITETSPENGER,
            )

        private fun map(
            type: MålgruppeType,
            svarLønnet: SvarJaNei?,
        ) = mapFaktaOgSvarDto(
            vilkårperiode =
                LagreVilkårperiode(
                    behandlingId = behandlingId,
                    type = type,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    faktaOgSvar =
                        FaktaOgSvarMålgruppeDto(
                            svarMedlemskap = SvarJaNei.JA,
                            svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.NEI,
                            svarMottarSykepengerForFulltidsstilling = SvarJaNei.NEI,
                            svarLønnet = svarLønnet,
                        ),
                ),
            stønadstype = Stønadstype.FLYTTING_TSO,
            fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(),
        )

        @Test
        fun `nei på lønnet gir oppfylt`() {
            målgrupperMedLønnet.forEach { type ->
                val faktaOgVurdering = map(type, SvarJaNei.NEI)

                assertThat((faktaOgVurdering.vurderinger as LønnetVurdering).lønnet.svar).isEqualTo(SvarJaNei.NEI)
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            }
        }

        @Test
        fun `ja på lønnet gir ikke oppfylt`() {
            målgrupperMedLønnet.forEach { type ->
                assertThat(map(type, SvarJaNei.JA).utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }

        @Test
        fun `manglende svar på lønnet gir ikke vurdert`() {
            målgrupperMedLønnet.forEach { type ->
                assertThat(map(type, null).utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            }
        }
    }

    @Test
    fun `aktivitetspenger har aldersvilkårsvurdering`() {
        val målgruppe =
            LagreVilkårperiode(
                behandlingId = behandlingId,
                type = MålgruppeType.AKTIVITETSPENGER,
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                faktaOgSvar = FaktaOgSvarMålgruppeDto(),
            )

        val faktaOgVurdering =
            mapFaktaOgSvarDto(
                vilkårperiode = målgruppe,
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(),
            )

        assertThat(faktaOgVurdering.vurderinger).isInstanceOf(AldersvilkårVurdering::class.java)
        assertThat((faktaOgVurdering.vurderinger as AldersvilkårVurdering).aldersvilkår.svar).isEqualTo(SvarJaNei.JA)
        assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    @Test
    fun `arbeidssøker for flytting TSR mappes uten fakta og vurderinger`() {
        val målgruppe =
            LagreVilkårperiode(
                behandlingId = behandlingId,
                type = MålgruppeType.ARBEIDSSØKER,
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                faktaOgSvar = FaktaOgSvarMålgruppeDto(),
            )

        val faktaOgVurdering =
            mapFaktaOgSvarDto(
                vilkårperiode = målgruppe,
                stønadstype = Stønadstype.FLYTTING_TSR,
                fødselFaktaGrunnlag = null,
            )

        assertThat(faktaOgVurdering).isEqualTo(ArbeidssøkerFlyttingTsr())
        assertThat(faktaOgVurdering.type).isEqualTo(MålgruppeFlyttingTsrType.ARBEIDSSØKER_FLYTTING_TSR)
        assertThat(faktaOgVurdering.fakta).isEqualTo(IngenFakta)
        assertThat(faktaOgVurdering.vurderinger).isEqualTo(IngenVurderinger)
        assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

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
