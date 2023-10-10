package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class RegelValideringTest {

    @Test
    fun `sender in en tom liste med svar - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(regel, *emptyArray<VurderingDto>())
        }.hasMessage("Savner svar for en av delvilkåren for vilkår=EKSEMPEL")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in svar med feil rootId - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN2),
            )
        }.hasMessageStartingWith("Delvilkårsvurderinger savner svar på hovedregler")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in 2 svar men mangler svarId på første - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN),
                VurderingDto(RegelId.HAR_ET_NAVN2),
            )
        }.hasMessage(
            "Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=EKSEMPEL " +
                "regelId=HAR_ET_NAVN",
        )
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in fler svar enn det finnes mulighet for - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN, SvarId.NEI),
                VurderingDto(RegelId.HAR_ET_NAVN2, SvarId.NEI),
                VurderingDto(RegelId.HAR_ET_NAVN2),
            )
        }.hasMessageStartingWith("Finnes ikke noen flere regler, men finnes flere svar")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `regelId for det andre spørsmålet er feil - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN, SvarId.NEI),
                VurderingDto(RegelId.HAR_ET_NAVN3, SvarId.JA),
            )
        }.hasMessage("Finner ikke regelId=HAR_ET_NAVN3 for vilkårType=EKSEMPEL")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har begrunnelse på ett spørsmål som ikke skal ha begrunnelse - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "b"),
            )
        }.hasMessage(
            "Begrunnelse for vilkårType=EKSEMPEL regelId=HAR_ET_NAVN " +
                "svarId=JA skal ikke ha begrunnelse",
        )
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har en tom begrunnelse på ett spørsmål som ikke skal ha begrunnelse - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThatThrownBy {
            valider(
                regel,
                VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "      "),
            )
        }.hasMessage(
            "Begrunnelse for vilkårType=EKSEMPEL regelId=HAR_ET_NAVN " +
                "svarId=JA skal ikke ha begrunnelse",
        )
            .isInstanceOf(Feil::class.java)
    }

    private fun valider(
        regel: Vilkårsregel,
        vararg vurderinger: VurderingDto,
    ) {
        valider(regel, delvilkårDto(*vurderinger))
    }

    private fun valider(
        regel: Vilkårsregel,
        vararg delvilkårDto: DelvilkårDto,
    ) {
        RegelValidering.validerVilkår(
            vilkårsregel = regel,
            oppdatertDelvilkårsett = delvilkårDto.toList(),
            tidligereDelvilkårsett = regel.initiereDelvilkår(mockk()),
        )
    }
}
