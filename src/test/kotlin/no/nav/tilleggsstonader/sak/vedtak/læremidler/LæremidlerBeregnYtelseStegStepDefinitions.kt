package no.nav.tilleggsstonader.sak.vedtak.læremidler

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdFraUUID
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.RepositoryMockUtil.mockStønadsperiodeRepository
import no.nav.tilleggsstonader.sak.util.RepositoryMockUtil.mockTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.RepositoryMockUtil.mockVedtakRepository
import no.nav.tilleggsstonader.sak.util.RepositoryMockUtil.mockVilkårperiodeRepository
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.mapStønadsperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.LocalDate

class LæremidlerBeregnYtelseStegStepDefinitions {

    val logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepository = mockVilkårperiodeRepository()
    val stønadsperiodeRepository = mockStønadsperiodeRepository()
    val vedtakRepository = mockVedtakRepository()
    val tilkjentYtelseRepository = mockTilkjentYtelseRepository()

    val simuleringService = mockk<SimuleringService>().apply {
        justRun { slettSimuleringForBehandling(any()) }
    }
    val steg = LæremidlerBeregnYtelseSteg(
        beregningService = LæremidlerBeregningService(
            vilkårperiodeRepository = vilkårperiodeRepository,
            stønadsperiodeRepository = stønadsperiodeRepository,
        ),
        opphørValideringService = mockk(),
        vedtakRepository = vedtakRepository,
        tilkjentytelseService = TilkjentYtelseService(tilkjentYtelseRepository),
        simuleringService = simuleringService,
    )

    @Gitt("følgende aktiviteter for læremidler behandling={}")
    fun `følgende aktiviteter`(behandlingIdTall: Int, dataTable: DataTable) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        every {
            vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, any())
        } returns mapAktiviteter(behandlingId, dataTable)
    }

    @Gitt("følgende stønadsperioder for læremidler behandling={}")
    fun `følgende stønadsperioder`(behandlingIdTall: Int, dataTable: DataTable) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        every {
            stønadsperiodeRepository.findAllByBehandlingId(any())
        } returns mapStønadsperioder(behandlingId, dataTable)
    }

    @Når("innvilger vedtaksperioder for behandling={}")
    fun `følgende vedtaksperioder`(behandlingIdTall: Int, dataTable: DataTable) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val vedtaksperioder = dataTable.mapRad { rad ->
            VedtaksperiodeDto(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
            )
        }
        steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseLæremidlerRequest(vedtaksperioder))
    }

    @Så("forvent beregningsresultatet for behandling={}")
    fun `forvent beregningsresultatet`(behandlingIdTall: Int, dataTable: DataTable) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)
        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable)

        val beregningsresultat = vedtakRepository.findByIdOrThrow(behandlingId)
            .withTypeOrThrow<VedtakLæremidler>()
            .data
            .beregningsresultat()!!

        forventedeBeregningsperioder.forEachIndexed { index, periode ->
            try {
                assertThat(beregningsresultat.perioder[index]).isEqualTo(periode)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(periode)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(beregningsresultat.perioder).hasSize(forventedeBeregningsperioder.size)
    }

    @Så("forvent andeler for behandling={}")
    fun `forvent andeler`(behandlingIdTall: Int, dataTable: DataTable) {
        val behandlingId = behandlingIdTilUUID.getValue(behandlingIdTall)

        val forventedeAndeler = dataTable.mapRad { rad ->
            ForenkletAndel(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseValgfriDato(DomenenøkkelFelles.TOM, rad) ?: parseDato(DomenenøkkelFelles.FOM, rad),
                satstype = parseValgfriEnum<Satstype>(DomenenøkkelAndelTilkjentYtelse.SATS, rad) ?: Satstype.DAG,
                type = parseEnum(DomenenøkkelAndelTilkjentYtelse.TYPE, rad),
                utbetalingsdato = parseDato(DomenenøkkelAndelTilkjentYtelse.UTBETALINGSDATO, rad),
                statusIverksetting = parseValgfriEnum<StatusIverksetting>(
                    domenebegrep = DomenenøkkelAndelTilkjentYtelse.STATUS_IVERKSETTING,
                    rad = rad,
                ) ?: StatusIverksetting.UBEHANDLET,
            )
        }

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!
            .andelerTilkjentYtelse
            .sortedBy { it.fom }

        andeler.map { ForenkletAndel(it) }.forEachIndexed { index, andel ->
            try {
                assertThat(forventedeAndeler[index]).isEqualTo(andel)
            } catch (e: Throwable) {
                val actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(andel)
                logger.error("Feilet validering av rad ${index + 1} $actual")
                throw e
            }
        }
        assertThat(andeler).hasSize(forventedeAndeler.size)
    }

    private data class ForenkletAndel(
        val fom: LocalDate,
        val tom: LocalDate,
        val satstype: Satstype,
        val type: TypeAndel,
        val utbetalingsdato: LocalDate,
        val statusIverksetting: StatusIverksetting,
    ) {
        constructor(andel: AndelTilkjentYtelse) : this(
            fom = andel.fom,
            tom = andel.tom,
            satstype = andel.satstype,
            type = andel.type,
            utbetalingsdato = andel.utbetalingsdato,
            statusIverksetting = andel.statusIverksetting,
        )
    }

    private fun dummyBehandling(behandlingId: BehandlingId) =
        saksbehandling(
            id = behandlingId,
            fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER),
            forrigeBehandlingId = forrigeBehandlingId(behandlingId),
        )

    private fun forrigeBehandlingId(behandlingId: BehandlingId): BehandlingId? {
        val behandlingIdInt = behandlingIdFraUUID(behandlingId)
        return if (behandlingIdInt > 1) {
            behandlingIdTilUUID.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }
}
