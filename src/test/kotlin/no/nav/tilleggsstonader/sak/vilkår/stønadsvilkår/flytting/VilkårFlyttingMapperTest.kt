package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.tilVilkårFlyttingDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VilkårFlyttingMapperTest {
    private val lagret =
        vilkår(
            type = VilkårType.FLYTTING,
            fom = 1 januar 2026,
            tom = 15 januar 2026,
            utgift = null,
            fakta = FaktaFlyttingUbestemt("Testadresse"),
        )

    @Test
    fun `kopiering og mapping bevarer stabil identitet og opphav gjennom flere behandlinger`() {
        val førsteKopi = lagret.kopierTilBehandling(BehandlingId.random())
        val tilbakeført = førsteKopi.mapTilVilkårFlytting().mapTilVilkår()
        val andreKopi = tilbakeført.kopierTilBehandling(BehandlingId.random())
        assertThat(andreKopi.id).isNotEqualTo(lagret.id).isNotEqualTo(førsteKopi.id)
        assertThat(andreKopi.opphavsvilkår).isEqualTo(førsteKopi.opphavsvilkår)
        assertThat(tilbakeført.opphavsvilkår).isEqualTo(førsteKopi.opphavsvilkår)
    }

    @Test
    fun `bevarer null som status gjennom mapping`() {
        val domene = lagret.copy(status = null).mapTilVilkårFlytting()

        assertThat(domene.status).isNull()
        assertThat(domene.mapTilVilkår().status).isNull()
        assertThat(domene.tilVilkårFlyttingDto().status).isNull()
    }

    @Test
    fun `avviser feil vilkårstype`() {
        val ugyldig = lagret.copy(type = VilkårType.EKSEMPEL)

        assertThatThrownBy { ugyldig.mapTilVilkårFlytting() }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Ugyldig vilkårstype for flytting")
    }

    @Test
    fun `avviser manglende fakta`() {
        val ugyldig = lagret.copy(fakta = null)

        assertThatThrownBy { ugyldig.mapTilVilkårFlytting() }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fakta mangler for flytting")
    }

    @Test
    fun `avviser manglende datoer`() {
        val ugyldig = lagret.copy(fom = null, tom = null)

        assertThatThrownBy { ugyldig.mapTilVilkårFlytting() }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Forventer at fom er satt for flytting")
    }
}
