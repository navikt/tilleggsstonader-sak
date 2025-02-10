package no.nav.tilleggsstonader.sak.vedtak.læremidler

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdFraUUID
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.StønadsperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.TilkjentYtelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.mapStønadsperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.domain.vedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeLæremidlerDto
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

@Suppress("unused", "ktlint:standard:function-naming")
class LæremidlerBeregnYtelseStegStepDefinitions {
    val logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepository = VilkårperiodeRepositoryFake()
    val stønadsperiodeRepository = StønadsperiodeRepositoryFake()
    val vedtakRepository = VedtakRepositoryFake()
    val tilkjentYtelseRepository = TilkjentYtelseRepositoryFake()

    val simuleringService =
        mockk<SimuleringService>().apply {
            justRun { slettSimuleringForBehandling(any()) }
        }
    val steg =
        LæremidlerBeregnYtelseSteg(
            beregningService =
                LæremidlerBeregningService(
                    vilkårperiodeRepository = vilkårperiodeRepository,
                    stønadsperiodeRepository = stønadsperiodeRepository,
                ),
            opphørValideringService = mockk<OpphørValideringService>(relaxed = true),
            vedtakRepository = vedtakRepository,
            tilkjentytelseService = TilkjentYtelseService(tilkjentYtelseRepository),
            simuleringService = simuleringService,
        )
    val vedtaksperiodeId: UUID = UUID.randomUUID()

    @Gitt("følgende aktiviteter for læremidler behandling={}")
    fun `følgende aktiviteter`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        vilkårperiodeRepository.insertAll(mapAktiviteter(behandlingId, dataTable))
    }

    @Gitt("følgende stønadsperioder for læremidler behandling={}")
    fun `følgende stønadsperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        stønadsperiodeRepository.insertAll(mapStønadsperioder(behandlingId, dataTable))
    }

    @Når("innvilger vedtaksperioder for behandling={}")
    fun `følgende vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val vedtaksperioder =
            dataTable.mapRad { rad ->
                VedtaksperiodeLæremidlerDto(
                    id = vedtaksperiodeId,
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                )
            }
        steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseLæremidlerRequest(vedtaksperioder))
    }

    @Når("innvilger revurdering med vedtaksperioder for behandling={} med revurderFra={}")
    fun `innvilger vedtaksperioder for behandling={} med revurderFra={}`(
        behandlingIdTall: Int,
        revurderFraStr: String,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)

        val vedtaksperioder =
            dataTable.mapRad { rad ->
                VedtaksperiodeLæremidlerDto(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    id = UUID.randomUUID(),
                )
            }
        steg.utførSteg(dummyBehandling(behandlingId, revurderFra), InnvilgelseLæremidlerRequest(vedtaksperioder))
    }

    @Når("kopierer perioder fra forrige behandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val forrigeBehandlingId = forrigeBehandlingId(behandlingId) ?: error("Forventer å finne forrigeBehandlingId")

        val tidligereStønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(forrigeBehandlingId)
        val tidligereVilkårsperioder = vilkårperiodeRepository.findByBehandlingId(forrigeBehandlingId)
        stønadsperiodeRepository.insertAll(tidligereStønadsperioder.map { it.kopierTilBehandling(behandlingId) })
        vilkårperiodeRepository.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
    }

    @Gitt("lagrer beregningsresultatet behandling={}")
    fun `lagrer beregningsresultat`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val perioderBeregningsresultat = mapBeregningsresultat(dataTable)
        val vedtaksperiode =
            Vedtaksperiode(
                fom = perioderBeregningsresultat.minOf { it.fom },
                tom = perioderBeregningsresultat.maxOf { it.tom },
                id = UUID.randomUUID(),
            )
        val vedtak =
            InnvilgelseLæremidler(
                vedtaksperioder = listOf(vedtaksperiode),
                beregningsresultat = BeregningsresultatLæremidler(perioderBeregningsresultat),
            )
        vedtakRepository.insert(GeneriskVedtak(behandlingId = behandlingId, vedtak))
    }

    @Gitt("lagrer andeler behandling={}")
    fun `lagrer andeler`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val andeler =
            mapAndeler(dataTable)
                .map {
                    AndelTilkjentYtelse(
                        beløp = it.beløp,
                        fom = it.fom,
                        tom = it.tom,
                        satstype = it.satstype,
                        type = it.type,
                        kildeBehandlingId = behandlingId,
                        utbetalingsdato = it.utbetalingsdato,
                    )
                }.toSet()
        tilkjentYtelseRepository.insert(TilkjentYtelse(behandlingId = behandlingId, andelerTilkjentYtelse = andeler))
    }

    @Når("opphør behandling={} med revurderFra={}")
    fun `opphør med revurderFra`(
        behandlingIdTall: Int,
        revurderFraStr: String,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)
        steg.utførSteg(
            dummyBehandling(behandlingId, revurderFra = revurderFra),
            OpphørLæremidlerRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "begrunnelse",
            ),
        )
    }

    @Så("forvent beregningsresultatet for behandling={}")
    fun `forvent beregningsresultatet`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable)

        val beregningsresultat = hentVedtak(behandlingId).beregningsresultat()!!

        forventedeBeregningsperioder.forEachIndexed { index, periode ->
            try {
                assertThat(beregningsresultat.perioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual =
                    objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(beregningsresultat.perioder[index])
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(beregningsresultat.perioder).hasSize(forventedeBeregningsperioder.size)
    }

    @Så("forvent andeler for behandling={}")
    fun `forvent andeler`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val forventedeAndeler = mapAndeler(dataTable)

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandlingId)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }

        andeler.map { ForenkletAndel(it) }.forEachIndexed { index, andel ->
            try {
                assertThat(andel).isEqualTo(forventedeAndeler[index])
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(andel)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(andeler).hasSize(forventedeAndeler.size)
    }

    @Så("forvent vedtaksperioder for behandling={}")
    fun `forvent vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val vedtaksperioder = hentVedtak(behandlingId).vedtaksperioder()!!

        val forventedeVedtaksperioder =
            dataTable.mapRad { rad ->
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                )
            }

        forventedeVedtaksperioder.forEachIndexed { index, periode ->
            try {
                assertThat(vedtaksperioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(vedtaksperioder).hasSize(forventedeVedtaksperioder.size)
    }

    private fun hentVedtak(behandlingId: BehandlingId): VedtakLæremidler =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<VedtakLæremidler>()
            .data

    private data class ForenkletAndel(
        val fom: LocalDate,
        val tom: LocalDate,
        val beløp: Int,
        val satstype: Satstype,
        val type: TypeAndel,
        val utbetalingsdato: LocalDate,
        val statusIverksetting: StatusIverksetting,
    ) {
        constructor(andel: AndelTilkjentYtelse) : this(
            fom = andel.fom,
            tom = andel.tom,
            beløp = andel.beløp,
            satstype = andel.satstype,
            type = andel.type,
            utbetalingsdato = andel.utbetalingsdato,
            statusIverksetting = andel.statusIverksetting,
        )
    }

    private fun mapAndeler(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            ForenkletAndel(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad) ?: parseDato(DomenenøkkelFelles.FOM, rad),
                beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
                satstype = parseValgfriEnum<Satstype>(DomenenøkkelAndelTilkjentYtelse.SATS, rad) ?: Satstype.DAG,
                type = parseEnum(DomenenøkkelAndelTilkjentYtelse.TYPE, rad),
                utbetalingsdato = parseDato(DomenenøkkelAndelTilkjentYtelse.UTBETALINGSDATO, rad),
                statusIverksetting =
                    parseValgfriEnum<StatusIverksetting>(
                        domenebegrep = DomenenøkkelAndelTilkjentYtelse.STATUS_IVERKSETTING,
                        rad = rad,
                    ) ?: StatusIverksetting.UBEHANDLET,
            )
        }

    private fun dummyBehandling(
        behandlingId: BehandlingId,
        revurderFra: LocalDate? = null,
    ): Saksbehandling {
        val forrigeBehandlingId = forrigeBehandlingId(behandlingId)
        return saksbehandling(
            id = behandlingId,
            fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER),
            forrigeBehandlingId = forrigeBehandlingId,
            revurderFra = revurderFra,
            type = if (forrigeBehandlingId != null) BehandlingType.REVURDERING else BehandlingType.FØRSTEGANGSBEHANDLING,
        )
    }

    private fun forrigeBehandlingId(behandlingId: BehandlingId): BehandlingId? {
        val behandlingIdInt = behandlingIdFraUUID(behandlingId)
        return if (behandlingIdInt > 1) {
            behandlingIdTilUUID.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }
}
