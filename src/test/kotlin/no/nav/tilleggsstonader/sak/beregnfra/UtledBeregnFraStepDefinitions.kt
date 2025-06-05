package no.nav.tilleggsstonader.sak.beregnfra

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

enum class BeregnFraFellesNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STATUS("Status"),
    TYPE("Type"),
    RESULTAT("Resultat"),
}

enum class VilkårperiodeNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STØNADSTYPE("Stønadstype"),
    AKTIVITETSDAGER("Aktivitetsdager"),
    TYPE("Type"),
    STUDIENIVÅ("Studienivå"),
    STUDIEPROSENT("Studieprosent"),
}

enum class VilkårNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    UTGIFT("Utgift"),
    ER_FREMTIDIG_UTGIFT("Er fremtidig utgift"),
}

@Suppress("unused", "ktlint:standard:function-naming")
class UtledBeregnFraStepDefinitions {
    var behandlingId = BehandlingId.random()
    var behandling = behandling(id = behandlingId)

    var vilkår = emptyList<Vilkår>()
    var vilkårForrigeBehandling = emptyList<Vilkår>()

    var aktiviteter = emptyList<GeneriskVilkårperiode<AktivitetFaktaOgVurdering>>()
    var aktiviteterForrigeBehandling = emptyList<GeneriskVilkårperiode<AktivitetFaktaOgVurdering>>()

    var vilkårsperioder: Vilkårperioder? = null
    var vilkårsperioderTidligereBehandling: Vilkårperioder? = null
    var vedtaksperioder = emptyList<Vedtaksperiode>()
    var vedtaksperioderTidligereBehandling = emptyList<Vedtaksperiode>()

    var exception: Exception? = null
    var tidligsteEndring: LocalDate? = null

    @Gitt("følgende vilkår i forrige behandling - beregnFra")
    fun `følgende vilkår i forrige behandling`(dataTable: DataTable) {
        vilkårForrigeBehandling = mapVilkår(dataTable)
    }

    @Gitt("følgende vilkår i revurdering - beregnFra")
    fun `følgende vilkår i revurdering`(dataTable: DataTable) {
        vilkår = mapVilkår(dataTable)
    }

    @Gitt("følgende aktiviteter i forrige behandling - beregnFra")
    fun `følgende aktiviteter i forrige behandling`(dataTable: DataTable) {
        aktiviteterForrigeBehandling = mapAktiviteter(dataTable)
    }

    @Gitt("følgende aktiviteter i revurdering - beregnFra")
    fun `følgende aktiviteter i revurdering`(dataTable: DataTable) {
        aktiviteter = mapAktiviteter(dataTable)
    }

    @Når("utleder beregnFraDato")
    fun `utleder beregnFraDato`() {
        tidligsteEndring =
            BeregnFraUtleder(
                vilkår = vilkår,
                vilkårTidligereBehandling = vilkårForrigeBehandling,
                vilkårsperioder = Vilkårperioder(aktiviteter = aktiviteter, målgrupper = emptyList()),
                vilkårsperioderTidligereBehandling = Vilkårperioder(
                    aktiviteter = aktiviteterForrigeBehandling,
                    målgrupper = emptyList()
                ),
                vedtaksperioder = vedtaksperioder,
                vedtaksperioderTidligereBehandling = vedtaksperioderTidligereBehandling,
            ).utledTidligsteEndring()
    }

    @Så("forvent følgende dato for tidligste endring: {}")
    fun `forvent følgende dato for tidligste endring`(forventetDatoStr: String) {
        val forventetDato = parseDato(forventetDatoStr)
        assertThat(tidligsteEndring).isEqualTo(forventetDato)
    }

    private fun mapVilkår(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            vilkår(
                behandlingId = BehandlingId.random(),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                resultat = parseEnum(BeregnFraFellesNøkler.RESULTAT, rad),
                type = parseValgfriEnum<VilkårType>(BeregnFraFellesNøkler.TYPE, rad) ?: VilkårType.PASS_BARN,
                status = parseValgfriEnum<VilkårStatus>(
                    BeregnFraFellesNøkler.STATUS,
                    rad,
                ) ?: VilkårStatus.NY,
                utgift = parseValgfriInt(VilkårNøkler.UTGIFT, rad) ?: 1000,
                erFremtidigUtgift = parseValgfriBoolean(VilkårNøkler.ER_FREMTIDIG_UTGIFT, rad) ?: false
            )
        }

    private fun mapAktiviteter(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            aktivitet(
                behandlingId = behandlingId,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering = mapFaktaOgVurderingAktivitet(rad),
                resultat = parseEnum(BeregnFraFellesNøkler.RESULTAT, rad),
                status = parseEnum(BeregnFraFellesNøkler.STATUS, rad),
            )
        }

    private fun mapFaktaOgVurderingAktivitet(
        rad: Map<String, String>,
    ): AktivitetFaktaOgVurdering {
        val stønadstype: Stønadstype = parseEnum(VilkårperiodeNøkler.STØNADSTYPE, rad)

        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> faktaOgVurderingAktivitetTilsynBarn(
                type = parseEnum(VilkårperiodeNøkler.TYPE, rad),
                aktivitetsdager = parseInt(
                    VilkårperiodeNøkler.AKTIVITETSDAGER, rad
                ),
            )

            Stønadstype.LÆREMIDLER -> faktaOgVurderingAktivitetLæremidler(
                type = parseEnum(VilkårperiodeNøkler.TYPE, rad),
                studienivå = parseEnum(VilkårperiodeNøkler.STUDIENIVÅ, rad),
                prosent = parseInt(VilkårperiodeNøkler.STUDIEPROSENT, rad)
            )

            Stønadstype.BOUTGIFTER -> faktaOgVurderingAktivitetBoutgifter(
                type = parseEnum(VilkårperiodeNøkler.TYPE, rad)

            )

            else -> error("Ukjent stønadstype: $stønadstype")
        }
    }
}

//    @Gitt("følgende aktiviteter")
//    fun `følgende aktiviteter`(dataTable: DataTable) {
//        vilkårperiodeRepository.insertAll(mapAktiviteter(behandlingId, dataTable))
//    }
//
//    @Gitt("følgende målgrupper")
//    fun `følgende målgrupper`(dataTable: DataTable) {
//        vilkårperiodeRepository.insertAll(mapMålgrupper(behandlingId, dataTable))
//    }
//
//
//    @Når("beregner med revurderFra={}")
//    fun `beregner med revurder fra`(revurderFraStr: String) {
//        val revurderFra = parseDato(revurderFraStr)
//        beregn(
//            saksbehandling(
//                id = behandlingId,
//                type = BehandlingType.REVURDERING,
//                revurderFra = revurderFra,
//                forrigeIverksatteBehandlingId = BehandlingId.random(),
//            ),
//        )
//    }
//
//    private fun beregn(behandling: Saksbehandling) {
//        every { tilsynBarnUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifter
//        try {
//            beregningsresultat = service.beregn(vedtaksperioder, behandling, TypeVedtak.INNVILGELSE)
//        } catch (e: Exception) {
//            exception = e
//        }
//    }
//
//    @Så("forvent følgende feil: {}")
//    fun `forvent følgende feil`(forventetFeil: String) {
//        assertThat(exception!!).hasMessageContaining(forventetFeil)
//    }
//
//    @Så("forvent følgende dato for første endring")
//    fun `forvent følgende beregningsresultat`(dataTable: DataTable) {
//        if (exception != null) {
//            logger.error("Feilet", exception)
//        }
//
//        assertThat(exception).isNull()
//        val forventetBeregningsresultat =
//            dataTable.mapRad { rad ->
//                ForventetBeregningsresultat(
//                    dagsats = parseBigDecimal(BeregningNøkler.DAGSATS, rad),
//                    månedsbeløp = parseValgfriInt(BeregningNøkler.MÅNEDSBELØP, rad),
//                    grunnlag =
//                        ForventetBeregningsgrunnlag(
//                            måned = parseÅrMåned(BeregningNøkler.MÅNED, rad),
//                            makssats = parseValgfriInt(BeregningNøkler.MAKSSATS, rad),
//                            antallDagerTotal = parseValgfriInt(BeregningNøkler.ANTALL_DAGER, rad),
//                            utgifterTotal = parseValgfriInt(BeregningNøkler.UTGIFT, rad),
//                            antallBarn = parseValgfriInt(BeregningNøkler.ANTALL_BARN, rad),
//                        ),
//                )
//            }
//
//        val perioder = beregningsresultat!!.perioder
//
//        perioder.forEachIndexed { index, resultat ->
//            val forventetResultat = forventetBeregningsresultat[index]
//            try {
//                assertThat(resultat.grunnlag.måned)
//                    .`as` { "måned" }
//                    .isEqualTo(forventetResultat.grunnlag.måned)
//
//                assertThat(resultat.dagsats)
//                    .`as` { "dagsats" }
//                    .isEqualTo(forventetResultat.dagsats)
//
//                forventetResultat.månedsbeløp?.let {
//                    assertThat(resultat.månedsbeløp)
//                        .`as` { "totaltMånedsbeløp" }
//                        .isEqualTo(it)
//                }
//
//                forventetResultat.grunnlag.antallDagerTotal?.let {
//                    assertThat(resultat.grunnlag.vedtaksperiodeGrunnlag.sumOf { it.antallDager })
//                        .`as` { "antallDagerTotal" }
//                        .isEqualTo(it)
//                }
//
//                forventetResultat.grunnlag.utgifterTotal?.let {
//                    assertThat(resultat.grunnlag.utgifterTotal)
//                        .`as` { "utgifterTotal" }
//                        .isEqualTo(it)
//                }
//
//                forventetResultat.grunnlag.makssats?.let {
//                    assertThat(resultat.grunnlag.makssats)
//                        .`as` { "makssats" }
//                        .isEqualTo(it)
//                }
//            } catch (e: Throwable) {
//                val acutal = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultat)
//                logger.error("Feilet validering av rad ${index + 1} $acutal")
//                throw e
//            }
//        }
//
//        assertThat(perioder).hasSize(forventetBeregningsresultat.size)
//    }
//
//    @Så("forvent følgende vedtaksperioder for: {}")
//    fun `forvent følgende vedtaksperioder`(
//        månedStr: String,
//        dataTable: DataTable,
//    ) {
//        assertThat(exception).isNull()
//        val måned = parseÅrMåned(månedStr)
//        val forventeteVedtaksperioder = mapVedtaksperioder(dataTable)
//
//        val perioder =
//            beregningsresultat!!
//                .perioder
//                .find { it.grunnlag.måned == måned }
//                ?.grunnlag
//                ?.vedtaksperiodeGrunnlag
//                ?.map { it.vedtaksperiode }
//                ?: error("Finner ikke beregningsresultat for $måned")
//
//        perioder.forEachIndexed { index, resultat ->
//            val forventetResultat = forventeteVedtaksperioder[index]
//            try {
//                assertThat(resultat.fom).`as` { "fom" }.isEqualTo(forventetResultat.fom)
//                assertThat(resultat.tom).`as` { "tom" }.isEqualTo(forventetResultat.tom)
//            } catch (e: Throwable) {
//                logger.error("Feilet validering av rad ${index + 1}")
//                throw e
//            }
//        }
//
//        assertThat(perioder)
//            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
//            .containsExactlyElementsOf(forventeteVedtaksperioder.tilVedtaksperiodeBeregning().sorted())
//    }
//
//    @Så("forvent følgende vedtaksperiodeGrunnlag for: {}")
//    fun `forvent følgende vedtaksperiodeGrunnlag`(
//        månedStr: String,
//        dataTable: DataTable,
//    ) {
//        assertThat(exception).isNull()
//        val måned = parseÅrMåned(månedStr)
//        val forventeteVedtaksperioder = parseForventedeVedtaksperioder(dataTable)
//
//        val perioder =
//            beregningsresultat!!
//                .perioder
//                .find { it.grunnlag.måned == måned }
//                ?.grunnlag
//                ?.vedtaksperiodeGrunnlag
//                ?: error("Finner ikke beregningsresultat for $måned")
//
//        perioder.forEachIndexed { index, resultat ->
//            val forventetResultat = forventeteVedtaksperioder[index]
//            try {
//                assertThat(resultat.vedtaksperiode.fom).isEqualTo(forventetResultat.fom)
//                assertThat(resultat.vedtaksperiode.tom).isEqualTo(forventetResultat.tom)
//                assertThat(resultat.vedtaksperiode.målgruppe).isEqualTo(forventetResultat.målgruppe)
//                assertThat(resultat.vedtaksperiode.aktivitet).isEqualTo(forventetResultat.aktivitet)
//                assertThat(resultat.aktiviteter.size).isEqualTo(forventetResultat.antallAktiviteter)
//                assertThat(resultat.antallDager).isEqualTo(forventetResultat.antallDager)
//            } catch (e: Throwable) {
//                val actual =
//                    listOf(
//                        resultat.vedtaksperiode.fom,
//                        resultat.vedtaksperiode.tom,
//                        resultat.vedtaksperiode.målgruppe,
//                        resultat.vedtaksperiode.aktivitet,
//                        resultat.aktiviteter.size,
//                        resultat.antallDager,
//                    ).joinToString(" | ")
//                val expected =
//                    listOf(
//                        forventetResultat.fom,
//                        forventetResultat.tom,
//                        forventetResultat.målgruppe,
//                        forventetResultat.aktivitet,
//                        forventetResultat.antallAktiviteter,
//                        forventetResultat.antallDager,
//                    ).joinToString(" | ")
//                logger.error(
//                    "Feilet validering av rad ${index + 1}\n" +
//                            "expected = $expected\n" +
//                            "actual = $actual",
//                )
//                throw e
//            }
//        }
//    }
//
//    @Så("forvent følgende beløpsperioder for: {}")
//    fun `forvent følgende beløpsperioder`(
//        månedStr: String,
//        dataTable: DataTable,
//    ) {
//        assertThat(exception).isNull()
//        val måned = parseÅrMåned(månedStr)
//        val forventedeBeløpsperioder = parseForventedeBeløpsperioder(dataTable)
//
//        val beløpsperioder =
//            beregningsresultat!!
//                .perioder
//                .find { it.grunnlag.måned == måned }
//                ?.beløpsperioder
//                ?: error("Finner ikke beregningsresultat for $måned")
//
//        verifiserAtListerErLike(beløpsperioder, forventedeBeløpsperioder)
//    }
//
//    private fun parseForventedeVedtaksperioder(dataTable: DataTable): List<ForventedeVedtaksperioder> =
//        dataTable.mapRad { rad ->
//            ForventedeVedtaksperioder(
//                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
//                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
//                målgruppe = parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad)
//                    ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
//                aktivitet =
//                    parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
//                        ?: AktivitetType.TILTAK,
//                antallAktiviteter = parseInt(BeregningNøkler.ANTALL_AKTIVITETER, rad),
//                antallDager = parseInt(BeregningNøkler.ANTALL_DAGER, rad),
//            )
//        }
//
//    private fun parseForventedeBeløpsperioder(dataTable: DataTable): List<Beløpsperiode> =
//        dataTable.mapRad { rad ->
//            Beløpsperiode(
//                dato = parseÅrMånedEllerDato(BeregningNøkler.DATO, rad).datoEllerFørsteDagenIMåneden(),
//                målgruppe = parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad)
//                    ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
//                beløp = parseInt(BeregningNøkler.BELØP, rad),
//            )
//        }
// }
//
// data class ForventetBeregningsresultat(
//    val dagsats: BigDecimal,
//    val månedsbeløp: Int?,
//    val grunnlag: ForventetBeregningsgrunnlag,
// )
//
// data class ForventetBeregningsgrunnlag(
//    val måned: YearMonth,
//    val makssats: Int?,
//    val antallDagerTotal: Int?,
//    val utgifterTotal: Int?,
//    val antallBarn: Int?,
// )
//
// data class ForventedeVedtaksperioder(
//    val fom: LocalDate,
//    val tom: LocalDate,
//    val målgruppe: FaktiskMålgruppe,
//    val aktivitet: AktivitetType,
//    val antallAktiviteter: Int?,
//    val antallDager: Int?,
// )
