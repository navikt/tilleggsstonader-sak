package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class VilkårDagligReiseTest {
    @Test
    fun `skal kunne opprette vilkår daglig reise`() {
        assertDoesNotThrow {
            VilkårDagligReise(
                behandlingId = BehandlingId.random(),
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                resultat = Vilkårsresultat.OPPFYLT,
                status = VilkårStatus.NY,
                delvilkårsett = emptyList(),
                fakta = null,
            )
        }
    }

    @Test
    fun `skal kaste feil hvis fakta ikke er av forventet type`() {
        val faktaPrivatBil =
            FaktaPrivatBil(
                reisedagerPerUke = 4,
                reiseavstandEnVei = 10,
                prisBompengerPerDag = 0,
                prisFergekostandPerDag = 0,
            )
        val feil =
            assertThrows<Feil> {
                VilkårDagligReise(
                    behandlingId = BehandlingId.random(),
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    delvilkårsett = emptyList(),
                    fakta = faktaPrivatBil,
                )
            }
        assertThat(feil.message).isEqualTo("Foreløpig støttes kun fakta av typen offentlig transport")
    }

    @Test
    fun `skal kaste feil hvis fakta ikke er null når resulat er ikke oppfylt`() {
        val faktaOffentligTransport =
            FaktaOffentligTransport(
                reisedagerPerUke = 4,
                prisEnkelbillett = 44,
                prisSyvdagersbillett = 200,
                prisTrettidagersbillett = 780,
            )

        val feil =
            assertThrows<Feil> {
                VilkårDagligReise(
                    behandlingId = BehandlingId.random(),
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    resultat = Vilkårsresultat.IKKE_OPPFYLT,
                    status = VilkårStatus.NY,
                    delvilkårsett = emptyList(),
                    fakta = faktaOffentligTransport,
                )
            }
        assertThat(feil.message).isEqualTo("Fakta må være null når resultat er ikke oppfylt")
    }
}
