package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.tilVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.tilVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
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
            delvilkår = listOf(Delvilkår(vurderinger = listOf(Vurdering(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE)))),
        )

    @Test
    fun `bevarer domenefelter og alle fakta-varianter gjennom mapping`() {
        listOf(
            FaktaFlyttingUbestemt("Testadresse"),
            FaktaFlytteSelv(100, null, 10, null, null, "Testadresse"),
            FaktaFlyttebyrå(FlyttebyråTilbud("A", 1000), FlyttebyråTilbud("B", null), "Testadresse"),
        ).forEach { fakta ->

            val opprinnelig = lagret.copy(fakta = fakta, status = null)
            val domene = opprinnelig.mapTilVilkårFlytting()
            assertThat(domene.mapTilVilkår().mapTilVilkårFlytting()).isEqualTo(domene)
            assertThat(domene.id).isEqualTo(opprinnelig.id)
            assertThat(domene.behandlingId).isEqualTo(opprinnelig.behandlingId)
            assertThat(domene.delvilkårsett).isEqualTo(opprinnelig.delvilkårsett)
            assertThat(domene.fakta).isSameAs(fakta)
            val dto = domene.tilVilkårFlyttingDto()
            assertThat(dto.id).isEqualTo(domene.id)
            assertThat(dto.fom).isEqualTo(domene.fom)
            assertThat(dto.tom).isEqualTo(domene.tom)
            assertThat(dto.resultat).isEqualTo(domene.resultat)
            assertThat(dto.status).isNull()
            assertThat(dto.delvilkårsett).hasSize(1)
            assertThat(FaktaFlyttingMapper.run { dto.fakta.tilDomain() }).isEqualTo(fakta)
        }
    }

    @Test
    fun `bevarer slettetkommentar i domene og sletterespons`() {
        val slettet = lagret.markerSlettet("Feilregistrert")
        val domene = slettet.mapTilVilkårFlytting()
        assertThat(domene.mapTilVilkår().slettetKommentar).isEqualTo("Feilregistrert")
        val respons = SlettetVilkårResultat(false, slettet).tilVilkårFlyttingDto()
        assertThat(respons.slettetPermanent).isFalse()
        assertThat(respons.vilkår).isEqualTo(domene.tilVilkårFlyttingDto())
        assertThat(respons.vilkår.slettetKommentar).isEqualTo("Feilregistrert")
        val permanent = SlettetVilkårResultat(true, lagret).tilVilkårFlyttingDto()
        assertThat(permanent.slettetPermanent).isTrue()
        assertThat(permanent.vilkår.id).isEqualTo(lagret.id)
    }

    @Test
    fun `avviser feil type og manglende fakta eller datoer`() {
        listOf(
            lagret.copy(type = VilkårType.EKSEMPEL),
            lagret.copy(fakta = null),
            lagret.copy(fom = null, tom = null),
        ).forEach { ugyldig ->
            assertThatThrownBy { ugyldig.mapTilVilkårFlytting() }.isInstanceOf(Feil::class.java)
        }
    }

    @Test
    fun `domenet validerer perioden og bruker inkluderende overlapp`() {
        val domene = lagret.mapTilVilkårFlytting()
        assertThat(domene.overlapper(Datoperiode(15 januar 2026, 16 januar 2026))).isTrue()
        assertThat(domene.overlapper(Datoperiode(16 januar 2026, 20 januar 2026))).isFalse()
        assertThat(domene.copy(tom = domene.fom).fom).isEqualTo(domene.fom)
        assertThatThrownBy { domene.copy(fom = 16 januar 2026) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
