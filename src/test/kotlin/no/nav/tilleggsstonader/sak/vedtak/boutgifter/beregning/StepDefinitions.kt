package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

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
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BeregningNøkler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.util.UUID

@Suppress("ktlint:standard:function-naming")
class StepDefinitions {
    val logger = LoggerFactory.getLogger(javaClass)

    val utgiftService = mockk<BoutgifterUtgiftService>()

    val vedtakRepositroy = mockk<VedtakRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>(relaxed = true)
    val vedtaksperiodeValideringService = mockk<BoutgifterVedtaksperiodeValideringService>(relaxed = true)

    val beregningsService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = utgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepositroy,
        )

    val behandling = saksbehandling()

    var vilkårperioder: Vilkårperioder? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var utgifter = mutableMapOf<TypeBoutgift, List<UtgiftBeregningBoutgifter>>()
    var beregningsresultat: BeregningsresultatBoutgifter? = null
    var exception: Exception? = null

    @Gitt("følgende vedtaksperioder for boutgifter")
    fun `følgende vedtaksperioder for boutgifter`(dataTable: DataTable) {
        vedtaksperioder =
            dataTable.mapRad { rad ->
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    målgruppe = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
                    aktivitet =
                        parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                            ?: AktivitetType.TILTAK,
                )
            }
    }

    @Gitt("følgende utgifter for: {}")
    fun `følgende utgifter for`(
        typeBoutgift: TypeBoutgift,
        dataTable: DataTable,
    ) {
        utgifter[typeBoutgift] =
            dataTable.mapRad { rad ->
                UtgiftBeregningBoutgifter(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    utgift = parseInt(BoutgifterNøkler.UTGIFT, rad),
                )
            }
    }

    @Når("beregner stønad for boutgifter")
    fun beregner() {
        beregn()
    }

    private fun beregn() {
        every { utgiftService.hentUtgifterTilBeregning(any()) } returns utgifter
        try {
            beregningsresultat =
                beregningsService.beregn(
                    behandling = behandling,
                    vedtaksperioder = vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                )
        } catch (e: Exception) {
            exception = e
        }
    }

    @Så("skal stønaden for boutgifter være")
    fun `skal stønaden for boutgifter være`(dataTable: DataTable) {
        assertThat(exception).isNull()
        val forventetBeregningsresultat =
            dataTable.mapRad { rad ->
                BeregningsresultatForLøpendeMåned(
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = parseDato(DomenenøkkelFelles.FOM, rad),
                            tom = parseDato(DomenenøkkelFelles.TOM, rad),
                            utbetalingsdato = parseDato(BoutgifterNøkler.UTBETALINGSDATO, rad),
                            utgifter = utgifter,
                            makssats = parseInt(BoutgifterNøkler.MAKS_SATS, rad),
                            makssatsBekreftet = true,
                            målgruppe =
                                parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad)
                                    ?: MålgruppeType.AAP,
                            aktivitet =
                                parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                                    ?: AktivitetType.TILTAK,
                        ),
                )
            }

        forventetBeregningsresultat.forEachIndexed { index, periode ->
            try {
                assertThat(beregningsresultat!!.perioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
    }
}

enum class BoutgifterNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    UTBETALINGSDATO("Utbetalingsdato"),
    UTGIFT("Utgift"),
    MAKS_SATS("Maks sats"),
}
