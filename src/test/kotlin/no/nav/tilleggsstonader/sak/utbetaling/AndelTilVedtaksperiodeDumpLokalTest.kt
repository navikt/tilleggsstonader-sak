package no.nav.tilleggsstonader.sak.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

/**
 * Lokal enhetstest mot dump-tabeller - skal IKKE kjøres i CI.
 *
 * Slik bruker du:
 * 1. Fyll inn URL, brukernavn og passord.
 * 2. Velg stønadstype som skal trigges.
 * 3. Sett evt. ønsketAndelId hvis du vil teste en konkret andel.
 * 4. Fjern @Disabled og kjør testen.
 */
// @Disabled("Kun for lokal manuell testing mot tabeller i schema dump")
class AndelTilVedtaksperiodeDumpLokalTest {
    private val databaseConfig =
        DatabaseConfig(
            url = "jdbc:postgresql://localhost:5432/tilleggsstonader-sak",
            username = "postgres",
            password = "test",
        )

    private val stønadstypeSomSkalTrigges = Stønadstype.DAGLIG_REISE_TSO
    private val andeltyperForStønadstype: List<String> =
        finnTypeAndelerForStønadstype(stønadstypeSomSkalTrigges).map { it.name }
    private val ønsketAndelId: UUID? = null

    private val jdbcTemplate = NamedParameterJdbcTemplate(opprettDataSource(databaseConfig))
    private val tilkjentYtelseRepository = TilkjentYtelseDumpRepository(jdbcTemplate)
    private val vedtakRepository = VedtakDumpRepository(jdbcTemplate)

    @Test
    fun `skal kunne hente entiteter som trengs for å koble andel til vedtaksperiodeId`() {
        val tilkjenteYtelser =
            tilkjentYtelseRepository.finnTilkjenteYtelserMedAndeler(
                andelTypeFilter = andeltyperForStønadstype,
                antall = 50,
            )

        assertThat(tilkjenteYtelser).isNotEmpty

        val andelMedBehandling =
            finnAndel(tilkjenteYtelser, ønsketAndelId)
        val vedtak =
            vedtakRepository.finnVedtakForBehandlinger(listOf(andelMedBehandling.behandlingId))[andelMedBehandling.behandlingId]

        assertThat(andelMedBehandling.andelTilkjentYtelse).isNotNull
        assertThat(andelMedBehandling.tilkjentYtelse).isNotNull
        assertThat(vedtak).isNotNull
        assertThat(vedtak?.vedtaksperioderHvisFinnes()).isNotEmpty

        println("AndelId=${andelMedBehandling.andelTilkjentYtelse.id}")
        println("BehandlingId=${andelMedBehandling.behandlingId}")
        println("VedtaksperiodeIder=${vedtak?.vedtaksperioderHvisFinnes()?.map { it.id }}")
    }

    @Test
    fun `skal kunne hente alle typeandeler for en gitt stønadstype`() {
        val typeAndeler = finnTypeAndelerForStønadstype(stønadstypeSomSkalTrigges)
        assertThat(typeAndeler).isNotEmpty
        println("Stønadstype=$stønadstypeSomSkalTrigges, typeAndeler=${typeAndeler.map { it.name }}")
    }

    @Test
    fun `TODO - koble andel til vedtaksperiodeIder`() {
        val kobler = defaultKoblingSkall().getValue(stønadstypeSomSkalTrigges)
        val tilkjenteYtelser =
            tilkjentYtelseRepository.finnTilkjenteYtelserMedAndeler(
                andelTypeFilter = andeltyperForStønadstype,
                antall = 1000,
            )

        tilkjenteYtelser.forEach { tilkjentYtelse ->
            val vedtak =
                vedtakRepository
                    .finnVedtakForBehandlinger(listOf(tilkjentYtelse.behandlingId))
                    .values
                    .single()

            tilkjentYtelse.andelerTilkjentYtelse.forEach { andelTilkjentYtelse ->
                val vedtaksperioder = kobler.finnVedtaksperioder(andelTilkjentYtelse, vedtak)

                println("BehandlingId=${tilkjentYtelse.behandlingId}")
                println("Andel=$andelTilkjentYtelse")
                vedtaksperioder.forEach { vedtaksperiode ->
                    println("Vedtaksperiode=$vedtaksperiode")
                }

                println()
            }
        }
    }

    @Test
    fun `skal kunne hente generiske vedtak fra dump-schema`() {
        val vedtak = vedtakRepository.finnVedtak(antall = 200)

        assertThat(vedtak).isNotEmpty
        println("Fant vedtakstyper=${vedtak.mapNotNull { it.data.type::class.simpleName }.distinct().sorted()}")
    }

    private fun finnAndel(
        tilkjenteYtelser: List<TilkjentYtelse>,
        andelId: UUID?,
    ): AndelMedBehandlingId {
        val andeler =
            tilkjenteYtelser.flatMap { ty ->
                ty.andelerTilkjentYtelse.map { andel -> AndelMedBehandlingId(ty, ty.behandlingId, andel) }
            }

        return andelId?.let { id ->
            andeler.firstOrNull { it.andelTilkjentYtelse.id == id }
                ?: error("Fant ikke andel med id=$id for stønadstype=$stønadstypeSomSkalTrigges")
        } ?: andeler.first()
    }
}

private data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
)

private data class AndelMedBehandlingId(
    val tilkjentYtelse: TilkjentYtelse,
    val behandlingId: BehandlingId,
    val andelTilkjentYtelse: AndelTilkjentYtelse,
)

private class TilkjentYtelseDumpRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun finnTilkjenteYtelserMedAndeler(
        andelTypeFilter: List<String>,
        antall: Int,
    ): List<TilkjentYtelse> {
        require(andelTypeFilter.isNotEmpty()) {
            "Du må fylle inn andeltyper for stønadstype i andeltyperForStønadstype før testen kjøres"
        }

        val params =
            MapSqlParameterSource()
                .addValue("typeAndeler", andelTypeFilter)
                .addValue("antall", antall)

        val andeler =
            jdbcTemplate.query(
                """
                SELECT aty.*, ty.behandling_id
                FROM dump.andel_tilkjent_ytelse aty
                JOIN dump.tilkjent_ytelse ty ON ty.id = aty.tilkjent_ytelse_id
                WHERE aty.type IN (:typeAndeler)
                ORDER BY aty.tilkjent_ytelse_id, aty.fom
                LIMIT :antall
                """.trimIndent(),
                params,
            ) { rs, _ ->
                AndelFraDump(
                    behandlingId = BehandlingId(rs.getObject("behandling_id", UUID::class.java)),
                    tilkjentYtelseId = rs.getObject("tilkjent_ytelse_id", UUID::class.java),
                    andelTilkjentYtelse =
                        AndelTilkjentYtelse(
                            id = rs.getObject("id", UUID::class.java),
                            beløp = rs.getInt("belop"),
                            fom = rs.getObject("fom", LocalDate::class.java),
                            tom = rs.getObject("tom", LocalDate::class.java),
                            satstype = Satstype.valueOf(rs.getString("satstype")),
                            type = TypeAndel.valueOf(rs.getString("type")),
                            version = rs.getInt("version"),
                            statusIverksetting = StatusIverksetting.valueOf(rs.getString("status_iverksetting")),
                            iverksetting =
                                rs.getObject("iverksetting_id", UUID::class.java)?.let { iverksettingId ->
                                    Iverksetting(
                                        iverksettingId = iverksettingId,
                                        iverksettingTidspunkt =
                                            rs.getObject(
                                                "iverksetting_tidspunkt",
                                                LocalDateTime::class.java,
                                            ),
                                    )
                                },
                            endretTid = rs.getObject("endret_tid", LocalDateTime::class.java),
                            utbetalingsdato = rs.getObject("utbetalingsdato", LocalDate::class.java),
                            brukersNavKontor = rs.getString("brukers_nav_kontor"),
                            reiseId = rs.getObject("reise_id", UUID::class.java)?.let(::ReiseId),
                        ),
                )
            }

        return andeler
            .groupBy { it.tilkjentYtelseId to it.behandlingId }
            .map { (nøkkel, andelerForTilkjentYtelse) ->
                TilkjentYtelse(
                    id = nøkkel.first,
                    behandlingId = nøkkel.second,
                    andelerTilkjentYtelse = andelerForTilkjentYtelse.map { it.andelTilkjentYtelse }.toSet(),
                )
            }
    }

    private data class AndelFraDump(
        val behandlingId: BehandlingId,
        val tilkjentYtelseId: UUID,
        val andelTilkjentYtelse: AndelTilkjentYtelse,
    )
}

private class VedtakDumpRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun finnVedtakForBehandlinger(behandlingIder: List<BehandlingId>): Map<BehandlingId, GeneriskVedtak<out Vedtaksdata>> {
        if (behandlingIder.isEmpty()) return emptyMap()

        val params =
            MapSqlParameterSource()
                .addValue("behandlingIder", behandlingIder.map { it.id })

        return jdbcTemplate
            .query(
                """
                SELECT behandling_id, type, data, git_versjon, tidligste_endring, opphorsdato
                FROM dump.vedtak
                WHERE behandling_id IN (:behandlingIder)
                """.trimIndent(),
                params,
            ) { rs, _ -> mapTilVedtak(rs) }
            .associateBy { it.behandlingId }
    }

    fun finnVedtak(antall: Int): List<GeneriskVedtak<out Vedtaksdata>> =
        jdbcTemplate.query(
            """
            SELECT behandling_id, type, data, git_versjon, tidligste_endring, opphorsdato
            FROM dump.vedtak
            ORDER BY behandling_id
            LIMIT :antall
            """.trimIndent(),
            MapSqlParameterSource().addValue("antall", antall),
        ) { rs, _ -> mapTilVedtak(rs) }

    private fun mapTilVedtak(rs: java.sql.ResultSet): GeneriskVedtak<out Vedtaksdata> {
        val dataJson = rs.getString("data")
        val vedtaksdata: Vedtaksdata = jsonMapper.readValue(dataJson)

        return GeneriskVedtak(
            behandlingId = BehandlingId(rs.getObject("behandling_id", UUID::class.java)),
            type = TypeVedtak.valueOf(rs.getString("type")),
            data = vedtaksdata,
            gitVersjon = rs.getString("git_versjon"),
            tidligsteEndring = rs.getObject("tidligste_endring", LocalDate::class.java),
            opphørsdato = rs.getObject("opphorsdato", LocalDate::class.java),
        )
    }
}

private fun opprettDataSource(config: DatabaseConfig): DataSource =
    DriverManagerDataSource().apply {
        setDriverClassName("org.postgresql.Driver")
        url = config.url
        username = config.username
        password = config.password
    }

private fun finnTypeAndelerForStønadstype(stønadstype: Stønadstype): List<TypeAndel> =
    TypeAndel.entries
        .filter { it != TypeAndel.UGYLDIG }
        .filter { it.tilStønadstype() == stønadstype }

private fun TypeAndel.tilStønadstype(): Stønadstype? =
    when (this) {
        TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
        TypeAndel.TILSYN_BARN_AAP,
        TypeAndel.TILSYN_BARN_ETTERLATTE,
        -> Stønadstype.BARNETILSYN

        TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
        TypeAndel.LÆREMIDLER_AAP,
        TypeAndel.LÆREMIDLER_ETTERLATTE,
        -> Stønadstype.LÆREMIDLER

        TypeAndel.BOUTGIFTER_AAP,
        TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER,
        TypeAndel.BOUTGIFTER_ETTERLATTE,
        -> Stønadstype.BOUTGIFTER

        TypeAndel.DAGLIG_REISE_AAP,
        TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
        TypeAndel.DAGLIG_REISE_ETTERLATTE,
        -> Stønadstype.DAGLIG_REISE_TSO

        TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
        TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
        TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
        TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING,
        TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
        TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
        TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
        TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
        TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO,
        TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
        TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
        TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
        TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
        TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB,
        TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING,
        TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
        TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
        -> Stønadstype.DAGLIG_REISE_TSR

        TypeAndel.REISE_TIL_SAMLING_AAP,
        TypeAndel.REISE_TIL_SAMLING_ENSLIG_FORSØRGER,
        TypeAndel.REISE_TIL_SAMLING_ETTERLATTE,
        -> Stønadstype.REISE_TIL_SAMLING_TSO

        TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSTRENING,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_AVKLARING,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_AMO,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_AMO,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_HØYERE_UTDANNING,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_JOBBKLUBB,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_OPPFØLGING,
        TypeAndel.REISE_TIL_SAMLING_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
        -> Stønadstype.REISE_TIL_SAMLING_TSR

        TypeAndel.REISE_OPPSTART_AAP,
        TypeAndel.REISE_OPPSTART_ENSLIG_FORSØRGER,
        TypeAndel.REISE_OPPSTART_ETTERLATTE,
        -> Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO

        TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSFORBEREDENDE,
        TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSTRENING,
        TypeAndel.REISE_OPPSTART_TILTAK_AVKLARING,
        TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_AMO,
        TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
        TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO,
        TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
        TypeAndel.REISE_OPPSTART_TILTAK_HØYERE_UTDANNING,
        TypeAndel.REISE_OPPSTART_TILTAK_JOBBKLUBB,
        TypeAndel.REISE_OPPSTART_TILTAK_OPPFØLGING,
        -> Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR

        TypeAndel.UGYLDIG -> null
    }

private fun defaultKoblingSkall(): Map<Stønadstype, AndelTilVedtaksperiodeIdKobler> =
    mapOf(
        Stønadstype.BARNETILSYN to BarnetilsynKoblerSkall,
        Stønadstype.LÆREMIDLER to LæremidlerKoblerSkall,
        Stønadstype.BOUTGIFTER to BoutgifterKoblerSkall,
        Stønadstype.DAGLIG_REISE_TSO to DagligReiseKoblerSkall,
        Stønadstype.DAGLIG_REISE_TSR to DagligReiseKoblerSkall,
        Stønadstype.REISE_TIL_SAMLING_TSO to ReiseTilSamlingKoblerSkall,
        Stønadstype.REISE_TIL_SAMLING_TSR to ReiseTilSamlingKoblerSkall,
        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO to ReiseOppstartKoblerSkall,
        Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR to ReiseOppstartKoblerSkall,
    )

private fun interface AndelTilVedtaksperiodeIdKobler {
    fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode>
}

private data object BarnetilsynKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørPassAvBarn
        TODO("Implementeres av ansvarlig for BARNETILSYN")
    }
}

private data object LæremidlerKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørLæremidler
        val beregningsperioder = vedtak.beregningsresultat.perioder.filter {
            it.grunnlag.utbetalingsdato == andel.fom
        }

        return vedtak.vedtaksperioder.filter { vedtaksperiode -> beregningsperioder.any { b -> b.overlapper(vedtaksperiode) } }
    }
}

private data object BoutgifterKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørBoutgifter

        val beregningsperiode =
            vedtak.beregningsresultat.perioder.single {
                it.fom.tilFørsteDagIMåneden().datoEllerNesteMandagHvisLørdagEllerSøndag() == andel.fom
            }

        val vedtaksperioder =
            vedtak.vedtaksperioder.filter {
                beregningsperiode.overlapper(it)
            }

        return vedtaksperioder
    }
}

private data object DagligReiseKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørDagligReise

        val andelTilhørerPrivatBil = andel.reiseId != null

        if (andelTilhørerPrivatBil) {
            val beregningsresultat = vedtak.beregningsresultat.privatBil!!

            val reiseperioder =
                beregningsresultat.reiser
                    .single {
                        it.reiseId == andel.reiseId
                    }.perioder

            val periode =
                reiseperioder.single {
                    it.fom.iDagHvisMandagEllerForrigeMandag() == andel.fom
                }

            return vedtak.vedtaksperioder.filter {
                periode.overlapper(it)
            }
        } else {
            val beregningsresultat =
                vedtak.beregningsresultat.offentligTransport
                    ?: throw RuntimeException(
                        "Mangler beregningsresultat for offentlig transport i vedtak for behandling ${vedtaksdata.behandlingId}",
                    )

            return emptyList()
        }
    }
}

private data object ReiseTilSamlingKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørReiseTilSamling
        TODO("Implementeres av ansvarlig for REISE_TIL_SAMLING")
    }
}

private data object ReiseOppstartKoblerSkall : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
        TODO("Implementeres av ansvarlig for REISE_OPPSTART_AVSLUTNING_HJEMREISE")
    }
}
