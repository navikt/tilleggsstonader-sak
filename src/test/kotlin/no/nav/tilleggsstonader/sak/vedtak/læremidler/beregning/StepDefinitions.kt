package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.mapStønadsperioder
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

enum class BeregningNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STUDIENIVÅ("Studienivå"),
    STUDIEPROSENT("Studieprosent"),
    SATS("Sats"),
    AKTIVITET("Aktivitet"),
}

class StepDefinitions {
    val vilkårperiodeRepository = mockk<VilkårperiodeRepository>()
    val stønadsperiodeRepository = mockk<StønadsperiodeRepository>()
    val læremidlerBeregningService = LæremidlerBeregningService(vilkårperiodeRepository, stønadsperiodeRepository)

    val behandlingId = BehandlingId(UUID.randomUUID())

    var vedtaksPerioder: List<VedtaksPeriode> = emptyList()
    var resultat: BeregningsresultatLæremidler? = null

    var resultatValidering: Stønadsperiode? = null

    @Gitt("følgende vedtaksperioder for læremidler")
    fun `følgende beregningsperiode for læremidler`(dataTable: DataTable) {
        vedtaksPerioder = dataTable.mapRad { rad ->
            VedtaksPeriode(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
            )
        }
    }

    @Gitt("følgende aktiviteter for læremidler")
    fun `følgende aktiviteter`(dataTable: DataTable) {
        every {
            vilkårperiodeRepository.findByBehandlingIdAndResultat(
                any(),
                any(),
            )
        } returns mapAktiviteter(behandlingId, dataTable)
    }

    @Gitt("følgende stønadsperioder for læremidler")
    fun `følgende stønadsperioder`(dataTable: DataTable) {
        every {
            stønadsperiodeRepository.findAllByBehandlingId(
                any(),
            )
        } returns mapStønadsperioder(behandlingId, dataTable)
    }


    @Når("beregner stønad for læremidler")
    fun `beregner stønad for læremidler`() {
        resultat = læremidlerBeregningService.beregn(vedtaksPerioder!!, behandlingId)
    }

    @Så("skal stønaden være")
    fun `skal stønaden være`(dataTable: DataTable) {
        val perioder = dataTable.mapRad { rad ->
            BeregningsresultatForMåned(
                beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
                grunnlag = Beregningsgrunnlag(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    studienivå = parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad)
                        ?: Studienivå.HØYERE_UTDANNING,
                    studieprosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
                    sats = parseBigDecimal(BeregningNøkler.SATS, rad).toInt(),
                ),
            )
        }
        val forventetBeregningsresultat = BeregningsresultatLæremidler(
            perioder = perioder,
        )
        assertThat(resultat).isEqualTo(forventetBeregningsresultat)
    }

    @Når("validerer vedtaksperiode for læremidler")
    fun `validerer vedtaksperiode for læremidler`() {
        val vedtaksperiode = vedtaksPerioder!!.single()
        resultatValidering = læremidlerBeregningService.validerVedtaksperiode(vedtaksperiode, behandlingId)
    }

    @Så("skal resultat fra validering være")
    fun `skal resultat fra validering være`(dataTable: DataTable) {
        val stønadsperiode = dataTable.mapRad { rad ->
            Stønadsperiode(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                aktivitet = AktivitetType.TILTAK,
                målgruppe = MålgruppeType.AAP,
            )
        }.single()

        assertThat(resultatValidering?.fom).isEqualTo(stønadsperiode.fom)
        assertThat(resultatValidering?.tom).isEqualTo(stønadsperiode.tom)
    }
}
