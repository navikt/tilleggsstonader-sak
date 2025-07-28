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
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.behandlingIdTilTestId
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.testIdTilBehandlingId
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriDato
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.TilkjentYtelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
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
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BeregningNøkler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.mapMålgrupper
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

@Suppress("unused", "ktlint:standard:function-naming")
class LæremidlerBeregnYtelseStegStepDefinitions {
    val logger = LoggerFactory.getLogger(javaClass)

    val vilkårperiodeRepository = VilkårperiodeRepositoryFake()
    val vedtakRepository = VedtakRepositoryFake()
    val tilkjentYtelseRepository = TilkjentYtelseRepositoryFake()
    val behandlingService = mockk<BehandlingService>()
    val utledTidligsteEndringService =
        mockk<UtledTidligsteEndringService> {
            every { utledTidligsteEndringForBeregning(any(), any()) } returns null
        }
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

    val simuleringService =
        mockk<SimuleringService>().apply {
            justRun { slettSimuleringForBehandling(any()) }
        }
    val steg =
        LæremidlerBeregnYtelseSteg(
            beregningService =
                LæremidlerBeregningService(
                    vilkårperiodeRepository = vilkårperiodeRepository,
                    vedtaksperiodeValideringService = vedtaksperiodeValideringService,
                    vedtakRepository = vedtakRepository,
                ),
            opphørValideringService = mockk<OpphørValideringService>(relaxed = true),
            vedtakRepository = vedtakRepository,
            tilkjentYtelseService = TilkjentYtelseService(tilkjentYtelseRepository),
            simuleringService = simuleringService,
            utledTidligsteEndringService = utledTidligsteEndringService,
            unleashService = mockUnleashService(),
        )
    val vedtaksperiodeId: UUID = UUID.randomUUID()

    @Gitt("følgende aktiviteter for læremidler behandling={}")
    fun `følgende aktiviteter`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepository.insertAll(mapAktiviteter(behandlingId, dataTable))
    }

    @Gitt("følgende målgrupper for læremidler behandling={}")
    fun `følgende målgrupper`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        vilkårperiodeRepository.insertAll(mapMålgrupper(behandlingId, dataTable))
    }

    @Når("innvilger vedtaksperioder for behandling={}")
    fun `følgende vedtaksperioder`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val vedtaksperioder = mapVedtaksperioderDto(dataTable)
        steg.utførSteg(dummyBehandling(behandlingId), InnvilgelseLæremidlerRequest(vedtaksperioder))
    }

    @Når("innvilger revurdering med vedtaksperioder for behandling={} med revurderFra={}")
    fun `innvilger vedtaksperioder for behandling={} med revurderFra={}`(
        behandlingIdTall: Int,
        revurderFraStr: String,
        dataTable: DataTable,
    ) {
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)

        every { utledTidligsteEndringService.utledTidligsteEndringForBeregning(behandlingId, any()) } returns revurderFra

        val vedtaksperioder = mapVedtaksperioderDto(dataTable)
        steg.utførSteg(dummyBehandling(behandlingId, revurderFra), InnvilgelseLæremidlerRequest(vedtaksperioder))
    }

    @Når("kopierer perioder fra forrige behandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forrigeIverksatteBehandlingId =
            forrigeIverksatteBehandlingId(behandlingId) ?: error("Forventer å finne forrigeIverksatteBehandlingId")

        val tidligereVilkårsperioder = vilkårperiodeRepository.findByBehandlingId(forrigeIverksatteBehandlingId)
        vilkårperiodeRepository.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
    }

    @Gitt("lagrer beregningsresultatet behandling={}")
    fun `lagrer beregningsresultat`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val perioderBeregningsresultat = mapBeregningsresultat(dataTable)
        val vedtaksperiode = mapVedtaksperioder(dataTable)
        val vedtak =
            innvilgelse(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperiode,
                beregningsresultat = BeregningsresultatLæremidler(perioderBeregningsresultat),
            )
        vedtakRepository.insert(vedtak)
    }

    @Gitt("lagrer andeler behandling={}")
    fun `lagrer andeler`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

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
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val revurderFra = parseDato(revurderFraStr)
        steg.utførSteg(
            dummyBehandling(behandlingId, revurderFra = revurderFra),
            OpphørLæremidlerRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "begrunnelse",
                opphørsdato = revurderFra,
            ),
        )
    }

    @Så("forvent beregningsresultatet for behandling={}")
    fun `forvent beregningsresultatet`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forventedeBeregningsperioder = mapBeregningsresultat(dataTable)

        val beregningsresultat = hentVedtak(behandlingId).beregningsresultat

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
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

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
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        val vedtaksperioder = hentVedtak(behandlingId).vedtaksperioder

        val forventedeVedtaksperioder = mapVedtaksperioder(dataTable)

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

    private fun mapVedtaksperioder(dataTable: DataTable): List<Vedtaksperiode> =
        dataTable.mapRad { rad ->
            vedtaksperiode(
                id = vedtaksperiodeId,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                målgruppe =
                    parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad)
                        ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad) ?: AktivitetType.TILTAK,
            )
        }

    private fun mapVedtaksperioderDto(dataTable: DataTable): List<VedtaksperiodeLæremidlerDto> =
        dataTable.mapRad { rad ->
            vedtaksperiodeDto(
                id = vedtaksperiodeId,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                målgruppe =
                    parseValgfriEnum<FaktiskMålgruppe>(BeregningNøkler.MÅLGRUPPE, rad)
                        ?: FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad) ?: AktivitetType.TILTAK,
            )
        }

    private fun hentVedtak(behandlingId: BehandlingId): InnvilgelseEllerOpphørLæremidler =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()
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
        val forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId(behandlingId)
        return saksbehandling(
            id = behandlingId,
            fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER),
            forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
            revurderFra = revurderFra,
            type = if (forrigeIverksatteBehandlingId != null) BehandlingType.REVURDERING else BehandlingType.FØRSTEGANGSBEHANDLING,
        )
    }

    private fun forrigeIverksatteBehandlingId(behandlingId: BehandlingId): BehandlingId? {
        val behandlingIdInt = behandlingIdTilTestId(behandlingId)
        return if (behandlingIdInt > 1) {
            testIdTilBehandlingId.getValue(behandlingIdInt - 1)
        } else {
            null
        }
    }
}
