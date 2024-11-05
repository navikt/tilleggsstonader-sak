package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.SlåSammenPeriodeGrunnlagYtelseUtil.slåSammenOverlappendeEllerPåfølgende
import org.assertj.core.api.Assertions.assertThat

enum class PeriodeGrunnlagYtelseNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    TYPE("Type"),
    ENSLIG_FORSØRGER_STØNADSTYPE("Enslig forsørger stønadstype"),
}

class SlåSammenPeriodeGrunnlagYtelseStepDefinition {

    var grunnlagsperioderforYtelse: List<PeriodeGrunnlagYtelse> = emptyList()
    var resultat = emptyList<PeriodeGrunnlagYtelse>()

    @Gitt("Følgende grunnlagsperioderfor ytelse")
    fun `Følgende grunnlagsperioderfor ytelse`(dataTable: DataTable) {
        grunnlagsperioderforYtelse = dataTable.mapRad { rad ->
            PeriodeGrunnlagYtelse(
                type = parseEnum<TypeYtelsePeriode>(PeriodeGrunnlagYtelseNøkler.TYPE, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad),
                ensligForsørgerStønadstype = parseValgfriEnum<EnsligForsørgerStønadstype>(PeriodeGrunnlagYtelseNøkler.ENSLIG_FORSØRGER_STØNADSTYPE, rad),
            )
        }
    }

    @Når("Slår sammen grunnlagsperioder")
    fun `Slår sammen grunnlagsperioder`() {
        resultat = grunnlagsperioderforYtelse.slåSammenOverlappendeEllerPåfølgende()
    }

    @Så("Forvent grunnlagsperioderfor ytelse")
    fun `Forvent grunnlagsperioderfor ytelse`(dataTable: DataTable) {
        val forventet = dataTable.mapRad { rad ->
            PeriodeGrunnlagYtelse(
                type = parseEnum<TypeYtelsePeriode>(PeriodeGrunnlagYtelseNøkler.TYPE, rad),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad),
                ensligForsørgerStønadstype = parseValgfriEnum<EnsligForsørgerStønadstype>(PeriodeGrunnlagYtelseNøkler.ENSLIG_FORSØRGER_STØNADSTYPE, rad),
            )
        }
        assertThat(resultat).isEqualTo(forventet)
    }
}
