package no.nav.tilleggsstonader.sak.vedtak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslagskategori
import no.nav.tilleggsstonader.sak.vedtak.domain.gyldigeAvslagsårsaker
import no.nav.tilleggsstonader.sak.vedtak.domain.gyldigeÅrsakerForStønadstype
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ValiderGyldigÅrsakAvslagTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val validerGyldigÅrsakAvslag = ValiderGyldigÅrsakAvslag(vilkårperiodeService, vilkårService)

    val behandlingId = BehandlingId.random()

    @Test
    fun `sjekk at alle avslagsårsakene er med i valideringen`() {
        val alleÅrsakeneIValideringen = Stønadstype.entries.flatMap { gyldigeÅrsakerForStønadstype(it) }.toSet()
        val alleAvslagsårsaker = ÅrsakAvslag.entries.toSet()

        assertThat(alleAvslagsårsaker).containsExactlyInAnyOrderElementsOf(alleÅrsakeneIValideringen)
    }

    @Nested
    inner class `Valider at avslagsgrunn må være gyldig for stønadstype` {
        @Test
        fun `skal kaste feil dersom årsak ikke er gyldig for stønadstype`() {
            validerOgForventFeil(
                stønadstype = Stønadstype.LÆREMIDLER,
                årsaker = listOf(ÅrsakAvslag.HAR_IKKE_MERUTGIFTER), // Kun gyldig for BOUTGIFTER
                forventetFeilmelding = "ikke gyldige for LÆREMIDLER",
            )
        }

        @Test
        fun `skal ikke kaste valideringsfeil dersom årsak er i kategorien GENERELL`() {
            mockHentVilkårperioder()
            Stønadstype.entries.forEach { stønadstype ->
                val årsakerSomIkkeValideres = gyldigeAvslagsårsaker(stønadstype, Avslagskategori.GENERELL)

                årsakerSomIkkeValideres.forEach { avslagsårsak ->
                    validerOgForventSuksess(
                        stønadstype = stønadstype,
                        årsaker = listOf(avslagsårsak),
                    )
                }
            }
        }
    }

    @Nested
    inner class `Valider avslag på grunn av aktivitet` {
        @Test
        fun `gyldig å avslå på aktivitet dersom det finnes en ikke-oppfylt aktivitet`() {
            mockHentVilkårperioder(aktiviteter = listOf(ResultatVilkårperiode.IKKE_OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    validerOgForventSuksess(
                        stønadstype = stønadstype,
                        årsaker = listOf(årsakAvslagSomGjelderAktivitet),
                    )
                }
            }
        }

        @Test
        fun `gyldig å avslå på aktivitet så lenge det finnes minst én ikke-oppfylt aktivitet`() {
            mockHentVilkårperioder(aktiviteter = listOf(ResultatVilkårperiode.OPPFYLT, ResultatVilkårperiode.IKKE_OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    validerOgForventSuksess(
                        stønadstype = stønadstype,
                        årsaker = listOf(årsakAvslagSomGjelderAktivitet),
                    )
                }
            }
        }

        @Test
        fun `skal kaste feil om avslaggrunn er aktivitet dersom det ikke finnes noen aktivitet med resultat IKKE_OPPFYLT`() {
            mockHentVilkårperioder(aktiviteter = listOf(ResultatVilkårperiode.OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.AKTIVITET).forEach { årsakAvslagSomGjelderAktivitet ->
                    validerOgForventFeil(
                        stønadstype = stønadstype,
                        årsaker = listOf(årsakAvslagSomGjelderAktivitet),
                        forventetFeilmelding = "Kan ikke avslå med følgende årsaker",
                    )
                }
            }
        }
    }

    @Nested
    inner class `Valider avslag på grunn av målgruppe` {
        @Test
        fun `gyldig å avslå på målgruppe dersom det finnes en ikke-oppfylt målgruppe`() {
            mockHentVilkårperioder(målgrupper = listOf(ResultatVilkårperiode.IKKE_OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                validerOgForventSuksess(
                    stønadstype = stønadstype,
                    årsaker = listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                )
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er gyldig avslagårsak dersom det finnes minst en ikke oppfylt målgruppe`() {
            mockHentVilkårperioder(målgrupper = listOf(ResultatVilkårperiode.OPPFYLT, ResultatVilkårperiode.IKKE_OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                validerOgForventSuksess(
                    stønadstype = stønadstype,
                    årsaker = listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                )
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er ikke gyldig hvis det ikke er lagt inn en målgruppe som ikke er oppfylt`() {
            mockHentVilkårperioder(målgrupper = listOf(ResultatVilkårperiode.OPPFYLT))
            mockHentVilkår()

            Stønadstype.entries.forEach { stønadstype ->
                validerOgForventFeil(
                    stønadstype = stønadstype,
                    årsaker = listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                    forventetFeilmelding = "Kan ikke avslå med følgende årsaker",
                )
            }
        }
    }

    @Nested
    inner class `Valider avslag på bakgrunn av stønadsvilkår` {
        @Test
        fun `avslag på bakgrunn av stønadsvilkår er gyldig dersom det finnes et ikke-oppfylt stønadsvilkår`() {
            mockHentVilkårperioder()
            mockHentVilkår(listOf(vilkår(resultat = Vilkårsresultat.IKKE_OPPFYLT)))

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.STØNADSVILKÅR).forEach { avslagsgrunn ->
                    validerOgForventSuksess(
                        stønadstype = stønadstype,
                        årsaker = listOf(avslagsgrunn),
                    )
                }
            }
        }

        @Test
        fun `avslag på bakgrunn av stønadsvilkår er ikke gyldig hvis det ikke finnes et ikke oppfylt stønadsvilkår`() {
            mockHentVilkårperioder()
            mockHentVilkår(listOf(vilkår(resultat = Vilkårsresultat.OPPFYLT)))

            Stønadstype.entries.forEach { stønadstype ->
                gyldigeAvslagsårsaker(stønadstype, Avslagskategori.STØNADSVILKÅR).forEach { avslagsgrunn ->
                    validerOgForventFeil(
                        stønadstype = stønadstype,
                        årsaker = listOf(avslagsgrunn),
                        forventetFeilmelding = "Kan ikke avslå med følgende årsaker",
                    )
                }
            }
        }
    }

    private fun validerOgForventFeil(
        stønadstype: Stønadstype,
        årsaker: List<ÅrsakAvslag>,
        forventetFeilmelding: String,
    ) {
        assertThatThrownBy {
            validerGyldigÅrsakAvslag.validerAvslagErGyldig(
                behandlingId = behandlingId,
                årsakerAvslag = årsaker,
                stønadstype = stønadstype,
            )
        }.hasMessageContaining(forventetFeilmelding)
    }

    private fun validerOgForventSuksess(
        stønadstype: Stønadstype,
        årsaker: List<ÅrsakAvslag>,
    ) {
        assertDoesNotThrow {
            validerGyldigÅrsakAvslag.validerAvslagErGyldig(
                behandlingId = behandlingId,
                årsakerAvslag = årsaker,
                stønadstype = stønadstype,
            )
        }
    }

    private fun mockHentVilkårperioder(
        aktiviteter: List<ResultatVilkårperiode> = emptyList(),
        målgrupper: List<ResultatVilkårperiode> = emptyList(),
    ) {
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
            Vilkårperioder(
                aktiviteter = aktiviteter.map { aktivitet(behandlingId = behandlingId, resultat = it) },
                målgrupper = målgrupper.map { målgruppe(behandlingId = behandlingId, resultat = it) },
            )
    }

    private fun mockHentVilkår(vilkår: List<Vilkår> = emptyList()) {
        every { vilkårService.hentVilkår(behandlingId) } returns vilkår
    }
}
