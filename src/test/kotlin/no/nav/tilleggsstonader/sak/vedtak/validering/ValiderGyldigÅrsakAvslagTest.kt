package no.nav.tilleggsstonader.sak.vedtak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslagskategori
import no.nav.tilleggsstonader.sak.vedtak.domain.gyldigeAvslagsårsaker
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ValiderGyldigÅrsakAvslagTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val årsakAvslagValideringService = ÅrsakAvslagValideringService(vilkårperiodeService, vilkårService)

    val behandlingId = BehandlingId.random()

    @Nested
    inner class `Valider at avslagsgrunn må være gyldig for stønadstype` {
        @Test
        fun `skal kaste feil dersom årsak ikke er gyldig for stønadstype`() {
            assertThatThrownBy {
                årsakAvslagValideringService.validerAvslagErGyldig(
                    behandlingId = behandlingId,
                    årsakerAvslag = listOf(ÅrsakAvslag.HAR_IKKE_MERUTGIFTER), // Kun gyldig for BOUTGIFTER
                    stønadstype = Stønadstype.LÆREMIDLER,
                )
            }.hasMessageContaining("ikke gyldige for LÆREMIDLER")
        }

        @Test
        fun `skal ikke kaste valideringsfeil dersom årsak er ANNET`() {
            assertDoesNotThrow {
                årsakAvslagValideringService.validerAvslagErGyldig(
                    behandlingId = behandlingId,
                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                    stønadstype = Stønadstype.BARNETILSYN,
                )
            }
        }
    }

    @Nested
    inner class `Valider avslag på grunn av aktivitet` {
        @Test
        fun `gyldig å avslå på aktivitet dersom det finnes en ikke-oppfylt aktivitet`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter =
                        listOf(
                            aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                        ),
                    målgrupper = emptyList(),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    assertDoesNotThrow {
                        årsakAvslagValideringService.validerAvslagErGyldig(
                            behandlingId = behandlingId,
                            årsakerAvslag = listOf(årsakAvslagSomGjelderAktivitet),
                            stønadstype = stønadstype,
                        )
                    }
                }
            }
        }

        @Test
        fun `gyldig å avslå på aktivitet så lenge det finnes minst én ikke-oppfylt aktivitet`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter =
                        listOf(
                            aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                        ),
                    målgrupper = emptyList(),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    assertDoesNotThrow {
                        årsakAvslagValideringService.validerAvslagErGyldig(
                            behandlingId = behandlingId,
                            årsakerAvslag = listOf(årsakAvslagSomGjelderAktivitet),
                            stønadstype = stønadstype,
                        )
                    }
                }
            }
        }

        @Test
        fun `skal kaste feil om avslaggrunn er aktivitet dersom det ikke finnes en aktivitet med resultat ikke oppfylt`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter =
                        listOf(
                            aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                        ),
                    målgrupper = emptyList(),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    assertThatThrownBy {
                        årsakAvslagValideringService.validerAvslagErGyldig(
                            behandlingId = behandlingId,
                            årsakerAvslag = listOf(årsakAvslagSomGjelderAktivitet),
                            stønadstype = stønadstype,
                        )
                    }.hasMessageContaining("asdasd")
                }
            }
        }
    }

    @Nested
    inner class `Valider avslag på grunn av målgruppe` {
        @Test
        fun `IKKE_I_MÅLGRUPPE er gyldig avslagårsak dersom det finnes en ikke oppfylt målgruppe`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter = emptyList(),
                    målgrupper =
                        listOf(
                            målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                        ),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->
                assertDoesNotThrow {
                    årsakAvslagValideringService.validerAvslagErGyldig(
                        behandlingId = behandlingId,
                        årsakerAvslag = listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                        stønadstype = stønadstype,
                    )
                }
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er gyldig avslagårsak dersom det finnes minst en ikke oppfylt målgruppe`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter = emptyList(),
                    målgrupper =
                        listOf(
                            målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                        ),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->
                assertDoesNotThrow {
                    årsakAvslagValideringService.validerAvslagErGyldig(
                        behandlingId,
                        listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                        stønadstype,
                    )
                }
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er ikke gyldig hvis det ikke er lagt inn en målgruppe som ikke er oppfylt`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter = emptyList(),
                    målgrupper =
                        listOf(
                            målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                        ),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

            Stønadstype.entries.forEach { stønadstype ->

                assertThatThrownBy {
                    årsakAvslagValideringService.validerAvslagErGyldig(
                        behandlingId,
                        listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                        stønadstype,
                    )
                }.hasMessageContaining("målgruppeårasdsadsaker")
            }
        }
    }

    @Nested
    inner class ValiderStønadsvilkårBougifter {
        @Test
        fun `avslag på bakgrunn av stønadsvilkår er gyldig dersom det finnes et ikke-oppfylt stønadsvilkår`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter = emptyList(),
                    målgrupper = emptyList(),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns
                listOf(
                    Vilkår(
                        id = VilkårId.random(),
                        behandlingId = behandlingId,
                        type = VilkårType.EKSEMPEL,
                        resultat = Vilkårsresultat.IKKE_OPPFYLT,
                        status = VilkårStatus.NY,
                        erFremtidigUtgift = false,
                        delvilkårwrapper = DelvilkårWrapper(emptyList()),
                        opphavsvilkår = null,
                        gitVersjon = null,
                    ),
                )

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.STØNADSVILKÅR).forEach { avslagsgrunn ->
                    assertDoesNotThrow {
                        årsakAvslagValideringService.validerAvslagErGyldig(
                            behandlingId = behandlingId,
                            årsakerAvslag = listOf(avslagsgrunn),
                            stønadstype = stønadstype,
                        )
                    }
                }
            }
        }

        @Test
        fun `avslag på bakgrunn av stønadsvilkår er ikke gyldig hvis det ikke finnes et ikke oppfylt stønadsvilkår`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                Vilkårperioder(
                    aktiviteter = emptyList(),
                    målgrupper = emptyList(),
                )

            every { vilkårService.hentVilkår(behandlingId) } returns
                listOf(
                    Vilkår(
                        id = VilkårId.random(),
                        behandlingId = behandlingId,
                        type = VilkårType.EKSEMPEL,
                        resultat = Vilkårsresultat.OPPFYLT,
                        status = VilkårStatus.NY,
                        erFremtidigUtgift = false,
                        delvilkårwrapper = DelvilkårWrapper(emptyList()),
                        opphavsvilkår = null,
                        gitVersjon = null,
                    ),
                )

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.STØNADSVILKÅR).forEach { avslagsgrunn ->
                    assertThatThrownBy {
                        årsakAvslagValideringService.validerAvslagErGyldig(
                            behandlingId,
                            listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON),
                            Stønadstype.BOUTGIFTER,
                        )
                    }.hasMessageContaining("stønadsvilkårårsaker")
                }
            }
        }
    }
}
