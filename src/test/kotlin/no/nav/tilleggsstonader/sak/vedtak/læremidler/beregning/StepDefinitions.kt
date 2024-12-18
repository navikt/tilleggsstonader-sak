package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.mapStønadsperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.delTilUtbetalingsPerioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.util.UUID

enum class BeregningNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STUDIENIVÅ("Studienivå"),
    STUDIEPROSENT("Studieprosent"),
    SATS("Sats"),
    AKTIVITET("Aktivitet"),
    UTBETALINGSMÅNED("Utbetalingsmåned"),
    MÅLGRUPPE("Målgruppe"),
}

class StepDefinitions {

    val logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepository = mockk<VilkårperiodeRepository>()
    val stønadsperiodeRepository = mockk<StønadsperiodeRepository>()
    val læremidlerBeregningService = LæremidlerBeregningService(vilkårperiodeRepository, stønadsperiodeRepository)

    val behandlingId = BehandlingId(UUID.randomUUID())

    var vedtaksPerioder: List<Vedtaksperiode> = emptyList()
    var stønadsperioder: List<Stønadsperiode> = emptyList()
    var resultat: BeregningsresultatLæremidler? = null

    var beregningException: Exception? = null
    var valideringException: Exception? = null

    var vedtaksperioderSplittet: List<UtbetalingPeriode> = emptyList()

    @Gitt("følgende vedtaksperioder for læremidler")
    fun `følgende beregningsperiode for læremidler`(dataTable: DataTable) {
        vedtaksPerioder = dataTable.mapRad { rad ->
            Vedtaksperiode(
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
        stønadsperioder = mapStønadsperioder(behandlingId, dataTable)
    }

    @Når("beregner stønad for læremidler")
    fun `beregner stønad for læremidler`() {
        try {
            resultat = læremidlerBeregningService.beregn(vedtaksPerioder, behandlingId)
        } catch (e: Exception) {
            beregningException = e
        }
    }

    @Når("splitter vedtaksperioder for læremidler")
    fun `splitter vedtaksperioder for læremidler`() {
        vedtaksperioderSplittet = vedtaksPerioder.flatMap { it.delTilUtbetalingsPerioder() }
    }

    @Når("validerer vedtaksperiode for læremidler")
    fun `validerer vedtaksperiode for læremidler`() {
        try {
            validerVedtaksperioder(vedtaksPerioder, stønadsperioder.tilSortertStønadsperiodeBeregningsgrunnlag())
        } catch (feil: Exception) {
            valideringException = feil
        }
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
                    utbetalingsmåned = parseÅrMåned(BeregningNøkler.UTBETALINGSMÅNED, rad),
                    målgruppe = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad)
                        ?: MålgruppeType.AAP,
                ),
            )
        }
        val forventetBeregningsresultat = BeregningsresultatLæremidler(
            perioder = perioder,
        )

        forventetBeregningsresultat.perioder.forEachIndexed { index, periode ->
            try {
                assertThat(periode).isEqualTo(resultat!!.perioder[index])
            } catch (e: Throwable) {
                val acutal = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $acutal")
                throw e
            }
        }
        assertThat(forventetBeregningsresultat.perioder).hasSize(resultat!!.perioder.size)
    }

    @Så("forvent følgende feil fra læremidlerberegning: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(beregningException!!).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende feil fra vedtaksperiode validering: {}")
    fun `skal resultat fra validering være`(forventetFeil: String) {
        assertThat(valideringException!!).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende utbetalingsperioder")
    fun `forvent følgende utbetalingsperioder`(dataTable: DataTable) {
        val forventedePerioder = dataTable.mapRad { rad ->
            UtbetalingPeriode(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                utbetalingsmåned = parseÅrMåned(BeregningNøkler.UTBETALINGSMÅNED, rad),
            )
        }
        assertThat(vedtaksperioderSplittet).containsExactlyElementsOf(forventedePerioder)
    }
}
