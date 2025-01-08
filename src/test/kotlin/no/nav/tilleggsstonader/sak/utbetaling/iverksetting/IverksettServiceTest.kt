package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.CapturingSlot
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.IverksettClientConfig.Companion.clearMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class IverksettServiceTest : IntegrationTest() {

    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Autowired
    lateinit var taskService: TaskService

    val forrigeMåned = YearMonth.now().minusMonths(1)
    val nåværendeMåned = YearMonth.now()
    val nesteMåned = YearMonth.now().plusMonths(1)
    val nestNesteMåned = YearMonth.now().plusMonths(2)

    val iverksettingDto = slot<IverksettDto>()

    @BeforeEach
    fun setUp() {
        clearMock(iverksettClient)
        justRun { iverksettClient.iverksett(capture(iverksettingDto)) }
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        clearMock(iverksettClient)
    }

    private fun IverksettService.iverksett(behandlingId: BehandlingId, iverksettingId: BehandlingId, utbetalingsdato: LocalDate) {
        this.iverksett(behandlingId, iverksettingId.id, utbetalingsdato)
    }

    @Test
    fun `skal ikke iverksette hvis resultat er avslag`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(resultat = BehandlingResultat.AVSLÅTT))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned.atEndOfMonth())

        verify(exactly = 0) { iverksettClient.iverksett(any()) }
    }

    @Test
    fun `skal iverksette og oppdatere andeler med status`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, resultat = BehandlingResultat.INNVILGET))
        lagreTotrinnskontroll(behandling)
        val tilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned.atEndOfMonth())

        verify(exactly = 1) { iverksettClient.iverksett(any()) }

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id)
        val andel = oppdatertTilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.iverksetting?.iverksettingId).isEqualTo(behandling.id.id)
        assertThat(andel.statusIverksetting).isEqualTo(StatusIverksetting.SENDT)
        assertHarOpprettetTaskForÅSjekkeStatus(fagsak, behandling)
    }

    private fun assertHarOpprettetTaskForÅSjekkeStatus(fagsak: Fagsak, behandling: Behandling) {
        val task = taskService.finnTasksMedStatus(Status.entries, HentStatusFraIverksettingTask.TYPE).single()
        val eksternBehandlingId = testoppsettService.hentSaksbehandling(behandling.id).eksternId
        assertThat(objectMapper.readValue<Map<String, Any>>(task.payload)).isEqualTo(
            mapOf(
                "eksternFagsakId" to fagsak.eksternId.id.toInt(),
                "behandlingId" to behandling.id.toString(),
                "eksternBehandlingId" to eksternBehandlingId.toInt(),
                "iverksettingId" to behandling.id.toString(),
            ),
        )
    }

    @Nested
    inner class IverksettingFlyt {

        val fagsak = fagsak()

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)
        val behandling2 =
            behandling(
                fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
                forrigeBehandlingId = behandling.id,
            )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, andeler = lagAndeler(behandling))
        val tilkjentYtelse2 = tilkjentYtelse(behandlingId = behandling2.id, andeler = lagAndeler(behandling2))

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            tilkjentYtelseRepository.insert(tilkjentYtelse)
            lagreTotrinnskontroll(behandling)
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
        }

        @Test
        fun `første behandling - første iverksetting`() {
            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting).isNull()
        }

        @Test
        fun `første behandling  - andre iverksetting`() {
            val iverksettingId = UUID.randomUUID()
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksett(behandling.id, iverksettingId, nesteMåned.atEndOfMonth())

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId)
                .isEqualTo(hentEksternBehandlingId(behandling))
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling.id.id)
        }

        @Test
        fun `andre behandling - første iverksetting - skal bruke behandling2 som iverksettingId`() {
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            lagreTotrinnskontroll(behandling2)
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId)
                .isEqualTo(hentEksternBehandlingId(behandling))
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling.id.id)
        }

        @Test
        fun `andre behandling - første iverksetting med 2 iverksettinger`() {
            val iverksettingIdBehandling1 = UUID.randomUUID()
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksett(behandling.id, iverksettingIdBehandling1, nesteMåned.atEndOfMonth())

            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            lagreTotrinnskontroll(behandling2)
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId)
                .isEqualTo(hentEksternBehandlingId(behandling))
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(iverksettingIdBehandling1)
        }

        @Test
        fun `andre behandling  - andre iverksetting`() {
            val iverksettingId = UUID.randomUUID()

            oppdaterAndelerTilOk(behandling)

            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(tilkjentYtelse2)
            lagreTotrinnskontroll(behandling2)

            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)
            oppdaterAndelerTilOk(behandling2)
            iverksettService.iverksett(behandling2.id, iverksettingId, nesteMåned.atEndOfMonth())

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId)
                .isEqualTo(hentEksternBehandlingId(behandling2))
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling2.id.id)
        }

        @Test
        fun `andre behandling kun med 0-beløp - skal ikke sende noen andeler`() {
            oppdaterAndelerTilOk(behandling)
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling2.id,
                    lagAndel(behandling2, forrigeMåned, beløp = 0),
                ),
            )
            lagreTotrinnskontroll(behandling2)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling2.id)

            assertThat(iverksettingDto.captured.vedtak.utbetalinger).isEmpty()
        }

        @Test
        fun `skal feile hvis forrige iverksetting ikke er ferdigstilt`() {
            val iverksettingId = UUID.randomUUID()
            assertThatThrownBy {
                iverksettService.iverksett(behandling.id, iverksettingId, nåværendeMåned.atEndOfMonth())
            }.hasMessageContaining("det finnes tidligere andeler med annen status enn OK/UBEHANDLET")
        }

        @Test
        fun `skal markere andeler fra forrige behandling som UAKTUELL`() {
            oppdaterAndelerTilOk(behandling)
            testoppsettService.lagre(behandling2)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling2.id,
                    lagAndel(behandling2, forrigeMåned, beløp = 100),
                ),
            )
            lagreTotrinnskontroll(behandling2)
            iverksettService.iverksettBehandlingFørsteGang(behandling2.id)

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UAKTUELL)
        }

        @Test
        fun `skal feile hvis en andel fra forrige behandling er sendt til iverksetting med ikke kvittert OK`() {
            testoppsettService.lagre(behandling2)

            assertThatThrownBy { iverksettService.iverksettBehandlingFørsteGang(behandling2.id) }
                .hasMessageContaining("Andeler fra forrige behandling er sendt til iverksetting men ikke kvittert OK. Prøv igjen senere.")
        }
    }

    @Nested
    inner class HåndteringAvAndelSomSkalUtbetalesUlikeDager {

        val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
            lagreTotrinnskontroll(behandling)
        }

        @Test
        fun `Skal kun iverksette andeler innenfor en måned som gjelder for gitt utbetalingsdato`() {
            val andel1 = andelTilkjentYtelse(
                kildeBehandlingId = behandling.id,
                fom = nåværendeMåned.atDay(1),
            )
            val andel2 = andelTilkjentYtelse(
                kildeBehandlingId = behandling.id,
                fom = nesteMåned.atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag(),
            )
            val andel3 = andelTilkjentYtelse(
                kildeBehandlingId = behandling.id,
                fom = nesteMåned.atDay(15).datoEllerNesteMandagHvisLørdagEllerSøndag(),
            )
            tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id, andel1, andel2, andel3))

            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            with(hentAndeler(behandling)) {
                single { it.id == andel1.id }.assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
                single { it.id == andel2.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                single { it.id == andel3.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            }

            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK)

            // iverksetter med utbetalingsdato for andel2, som er den samme måneden som andel3, men andel 3 er senere den måneden
            iverksettService.iverksett(behandling.id, andel2.id, utbetalingsdato = andel2.fom)
            with(hentAndeler(behandling)) {
                single { it.id == andel1.id }.assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
                single { it.id == andel2.id }.assertHarStatusOgId(StatusIverksetting.SENDT, andel2.id)
                single { it.id == andel3.id }.assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
            }
        }
    }

    @Nested
    inner class IverksettFlytMedAndelerSomIkkeSkalUtbetales {

        val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
            lagreTotrinnskontroll(behandling)
        }

        @Test
        fun `skal ikke sende andeler som har status=VENTER_PÅ_SATS_ENDRING som skal satsjusteres før de blir iverksatte`() {
            val tilkjentYtelse = tilkjentYtelse(
                behandlingId = behandling.id,
                lagAndel(behandling, forrigeMåned),
                lagAndel(behandling, nesteMåned),
                lagAndel(behandling, nestNesteMåned, statusIverksetting = StatusIverksetting.VENTER_PÅ_SATS_ENDRING),
            )
            tilkjentYtelseRepository.insert(tilkjentYtelse)

            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            with(hentAndeler(behandling)) {
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
                forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
                forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            }
            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK)

            val iverksettingId = UUID.randomUUID()
            iverksettService.iverksett(behandling.id, iverksettingId, nestNesteMåned.plusMonths(1).atEndOfMonth())
            with(hentAndeler(behandling)) {
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
                forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
                forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            }
        }

        @Test
        fun `skal ikke iverksette noen perioder hvis man innvilget en periode bak i tiden som fortsatt status=VENTER_PÅ_SATS_ENDRING`() {
            val tilkjentYtelse = tilkjentYtelse(
                behandlingId = behandling.id,
                lagAndel(behandling, forrigeMåned, statusIverksetting = StatusIverksetting.VENTER_PÅ_SATS_ENDRING),
            )
            tilkjentYtelseRepository.insert(tilkjentYtelse)
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)

            with(hentAndeler(behandling)) {
                verifiserHarLagtTilNullPeriode(forrigeMåned.minusMonths(1))
                forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
                assertThat(this).hasSize(2)
            }
        }
    }

    @Nested
    inner class IverksettingNullperioder {
        val fagsak = fagsak()

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            lagreTotrinnskontroll(behandling)
        }

        @Test
        fun `skal iverksette uten utbetalinger når første periode er fremover i tid`() {
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandlingId = behandling.id,
                    andeler = arrayOf(lagAndel(behandling, nesteMåned)),
                ),
            )

            iverksettService.iverksettBehandlingFørsteGang(behandling.id)

            val andeler = hentAndeler(behandling)

            assertThat(andeler).hasSize(2)
            val andelForrigeMåned = andeler.forMåned(nåværendeMåned)
            andelForrigeMåned.assertHarStatusOgId(StatusIverksetting.SENDT, behandling.id)
            assertThat(andelForrigeMåned.beløp).isEqualTo(0)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)
        }

        @Test
        fun `skal iverksette måned 2 når status er OK_UTEN_UTBETALING for første måned`() {
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandlingId = behandling.id,
                    andeler = arrayOf(lagAndel(behandling, nesteMåned)),
                ),
            )
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK_UTEN_UTBETALING)

            val iverksettingId = UUID.randomUUID()
            iverksettService.iverksett(behandling.id, iverksettingId, nesteMåned.atEndOfMonth())

            val andeler = hentAndeler(behandling)
            val andelForrigeMåned = andeler.forMåned(nåværendeMåned)
            andelForrigeMåned.assertHarStatusOgId(StatusIverksetting.OK_UTEN_UTBETALING, behandling.id)
            assertThat(andelForrigeMåned.beløp).isEqualTo(0)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
        }
    }

    private fun CapturingSlot<IverksettDto>.assertUtbetalingerInneholder(vararg måned: YearMonth) {
        assertThat(captured.vedtak.utbetalinger.map { YearMonth.from(it.fraOgMedDato) })
            .containsExactly(*måned)
    }

    private fun lagreTotrinnskontroll(behandling: Behandling) {
        totrinnskontrollRepository.insert(
            totrinnskontroll(
                status = TotrinnInternStatus.GODKJENT,
                behandlingId = behandling.id,
                beslutter = "beslutter",
            ),
        )
    }

    private fun hentEksternBehandlingId(behandling: Behandling) =
        testoppsettService.hentSaksbehandling(behandling.id).eksternId.toString()

    private fun hentAndeler(behandling: Behandling): Set<AndelTilkjentYtelse> {
        return tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
    }

    private fun Collection<AndelTilkjentYtelse>.forMåned(yearMonth: YearMonth): AndelTilkjentYtelse {
        val dato = yearMonth.atDay(1)
        return this.single {
            val datoEllerNesteMandag =
                if (it.satstype == Satstype.DAG) dato.datoEllerNesteMandagHvisLørdagEllerSøndag() else dato
            it.fom == datoEllerNesteMandag
        }
    }

    fun AndelTilkjentYtelse.assertHarStatusOgId(statusIverksetting: StatusIverksetting, iverksettingId: BehandlingId?) {
        assertHarStatusOgId(statusIverksetting, iverksettingId?.id)
    }

    fun AndelTilkjentYtelse.assertHarStatusOgId(statusIverksetting: StatusIverksetting, iverksettingId: UUID? = null) {
        assertThat(this.statusIverksetting).isEqualTo(statusIverksetting)
        assertThat(this.iverksetting?.iverksettingId).isEqualTo(iverksettingId)
    }

    private fun Set<AndelTilkjentYtelse>.verifiserHarLagtTilNullPeriode(yearMonth: YearMonth) {
        with(forMåned(yearMonth)) {
            assertThat(beløp).isEqualTo(0)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.SENDT)
            assertThat(type).isEqualTo(TypeAndel.UGYLDIG)
        }
    }

    private fun oppdaterAndelerTilOk(
        behandling: Behandling,
        statusIverksetting: StatusIverksetting = StatusIverksetting.OK,
    ) {
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
        val oppdaterteAndeler = andeler.filter { it.statusIverksetting == StatusIverksetting.SENDT }
            .map { it.copy(statusIverksetting = statusIverksetting) }
        andelTilkjentYtelseRepository.updateAll(oppdaterteAndeler)
    }

    private fun lagAndeler(behandling: Behandling) = arrayOf(
        lagAndel(behandling, forrigeMåned),
        lagAndel(behandling, nåværendeMåned),
        lagAndel(behandling, nesteMåned),
        lagAndel(behandling, nestNesteMåned),
    )

    private fun lagAndel(
        behandling: Behandling,
        måned: YearMonth,
        beløp: Int = 10,
        statusIverksetting: StatusIverksetting = StatusIverksetting.UBEHANDLET,
    ): AndelTilkjentYtelse {
        val fom = måned.atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag()
        return andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            fom = fom,
            tom = fom,
            beløp = beløp,
            statusIverksetting = statusIverksetting,
        )
    }
}
