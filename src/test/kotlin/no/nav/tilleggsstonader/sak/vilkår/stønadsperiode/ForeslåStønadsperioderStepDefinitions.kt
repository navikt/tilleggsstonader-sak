package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat

class ForeslåStønadsperioderStepDefinitions {
    var aktiviteter: List<Vilkårperiode> = emptyList()
    var målgrupper: List<Vilkårperiode> = emptyList()
    var resultat: List<StønadsperiodeDto> = emptyList()
    var feil: ApiFeil? = null

    @Gitt("følgende vilkårsperioder med aktiviteter")
    fun `følgende vilkårsperioder med aktiviteter`(dataTable: DataTable) {
        aktiviteter = dataTable.mapRad { rad ->
            aktivitet(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(
                    type = AktivitetType.valueOf(rad["type"]!!),
                ),

            )
        }
    }

    @Gitt("følgende vilkårsperioder med målgrupper")
    fun `følgende vilkårsperioder med målgrupper`(dataTable: DataTable) {
        målgrupper = dataTable.mapRad { rad ->
            målgruppe(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering = faktaOgVurderingMålgruppe(
                    type = MålgruppeType.valueOf(rad["type"]!!),
                ),
            )
        }
    }

    @Når("forslag til stønadsperioder lages")
    fun `forslag til stønadsperioder lages`() {
        try {
            resultat = ForeslåStønadsperiode.finnStønadsperioder(
                Vilkårperioder(
                    målgrupper = målgrupper,
                    aktiviteter = aktiviteter,
                ),
            )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Så("forvent følgende beregningsfeil: {}")
    fun `forvent følgende beregningsfeil`(feil: String) {
        assertThat(this.feil).isNotNull
        assertThat(this.feil?.message).isEqualTo(feil)
    }

    @Så("forvent følgende stønadsperioder")
    fun `forvent følgende beregningsresultat`(dataTable: DataTable) {
        val forventetStønadsperioder = dataTable.mapRad { rad ->
            StønadsperiodeDto(
                id = null,
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                målgruppe = MålgruppeType.valueOf(rad["målgruppe"]!!),
                aktivitet = AktivitetType.valueOf(rad["aktivitet"]!!),
                status = null,
            )
        }

        assertThat(resultat).isEqualTo(forventetStønadsperioder)
    }
}
