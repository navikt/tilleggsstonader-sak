package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.mockk.every
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.IverksettClientConfig.Companion.clearMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class IverksettStatusServiceTest : IntegrationTest() {

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var iverksettStatusService: IverksettStatusService

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    private val behandling = behandling(resultat = BehandlingResultat.INNVILGET)

    @BeforeEach
    fun setUp() {
        clearMock(iverksettClient)
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        clearMock(iverksettClient)
    }

    @Test
    fun `skal oppdatere iverksatte andeler med status OK`() {
        val iverksattAndel = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            statusIverksetting = StatusIverksetting.SENDT,
            iverksetting = Iverksetting(behandling.id, LocalDateTime.now()),
        )
        val andelIkkeSendt = andelTilkjentYtelse(kildeBehandlingId = behandling.id)
        val tilkjentYtelse = opprettTilkjentYtelse(behandling, iverksattAndel, andelIkkeSendt)

        iverksettStatusService.hentStatusOgOppdaterAndeler(behandling.id, behandling.id)

        val oppdaterteAndeler = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id).andelerTilkjentYtelse
        assertThat(oppdaterteAndeler.single { it.id == iverksattAndel.id }.statusIverksetting)
            .isEqualTo(StatusIverksetting.OK)
        assertThat(oppdaterteAndeler.single { it.id == andelIkkeSendt.id }.statusIverksetting)
            .isEqualTo(StatusIverksetting.UBEHANDLET)
    }

    @ParameterizedTest
    @EnumSource(value = IverksettStatus::class, names = ["OK"], mode = EnumSource.Mode.EXCLUDE)
    fun `skal kaste feil hvis status ikke er OK`(status: IverksettStatus) {
        every { iverksettClient.hentStatus(any(), any()) } returns status
        assertThatThrownBy {
            iverksettStatusService.hentStatusOgOppdaterAndeler(behandling.id, behandling.id)
        }.isInstanceOf(TaskExceptionUtenStackTrace::class.java)
    }

    @Test
    fun `skal kaste feil hvis andel for iverksetting har annen status enn SENDT`() {
        val iverksetting = Iverksetting(behandling.id, LocalDateTime.now())
        val andel =
            andelTilkjentYtelse(behandling.id, statusIverksetting = StatusIverksetting.OK, iverksetting = iverksetting)
        opprettTilkjentYtelse(behandling, andel)

        assertThatThrownBy {
            iverksettStatusService.hentStatusOgOppdaterAndeler(behandling.id, behandling.id)
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("som har annen status enn SENDT")
    }

    @Test
    fun `skal kaste feil hvis det ikke finnes noen andeler av gitt iverksettingId`() {
        val andel = andelTilkjentYtelse(behandling.id)
        opprettTilkjentYtelse(behandling, andel)

        assertThatThrownBy {
            iverksettStatusService.hentStatusOgOppdaterAndeler(behandling.id, behandling.id)
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Forventet å finne minumum en andel")
    }

    private fun opprettTilkjentYtelse(
        behandling: Behandling,
        vararg andel: AndelTilkjentYtelse,
    ) = tilkjentYtelseRepository.insert(
        tilkjentYtelse(
            behandlingId = behandling.id,
            startdato = null,
            andeler = *andel,
        ),
    )
}
