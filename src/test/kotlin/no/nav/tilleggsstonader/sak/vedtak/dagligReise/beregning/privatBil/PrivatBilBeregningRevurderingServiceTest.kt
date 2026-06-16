package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PrivatBilBeregningRevurderingServiceTest {
    private val unleashService = mockk<UnleashService>()
    private val service = PrivatBilBeregningRevurderingService(unleashService)

    private val reiseId = ReiseId.random()

    private fun reiseMedStatus(
        status: VilkårStatus,
        reiseId: ReiseId = this.reiseId,
        fom: java.time.LocalDate = 1 januar 2025,
        tom: java.time.LocalDate = 31 mars 2025,
    ) = ReiseMedPrivatBil(
        fom = fom,
        tom = tom,
        reiseId = reiseId,
        aktivitetsadresse = null,
        aktivitetType = no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType.TILTAK,
        tiltaksvariant = null,
        delPerioder = emptyList(),
        reiseavstandEnVei = java.math.BigDecimal.TEN,
        status = status,
    )

    @BeforeEach
    fun setup() {
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true
    }

    @Nested
    inner class NyReise {
        @Test
        fun `reise med status NY bruker alltid nytt rammevedtak`() {
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 31 mars 2025)
            val forrigeRammevedtakForReiseForReise = rammevedtakPrivatBil(reiseId = ReiseId.random())

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.NY)),
                    forrigeRammevedtak = forrigeRammevedtakForReiseForReise,
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 januar 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }

        @Test
        fun `reise med status NY kaster feil dersom rammevedtak for ny reise mangler`() {
            assertThatThrownBy {
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.NY)),
                    forrigeRammevedtak = rammevedtakPrivatBil(reiseId = ReiseId.random()),
                    nyttRammevedtak = null,
                    tidligsteEndring = 1 februar 2025,
                )
            }.hasMessageContaining("Forventer at det finnes et nytt rammmevedtak for nye reiser")
        }
    }

    @Nested
    inner class SlettetReise {
        @Test
        fun `reise med status SLETTET returnerer alltid null (utelates fra resultat)`() {
            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.SLETTET)),
                    forrigeRammevedtak = rammevedtakPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 februar 2025),
                    nyttRammevedtak = null,
                    tidligsteEndring = 1 mars 2025,
                )

            assertThat(resultat).isNull()
        }

        @Test
        fun `reise med status SLETTET returnerer null selv om perioden er før tidligste endring`() {
            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.SLETTET, fom = 1 januar 2025, tom = 28 januar 2025)),
                    forrigeRammevedtak = rammevedtakPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025),
                    nyttRammevedtak = null,
                    tidligsteEndring = 1 mars 2025,
                )

            assertThat(resultat).isNull()
        }
    }

    @Nested
    inner class UendretReise {
        @Test
        fun `reise med status UENDRET som er ferdig FØR tidligsteEndring bruker gammelt`() {
            val forrigeRammevedtakForReiseForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.UENDRET, fom = 1 januar 2025, tom = 28 januar 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReiseForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 mars 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(forrigeRammevedtakForReiseForReise)
        }

        @Test
        fun `reise med status UENDRET som overlapper tidligsteEndring bruker nytt`() {
            // Bruker empty list for å se forskjell på om nytt eller gammel rammevedtak ble brukt.
            // Begge burde i utgangspunktet være like.
            val reiserIForrigeRammevedtak = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 31 mars 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 31 mars 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.UENDRET, fom = 1 januar 2025, tom = 31 mars 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(reiserIForrigeRammevedtak)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 februar 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }

        @Test
        fun `reise med status UENDRET med tidligsteEndring null kaster feil`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)

            assertThatThrownBy {
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.UENDRET, fom = 1 januar 2025, tom = 28 januar 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = null,
                )
            }.hasMessageContaining("Forventer at tidligste endring finnes for en revurdering")
        }
    }

    @Nested
    inner class EndretReise {
        @Test
        fun `reise med status ENDRET der forrige-periode overlapper tidligsteEndring bruker nytt`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 30 april 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 februar 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.ENDRET, fom = 1 januar 2025, tom = 28 februar 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 mars 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }

        @Test
        fun `reise med status ENDRET der ny periode overlapper tidligsteEndring bruker nytt`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 30 april 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.ENDRET, fom = 1 januar 2025, tom = 30 april 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 februar 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }

        @Test
        fun `reise med status ENDRET der begge perioder er FØR tidligsteEndring bruker gammelt`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.ENDRET, fom = 1 januar 2025, tom = 28 januar 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 mars 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(forrigeRammevedtakForReise)
        }

        @Test
        fun `reise med status ENDRET skal bruke ny ramme dersom hele perioden har flyttet seg fremover i tid`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 februar 2025, tom = 31 mars 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.ENDRET, fom = 1 februar 2025, tom = 31 mars 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 januar 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }

        @Test
        fun `reise med status ENDRET skal bruke ny ramme dersom hele perioden har flyttet seg bakover i tid`() {
            val forrigeRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 februar 2025, tom = 31 mars 2025)
            val nyttRammevedtakForReise = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = 1 januar 2025, tom = 28 januar 2025)

            val resultat =
                service.beregnRammevedtakVedRevurdering(
                    reiserMedBil = listOf(reiseMedStatus(VilkårStatus.ENDRET, fom = 1 februar 2025, tom = 31 mars 2025)),
                    forrigeRammevedtak = RammevedtakPrivatBil(reiser = listOf(forrigeRammevedtakForReise)),
                    nyttRammevedtak = RammevedtakPrivatBil(reiser = listOf(nyttRammevedtakForReise)),
                    tidligsteEndring = 1 januar 2025,
                )

            assertThat(resultat!!.reiser).containsExactly(nyttRammevedtakForReise)
        }
    }
}
