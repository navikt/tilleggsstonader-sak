package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BoutgifterCucumberNøkler
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

@Suppress("ktlint:standard:function-naming")
class StepDefinitions {
    val logger = LoggerFactory.getLogger(javaClass)

    val utgiftService = mockk<BoutgifterUtgiftService>()

    val vedtakRepositroy = mockk<VedtakRepository>(relaxed = true)
    val vilkårperiodeService = mockk<VilkårperiodeService>(relaxed = true)
    val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>(relaxed = true)
    val unleashService = mockk<UnleashService>()

    val beregningsService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = utgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepositroy,
            unleashService = unleashService,
        )

    var vilkårperioder: Vilkårperioder? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var utgifter = mutableMapOf<TypeBoutgift, List<UtgiftBeregningBoutgifter>>()
    var beregningsresultat: BeregningsresultatBoutgifter? = null
    var exception: Exception? = null

    init {
        every { vedtakRepositroy.findByIdOrThrow(any()) } returns
            GeneriskVedtak(
                behandlingId = BehandlingId.random(),
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseBoutgifter(
                        beregningsresultat = BeregningsresultatBoutgifter(emptyList()),
                        vedtaksperioder = emptyList(),
                    ),
                gitVersjon = "versjon-test",
            )
        every { unleashService.isEnabled(Toggle.SKAL_VISE_DETALJERT_BEREGNINGSRESULTAT) } returns true
    }

    @Gitt("følgende vedtaksperioder for boutgifter")
    fun `følgende vedtaksperioder for boutgifter`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende utgifter for: {}")
    fun `følgende utgifter for`(
        typeBoutgift: TypeBoutgift,
        dataTable: DataTable,
    ) {
        utgifter[typeBoutgift] = mapUtgifter(dataTable)
    }

    private fun beregn(saksbehandling: Saksbehandling) {
        every { utgiftService.hentUtgifterTilBeregning(any()) } returns utgifter
        try {
            beregningsresultat =
                beregningsService.beregn(
                    behandling = saksbehandling,
                    vedtaksperioder = vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                )
        } catch (e: Exception) {
            exception = e
        }
    }

    @Når("beregner boutgifter")
    fun `beregner boutgifter`() {
        beregn(saksbehandling())
    }

    @Når("beregner boutgifter med revurderFra={}")
    fun `beregner boutgifter med revurder fra`(revurderFraStr: String) {
        val revurderFra = parseDato(revurderFraStr)
        beregn(
            saksbehandling(
                type = BehandlingType.REVURDERING,
                revurderFra = revurderFra,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            ),
        )
    }

    @Så("skal beregnet stønad for boutgifter være")
    fun `skal beregnet stønad for boutgifter være`(dataTable: DataTable) {
        assertThat(exception).isNull()
        val forventetBeregningsresultat = mapBeregningsresultat(dataTable, utgifter)
        val forventedeStønadsbeløp = dataTable.mapRad { rad -> parseInt(BoutgifterCucumberNøkler.STØNADSBELØP, rad) }

        assertThat(beregningsresultat!!.perioder).hasSize(forventetBeregningsresultat.size)

        forventetBeregningsresultat.forEachIndexed { index, periode ->
            try {
                assertThat(beregningsresultat!!.perioder[index]).isEqualTo(periode)
                assertThat(beregningsresultat!!.perioder[index].stønadsbeløp).isEqualTo(forventedeStønadsbeløp[index])
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
    }

    @Så("forvent følgende feil for boutgifter: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(exception).hasMessageContaining(forventetFeil)
    }
}
