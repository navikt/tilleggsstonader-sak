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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.SlåSammenPeriodeGrunnlagYtelseUtil.slåSammenOverlappendeEllerPåfølgende
import org.assertj.core.api.Assertions.assertThat

enum class PeriodeGrunnlagYtelseNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    TYPE("Type"),
    SUBTYPE("Subtype"),
}

@Suppress("ktlint:standard:function-naming", "unused")
class SlåSammenPeriodeGrunnlagYtelseStepDefinition {
    var grunnlagsperioderforYtelse: List<PeriodeGrunnlagYtelse> = emptyList()
    var resultat = emptyList<PeriodeGrunnlagYtelse>()

    @Gitt("Følgende grunnlagsperioderfor ytelse")
    fun `Følgende grunnlagsperioderfor ytelse`(dataTable: DataTable) {
        grunnlagsperioderforYtelse =
            dataTable.mapRad { rad ->
                lagPeriode(
                    type = parseEnum<TypeYtelsePeriode>(PeriodeGrunnlagYtelseNøkler.TYPE, rad),
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad),
                    subtype =
                        parseValgfriEnum<PeriodeGrunnlagYtelse.YtelseSubtype>(
                            PeriodeGrunnlagYtelseNøkler.SUBTYPE,
                            rad,
                        ),
                )
            }
    }

    @Når("Slår sammen grunnlagsperioder")
    fun `Slår sammen grunnlagsperioder`() {
        resultat = grunnlagsperioderforYtelse.slåSammenOverlappendeEllerPåfølgende()
    }

    @Så("Forvent grunnlagsperioderfor ytelse")
    fun `Forvent grunnlagsperioderfor ytelse`(dataTable: DataTable) {
        val forventet =
            dataTable.mapRad { rad ->
                lagPeriode(
                    type = parseEnum<TypeYtelsePeriode>(PeriodeGrunnlagYtelseNøkler.TYPE, rad),
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad),
                    subtype =
                        parseValgfriEnum<PeriodeGrunnlagYtelse.YtelseSubtype>(
                            PeriodeGrunnlagYtelseNøkler.SUBTYPE,
                            rad,
                        ),
                )
            }
        assertThat(resultat).isEqualTo(forventet)
    }

    private fun lagPeriode(
        type: TypeYtelsePeriode,
        fom: java.time.LocalDate,
        tom: java.time.LocalDate?,
        subtype: PeriodeGrunnlagYtelse.YtelseSubtype?,
    ): PeriodeGrunnlagYtelse =
        when (type) {
            TypeYtelsePeriode.AAP -> PeriodeGrunnlagYtelse.AAP(fom = fom, tom = tom, subtype = subtype)
            TypeYtelsePeriode.DAGPENGER -> PeriodeGrunnlagYtelse.Dagpenger(fom = fom, tom = tom)
            TypeYtelsePeriode.ENSLIG_FORSØRGER ->
                PeriodeGrunnlagYtelse.EnsligForsørger(
                    fom = fom,
                    tom = tom,
                    subtype = subtype,
                    erNyttRegelverk2026 = false,
                )

            TypeYtelsePeriode.OMSTILLINGSSTØNAD -> PeriodeGrunnlagYtelse.Omstillingsstønad(fom = fom, tom = tom)
            TypeYtelsePeriode.TILTAKSPENGER_TPSAK -> PeriodeGrunnlagYtelse.TiltakspengerTPSak(fom = fom, tom = tom)
            TypeYtelsePeriode.TILTAKSPENGER_ARENA -> PeriodeGrunnlagYtelse.TiltakspengerArena(fom = fom, tom = tom)
        }
}
