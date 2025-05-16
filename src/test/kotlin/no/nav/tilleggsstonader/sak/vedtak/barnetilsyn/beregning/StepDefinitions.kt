package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.barnIder
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.cucumber.verifiserAtListerErLike
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Suppress("unused", "ktlint:standard:function-naming")
class StepDefinitions {
    private val logger = LoggerFactory.getLogger(javaClass)
    val vilkårperiodeRepository = VilkårperiodeRepositoryFake()
    val tilsynBarnUtgiftService = mockk<TilsynBarnUtgiftService>()
    val repository = mockk<VedtakRepository>(relaxed = true)
    val vedtaksperiodeValidingerService = mockk<VedtaksperiodeValideringService>(relaxed = true)

    val service =
        TilsynBarnBeregningService(
            vilkårperiodeRepository = vilkårperiodeRepository,
            tilsynBarnUtgiftService = tilsynBarnUtgiftService,
            vedtakRepository = repository,
            vedtaksperiodeValidingerService = vedtaksperiodeValidingerService,
        )

    var exception: Exception? = null

    var vedtaksperioder = emptyList<Vedtaksperiode>()
    var utgifter = mutableMapOf<BarnId, List<UtgiftBeregning>>()
    var beregningsresultat: BeregningsresultatTilsynBarn? = null
    var behandlingId = BehandlingId.random()
    var behandling = behandling(id = behandlingId)

    init {
        every { repository.findByIdOrThrow(any()) } returns
            innvilgetVedtak(beregningsresultat = BeregningsresultatTilsynBarn(perioder = emptyList()))
    }

    @Gitt("følgende vedtaksperioder")
    fun `følgende støndsperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende aktiviteter")
    fun `følgende aktiviteter`(dataTable: DataTable) {
        vilkårperiodeRepository.insertAll(mapAktiviteter(behandlingId, dataTable))
    }

    @Gitt("følgende målgrupper")
    fun `følgende målgrupper`(dataTable: DataTable) {
        vilkårperiodeRepository.insertAll(mapMålgrupper(behandlingId, dataTable))
    }

    @Gitt("følgende utgifter for barn med id: {}")
    fun `følgende utgifter`(
        barnId: Int,
        dataTable: DataTable,
    ) {
        val barnUuid = barnIder[barnId]!!
        assertThat(utgifter).doesNotContainKey(barnUuid)
        utgifter[barnUuid] =
            dataTable.mapRad { rad ->
                UtgiftBeregning(
                    fom = parseÅrMåned(DomenenøkkelFelles.FOM, rad),
                    tom = parseÅrMåned(DomenenøkkelFelles.TOM, rad),
                    utgift = parseInt(BeregningNøkler.UTGIFT, rad),
                )
            }
    }

    @Gitt("beregningsperioder fra forrige behandling")
    fun `perioder fra forrige behandling`(dataTable: DataTable) {
        val perioder =
            dataTable.mapRad {
                val måned = parseÅrMåned(BeregningNøkler.MÅNED, it)
                beregningsresultatForMåned(måned)
            }
        every { repository.findByIdOrThrow(any()) } returns
            innvilgetVedtak(beregningsresultat = BeregningsresultatTilsynBarn(perioder = perioder))
    }

    @Når("beregner")
    fun beregner() {
        beregn(saksbehandling(id = behandlingId))
    }

    @Når("beregner med revurderFra={}")
    fun `beregner med revurder fra`(revurderFraStr: String) {
        val revurderFra = parseDato(revurderFraStr)
        beregn(
            saksbehandling(
                id = behandlingId,
                type = BehandlingType.REVURDERING,
                revurderFra = revurderFra,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            ),
        )
    }

    private fun beregn(behandling: Saksbehandling) {
        every { tilsynBarnUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifter
        try {
            beregningsresultat = service.beregn(vedtaksperioder, behandling, TypeVedtak.INNVILGELSE)
        } catch (e: Exception) {
            exception = e
        }
    }

    @Så("forvent følgende feil: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(exception!!).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende beregningsresultat")
    fun `forvent følgende beregningsresultat`(dataTable: DataTable) {
        if (exception != null) {
            logger.error("Feilet beregning", exception)
        }
        assertThat(exception).isNull()
        val forventetBeregningsresultat =
            dataTable.mapRad { rad ->
                ForventetBeregningsresultat(
                    dagsats = parseBigDecimal(BeregningNøkler.DAGSATS, rad),
                    månedsbeløp = parseValgfriInt(BeregningNøkler.MÅNEDSBELØP, rad),
                    grunnlag =
                        ForventetBeregningsgrunnlag(
                            måned = parseÅrMåned(BeregningNøkler.MÅNED, rad),
                            makssats = parseValgfriInt(BeregningNøkler.MAKSSATS, rad),
                            antallDagerTotal = parseValgfriInt(BeregningNøkler.ANTALL_DAGER, rad),
                            utgifterTotal = parseValgfriInt(BeregningNøkler.UTGIFT, rad),
                            antallBarn = parseValgfriInt(BeregningNøkler.ANTALL_BARN, rad),
                        ),
                )
            }

        val perioder = beregningsresultat!!.perioder

        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventetBeregningsresultat[index]
            try {
                assertThat(resultat.grunnlag.måned)
                    .`as` { "måned" }
                    .isEqualTo(forventetResultat.grunnlag.måned)

                assertThat(resultat.dagsats)
                    .`as` { "dagsats" }
                    .isEqualTo(forventetResultat.dagsats)

                forventetResultat.månedsbeløp?.let {
                    assertThat(resultat.månedsbeløp)
                        .`as` { "totaltMånedsbeløp" }
                        .isEqualTo(it)
                }

                forventetResultat.grunnlag.antallDagerTotal?.let {
                    assertThat(resultat.grunnlag.vedtaksperiodeGrunnlag.sumOf { it.antallDager })
                        .`as` { "antallDagerTotal" }
                        .isEqualTo(it)
                }

                forventetResultat.grunnlag.utgifterTotal?.let {
                    assertThat(resultat.grunnlag.utgifterTotal)
                        .`as` { "utgifterTotal" }
                        .isEqualTo(it)
                }

                forventetResultat.grunnlag.makssats?.let {
                    assertThat(resultat.grunnlag.makssats)
                        .`as` { "makssats" }
                        .isEqualTo(it)
                }
            } catch (e: Throwable) {
                val acutal = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultat)
                logger.error("Feilet validering av rad ${index + 1} $acutal")
                throw e
            }
        }

        assertThat(perioder).hasSize(forventetBeregningsresultat.size)
    }

    @Så("forvent følgende vedtaksperioder for: {}")
    fun `forvent følgende vedtaksperioder`(
        månedStr: String,
        dataTable: DataTable,
    ) {
        assertThat(exception).isNull()
        val måned = parseÅrMåned(månedStr)
        val forventeteVedtaksperioder = mapVedtaksperioder(dataTable)

        val perioder =
            beregningsresultat!!
                .perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?.vedtaksperiodeGrunnlag
                ?.map { it.vedtaksperiode }
                ?: error("Finner ikke beregningsresultat for $måned")

        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventeteVedtaksperioder[index]
            try {
                assertThat(resultat.fom).`as` { "fom" }.isEqualTo(forventetResultat.fom)
                assertThat(resultat.tom).`as` { "tom" }.isEqualTo(forventetResultat.tom)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }

        assertThat(perioder)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyElementsOf(forventeteVedtaksperioder.tilVedtaksperiodeBeregning().sorted())
    }

    @Så("forvent følgende vedtaksperiodeGrunnlag for: {}")
    fun `forvent følgende vedtaksperiodeGrunnlag`(
        månedStr: String,
        dataTable: DataTable,
    ) {
        assertThat(exception).isNull()
        val måned = parseÅrMåned(månedStr)
        val forventeteVedtaksperioder = parseForventedeVedtaksperioder(dataTable)

        val perioder =
            beregningsresultat!!
                .perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?.vedtaksperiodeGrunnlag
                ?: error("Finner ikke beregningsresultat for $måned")

        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventeteVedtaksperioder[index]
            try {
                assertThat(resultat.vedtaksperiode.fom).isEqualTo(forventetResultat.fom)
                assertThat(resultat.vedtaksperiode.tom).isEqualTo(forventetResultat.tom)
                assertThat(resultat.vedtaksperiode.målgruppe).isEqualTo(forventetResultat.målgruppe)
                assertThat(resultat.vedtaksperiode.aktivitet).isEqualTo(forventetResultat.aktivitet)
                assertThat(resultat.aktiviteter.size).isEqualTo(forventetResultat.antallAktiviteter)
                assertThat(resultat.antallDager).isEqualTo(forventetResultat.antallDager)
            } catch (e: Throwable) {
                val actual =
                    listOf(
                        resultat.vedtaksperiode.fom,
                        resultat.vedtaksperiode.tom,
                        resultat.vedtaksperiode.målgruppe,
                        resultat.vedtaksperiode.aktivitet,
                        resultat.aktiviteter.size,
                        resultat.antallDager,
                    ).joinToString(" | ")
                val expected =
                    listOf(
                        forventetResultat.fom,
                        forventetResultat.tom,
                        forventetResultat.målgruppe,
                        forventetResultat.aktivitet,
                        forventetResultat.antallAktiviteter,
                        forventetResultat.antallDager,
                    ).joinToString(" | ")
                logger.error(
                    "Feilet validering av rad ${index + 1}\n" +
                        "expected = $expected\n" +
                        "actual = $actual",
                )
                throw e
            }
        }
    }

    @Så("forvent følgende beløpsperioder for: {}")
    fun `forvent følgende beløpsperioder`(
        månedStr: String,
        dataTable: DataTable,
    ) {
        assertThat(exception).isNull()
        val måned = parseÅrMåned(månedStr)
        val forventedeBeløpsperioder = parseForventedeBeløpsperioder(dataTable)

        val beløpsperioder =
            beregningsresultat!!
                .perioder
                .find { it.grunnlag.måned == måned }
                ?.beløpsperioder
                ?: error("Finner ikke beregningsresultat for $måned")

        verifiserAtListerErLike(beløpsperioder, forventedeBeløpsperioder)
    }

    private fun parseForventedeVedtaksperioder(dataTable: DataTable): List<ForventedeVedtaksperioder> =
        dataTable.mapRad { rad ->
            ForventedeVedtaksperioder(
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                målgruppe = parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad) ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet =
                    parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                        ?: AktivitetType.TILTAK,
                antallAktiviteter = parseInt(BeregningNøkler.ANTALL_AKTIVITETER, rad),
                antallDager = parseInt(BeregningNøkler.ANTALL_DAGER, rad),
            )
        }

    private fun parseForventedeBeløpsperioder(dataTable: DataTable): List<Beløpsperiode> =
        dataTable.mapRad { rad ->
            Beløpsperiode(
                dato = parseÅrMånedEllerDato(BeregningNøkler.DATO, rad).datoEllerFørsteDagenIMåneden(),
                målgruppe = parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad) ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                beløp = parseInt(BeregningNøkler.BELØP, rad),
            )
        }
}

data class ForventetBeregningsresultat(
    val dagsats: BigDecimal,
    val månedsbeløp: Int?,
    val grunnlag: ForventetBeregningsgrunnlag,
)

data class ForventetBeregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int?,
    val antallDagerTotal: Int?,
    val utgifterTotal: Int?,
    val antallBarn: Int?,
)

data class ForventedeVedtaksperioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val antallAktiviteter: Int?,
    val antallDager: Int?,
)
