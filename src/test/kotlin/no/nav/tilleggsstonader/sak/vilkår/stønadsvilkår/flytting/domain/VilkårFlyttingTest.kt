package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VilkårFlyttingTest {
    private val vilkår =
        VilkårFlytting(
            behandlingId = BehandlingId.random(),
            fom = 1 januar 2026,
            tom = 15 januar 2026,
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            status = VilkårStatus.NY,
            delvilkårsett = emptyList(),
            fakta = FaktaFlyttingUbestemt(),
        )

    @Test
    fun `avviser fra-dato etter til-dato`() {
        assertThatThrownBy { vilkår.copy(fom = 16 januar 2026) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `godtar periode på én dag`() {
        val éndagsvilkår = vilkår.copy(tom = vilkår.fom)

        assertThat(éndagsvilkår.tom).isEqualTo(éndagsvilkår.fom)
    }
}
