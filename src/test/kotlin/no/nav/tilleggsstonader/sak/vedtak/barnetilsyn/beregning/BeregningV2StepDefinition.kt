package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
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
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2.TilsynBarnBeregningServiceV2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

@Suppress("ktlint:standard:function-naming")
class BeregningV2StepDefinition {
    private val logger = LoggerFactory.getLogger(javaClass)
    val tilsynBarnUtgiftService = mockk<TilsynBarnUtgiftService>()
    val vilkårperiodeRepository = mockk<VilkårperiodeRepository>()
    val beregningService =
        TilsynBarnBeregningServiceV2(
            tilsynBarnUtgiftService = tilsynBarnUtgiftService,
            vilkårperiodeRepository = vilkårperiodeRepository,
        )

    var exception: Exception? = null

    var vedtaksperioder = emptyList<VedtaksperiodeDto>()
    var utgifter = mutableMapOf<BarnId, List<UtgiftBeregning>>()
    var beregningsresultat: BeregningsresultatTilsynBarn? = null
    var behandlingId = BehandlingId.random()

    @Gitt("V2 - følgende vedtaksperioder")
    fun `følgende vedtaksperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("V2 - følgende aktiviteter")
    fun `følgende aktiviteter`(dataTable: DataTable) {
        every {
            vilkårperiodeRepository.findByBehandlingIdAndResultat(
                behandlingId,
                ResultatVilkårperiode.OPPFYLT,
            )
        } returns mapAktiviteter(behandlingId, dataTable)
    }

    @Gitt("V2 - følgende utgifter for barn med id: {}")
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

    @Når("V2 - beregner")
    fun beregner() {
        beregn(saksbehandling(id = behandlingId))
    }

    private fun beregn(behandling: Saksbehandling) {
        every { tilsynBarnUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifter
        try {
            beregningsresultat =
                beregningService.beregn(
                    vedtaksperioder = vedtaksperioder,
                    behandlingId = behandlingId,
                )
        } catch (e: Exception) {
            exception = e
        }
    }

    @Så("V2 - forvent følgende beregningsresultat")
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
                    assertThat(resultat.grunnlag.vedtaksperioderGrunnlag.sumOf { it.antallAktivitetsDager })
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

    @Så("V2 - forvent følgende vedtaksperiodeGrunnlag for: {}")
    fun `forvent følgende vedtaksperiodeGrunnlag`(
        månedStr: String,
        dataTable: DataTable,
    ) {
        assertThat(exception).isNull()
        val måned = parseÅrMåned(månedStr)
        val forventetVedtaksperiodeGrunnlag = mapVedtaksperiodeGrunnlag(dataTable)

        val perioder =
            beregningsresultat!!
                .perioder
                .find { it.grunnlag.måned == måned }
                ?.grunnlag
                ?.vedtaksperioderGrunnlag
                ?: error("Finner ikke beregningsresultat for $måned")

        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventetVedtaksperiodeGrunnlag[index]
            try {
                assertThat(resultat.fom).`as` { "fom" }.isEqualTo(forventetResultat.fom)
                assertThat(resultat.tom).`as` { "tom" }.isEqualTo(forventetResultat.tom)
                assertThat(resultat.antallAktivitetsDager)
                    .`as` { "antallAktivitetsDager" }
                    .isEqualTo(forventetResultat.antallAktivitetsDager)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }

        assertThat(perioder).hasSize(forventetVedtaksperiodeGrunnlag.size)
    }

    @Så("V2 - forvent følgende beløpsperioder for: {}")
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

        beløpsperioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventedeBeløpsperioder[index]
            try {
                assertThat(resultat.dato).isEqualTo(forventetResultat.dato)
                assertThat(resultat.beløp).isEqualTo(forventetResultat.beløp)
                assertThat(resultat.målgruppe).isEqualTo(forventetResultat.målgruppe)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }
        assertThat(beløpsperioder).hasSize(forventedeBeløpsperioder.size)
    }

    @Så("V2 - forvent følgende feil: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(exception!!).hasMessageContaining(forventetFeil)
    }
}

fun parseForventedeBeløpsperioder(dataTable: DataTable): List<Beløpsperiode> =
    dataTable.mapRad { rad ->
        Beløpsperiode(
            dato = parseÅrMånedEllerDato(BeregningNøkler.DATO, rad).datoEllerFørsteDagenIMåneden(),
            målgruppe = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            beløp = parseInt(BeregningNøkler.BELØP, rad),
        )
    }

fun mapVedtaksperiodeGrunnlag(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        VedtaksperiodeGrunnlag(
            fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
            tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
            målgruppeType = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            aktivitetType =
                parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                    ?: AktivitetType.TILTAK,
            aktiviteter = emptyList(),
            antallAktivitetsDager = parseInt(BeregningNøkler.ANTALL_DAGER, rad),
        )
    }

fun mapVedtaksperioder(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        VedtaksperiodeDto(
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            målgruppeType = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            aktivitetType =
                parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                    ?: AktivitetType.TILTAK,
        )
    }
