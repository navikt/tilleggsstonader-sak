package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingMessageProducer
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Configuration
@Profile("simuler-utbetalingstatus")
class SimulerUtbetalingStatusConfig(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val utbetalingStatusHåndterer: UtbetalingStatusHåndterer,
) {
    @Bean
    @Primary
    fun utbetalingMessageProducer(): UtbetalingMessageProducer {
        val mock = mockk<UtbetalingMessageProducer>()
        every {
            mock.sendUtbetalinger(any(), any())
        } answers {
            val transaksjonsId = firstArg<UUID>()
            applicationEventPublisher.publishEvent(UtbetalingSendtEvent(transaksjonsId))
        }
        return mock
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun simulerSendMelding(utbetalingSendtEvent: UtbetalingSendtEvent) {
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            utbetalingSendtEvent.transaksjonsId.toString(),
            melding =
                UtbetalingStatusRecord(
                    status = UtbetalingStatus.OK,
                    detaljer =
                        UtbetalingStatusDetaljer(
                            ytelse = "TILLSTDR",
                            linjer = emptyList(),
                        ),
                    error = null,
                ),
            utbetalingGjelderFagsystem = UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )
    }

    data class UtbetalingSendtEvent(
        val transaksjonsId: UUID,
    ) : ApplicationEvent(transaksjonsId)
}
