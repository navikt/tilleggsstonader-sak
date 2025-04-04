package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnUtil.splittTilLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
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
    UTBETALINGSDATO("Utbetalingsdato"),
    MÅLGRUPPE("Målgruppe"),
    BEKREFTET_SATS("Bekreftet sats"),
    DEL_AV_TIDLIGERE_UTBETALING("Del av tidligere utbetaling"),
}

@Suppress("unused", "ktlint:standard:function-naming")
class StepDefinitions {
    val logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepository = VilkårperiodeRepositoryFake()
    val behandlingService = mockk<BehandlingService>()
    val vedtakRepository = VedtakRepositoryFake()

    val vilkårperiodeService =
        mockk<VilkårperiodeService>().apply {
            val mock = this
            every { mock.hentVilkårperioder(any()) } answers {
                val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(BehandlingId(firstArg<UUID>())).sorted()

                Vilkårperioder(
                    målgrupper = vilkårsperioder.ofType<MålgruppeFaktaOgVurdering>(),
                    aktiviteter = vilkårsperioder.ofType<AktivitetFaktaOgVurdering>(),
                )
            }
        }
    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vedtakRepository = vedtakRepository,
            vilkårperiodeService = vilkårperiodeService,
        )

    val læremidlerBeregningService =
        LæremidlerBeregningService(
            vilkårperiodeRepository = vilkårperiodeRepository,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = VedtakRepositoryFake(),
        )

    val behandlingId = BehandlingId(UUID.randomUUID())

    var vedtaksPerioder: List<Vedtaksperiode> = emptyList()
    var resultat: BeregningsresultatLæremidler? = null

    var beregningException: Exception? = null

    var vedtaksperioderSplittet: List<LøpendeMåned> = emptyList()

    @Gitt("følgende vedtaksperioder for læremidler")
    fun `følgende beregningsperiode for læremidler`(dataTable: DataTable) {
        vedtaksPerioder =
            dataTable.mapRad { rad ->
                vedtaksperiode(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    målgruppe = parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad) ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad) ?: AktivitetType.TILTAK,
                )
            }
    }

    @Gitt("følgende aktiviteter for læremidler")
    fun `følgende aktiviteter`(dataTable: DataTable) {
        vilkårperiodeRepository.insertAll(mapAktiviteter(behandlingId, dataTable))
    }

    @Gitt("følgende målgrupper for læremidler")
    fun `følgende målgrupper`(dataTable: DataTable) {
        vilkårperiodeRepository.insertAll(mapMålgrupper(behandlingId, dataTable))
    }

    @Når("beregner stønad for læremidler")
    fun `beregner stønad for læremidler`() {
        val behandling = saksbehandling(behandling = behandling(id = behandlingId))
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns behandling
        try {
            resultat =
                læremidlerBeregningService.beregn(
                    behandling,
                    vedtaksPerioder,
                )
        } catch (e: Exception) {
            beregningException = e
        }
    }

    @Når("splitter vedtaksperioder for læremidler")
    fun `splitter vedtaksperioder for læremidler`() {
        vedtaksperioderSplittet =
            vedtaksPerioder
                .map {
                    vedtaksperiodeBeregning(
                        fom = it.fom,
                        tom = it.tom,
                    )
                }.splittTilLøpendeMåneder()
    }

    @Så("skal stønaden være")
    fun `skal stønaden være`(dataTable: DataTable) {
        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable)

        assertThat(beregningException).isNull()

        forventedeBeregningsperioder.forEachIndexed { index, periode ->
            try {
                assertThat(resultat!!.perioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(resultat?.perioder).hasSize(forventedeBeregningsperioder.size)
    }

    @Så("forvent følgende feil fra læremidlerberegning: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(beregningException).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende utbetalingsperioder")
    fun `forvent følgende utbetalingsperioder`(dataTable: DataTable) {
        val forventedePerioder =
            dataTable.mapRad { rad ->
                LøpendeMåned(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    utbetalingsdato = parseDato(BeregningNøkler.UTBETALINGSDATO, rad),
                )
            }
        assertThat(vedtaksperioderSplittet).containsExactlyElementsOf(forventedePerioder)
    }
}
