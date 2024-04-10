package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.CapturingSlot
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.IverksettClientConfig.Companion.clearMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
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

    @Test
    fun `skal ikke iverksette hvis resultat er avslag`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(resultat = BehandlingResultat.AVSLÅTT))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned)

        verify(exactly = 0) { iverksettClient.iverksett(any()) }
    }

    @Test
    fun `skal iverksette og oppdatere andeler med status`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, resultat = BehandlingResultat.INNVILGET))
        lagreTotrinnskontroll(behandling)
        val tilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        iverksettService.iverksett(behandling.id, behandling.id, nesteMåned)

        verify(exactly = 1) { iverksettClient.iverksett(any()) }

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id)
        val andel = oppdatertTilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.iverksetting?.iverksettingId).isEqualTo(behandling.id)
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

        val tilkjentYtelse =
            tilkjentYtelse(behandlingId = behandling.id, andeler = lagAndeler(behandling))
        val tilkjentYtelse2 =
            tilkjentYtelse(behandlingId = behandling2.id, andeler = lagAndeler(behandling2))

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
            iverksettService.iverksett(behandling.id, iverksettingId, nesteMåned)

            val andeler = hentAndeler(behandling)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId).isEqualTo(behandling.id)
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling.id)
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

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId).isEqualTo(behandling.id)
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling.id)
        }

        @Test
        fun `andre behandling - første iverksetting med 2 iverksettinger`() {
            val iverksettingIdBehandling1 = UUID.randomUUID()
            oppdaterAndelerTilOk(behandling)
            iverksettService.iverksett(behandling.id, iverksettingIdBehandling1, nesteMåned)

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

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId).isEqualTo(behandling.id)
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
            iverksettService.iverksett(behandling2.id, iverksettingId, nesteMåned)

            val andeler = hentAndeler(behandling2)

            andeler.forMåned(forrigeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            andeler.forMåned(nåværendeMåned).assertHarStatusOgId(StatusIverksetting.OK, behandling2.id)
            andeler.forMåned(nesteMåned).assertHarStatusOgId(StatusIverksetting.SENDT, iverksettingId)
            andeler.forMåned(nestNesteMåned).assertHarStatusOgId(StatusIverksetting.UBEHANDLET)

            iverksettingDto.assertUtbetalingerInneholder(forrigeMåned, nåværendeMåned, nesteMåned)

            assertThat(iverksettingDto.captured.forrigeIverksetting?.behandlingId).isEqualTo(behandling2.id)
            assertThat(iverksettingDto.captured.forrigeIverksetting?.iverksettingId).isEqualTo(behandling2.id)
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
                iverksettService.iverksett(behandling.id, iverksettingId, nåværendeMåned)
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
    inner class IverksettingNullperioder {
        val fagsak = fagsak()

        val behandling =
            behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT)

        @Test
        fun `skal iverksette uten utbetalinger når første periode er fremover i tid`() {
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            lagreTotrinnskontroll(behandling)
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
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            lagreTotrinnskontroll(behandling)
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandlingId = behandling.id,
                    andeler = arrayOf(lagAndel(behandling, nesteMåned)),
                ),
            )
            iverksettService.iverksettBehandlingFørsteGang(behandling.id)
            oppdaterAndelerTilOk(behandling, StatusIverksetting.OK_UTEN_UTBETALING)

            val iverksettingId = UUID.randomUUID()
            iverksettService.iverksett(behandling.id, iverksettingId, nesteMåned)

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

    private fun hentAndeler(behandling: Behandling): Set<AndelTilkjentYtelse> {
        return tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
    }

    private fun Collection<AndelTilkjentYtelse>.forMåned(yearMonth: YearMonth) =
        this.single { it.fom == yearMonth.atDay(1) }

    fun AndelTilkjentYtelse.assertHarStatusOgId(statusIverksetting: StatusIverksetting, iverksettingId: UUID? = null) {
        assertThat(this.statusIverksetting).isEqualTo(statusIverksetting)
        assertThat(this.iverksetting?.iverksettingId).isEqualTo(iverksettingId)
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

    private fun lagAndel(behandling: Behandling, måned: YearMonth, beløp: Int = 10) = andelTilkjentYtelse(
        kildeBehandlingId = behandling.id,
        fom = måned.atDay(1),
        tom = måned.atEndOfMonth(),
        beløp = beløp,
    )
}
