package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.mockk.verify
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.IverksettClientConfig.Companion.clearMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class IverksettServiceTest : IntegrationTest() {

    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @BeforeEach
    fun setUp() {
        clearMock(iverksettClient)
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

        iverksettService.iverksett(behandling.id)

        verify(exactly = 0) { iverksettClient.iverksett(any()) }
    }

    @Test
    fun `skal iverksette og oppdatere andeler med status`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(resultat = BehandlingResultat.INNVILGET))
        totrinnskontrollRepository.insert(
            totrinnskontroll(
                status = TotrinnInternStatus.GODKJENT,
                behandlingId = behandling.id,
                beslutter = "beslutter",
            ),
        )
        val tilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id))

        iverksettService.iverksett(behandling.id)

        verify(exactly = 1) { iverksettClient.iverksett(any()) }

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id)
        val andel = oppdatertTilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.iverksetting?.iverksettingId).isEqualTo(behandling.id)
        assertThat(andel.status).isEqualTo(StatusIverksetting.SENDT)
    }

    @Test
    fun `skal oppdatere andeler med riktig status når status fra iverksett er ok`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(resultat = BehandlingResultat.INNVILGET))
        val andel = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            status = StatusIverksetting.SENDT,
            iverksetting = Iverksetting(behandling.id, LocalDateTime.now()),
        )
        val tilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingId = behandling.id, null, andel))

        iverksettService.hentStatusOgOppdaterAndeler(behandling.id)

        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByIdOrThrow(tilkjentYtelse.id)
        val oppdatertAndel = oppdatertTilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(oppdatertAndel.iverksetting?.iverksettingId).isEqualTo(behandling.id)
        assertThat(oppdatertAndel.status).isEqualTo(StatusIverksetting.OK)
    }
}
