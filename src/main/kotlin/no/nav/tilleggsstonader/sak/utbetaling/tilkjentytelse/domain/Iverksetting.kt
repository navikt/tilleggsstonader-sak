package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Håndtere:
 * * Behandling 2 fjerner alle andeler
 * * Hvordan vite om det er endring mellom behandling 1 og 2?
 * * Innværende måned skal utbetales direkte, lag en task for inneværende måned direkt når man iverksetter?
 *
 * Hvis man iverksetter noe idag, jobbet for å iverksette den feiler
 * Så prøver man å iverksette noe i morgen, hva skjer då? Hva skjer hvis del 2 kommer først?
 *
 * Nye rader for oppdatering av status?
 *
 * Når vi iverksetter skal man peke til forrige iverksettingen
 */
@Repository
interface IverksettingRepository : RepositoryInterface<Iverksetting, UUID>, InsertUpdateRepository<Iverksetting> {
    fun findByBehandlingId(behandlingId: UUID): Iverksetting?

    @Query(
        """
        SELECT i.* FROM iverksetting i 
        WHERE fagsak_id = :fagsakId
        ORDER BY opprettet DESC
        """
    )
    fun finnSisteForFagsak(fagsakId: UUID): Iverksetting?

    @Query("SELECT id FROM iverksett WHERE status = 'UBEHANDLET' AND utbetalingsdato < :dato")
    fun finnIverksettingerSomSkalUtbetales(dato: LocalDate): List<UUID>

}

@Repository
interface IverksettingHistorikkRepository
    : RepositoryInterface<IverksettingHistorikk, UUID>, InsertUpdateRepository<IverksettingHistorikk>

@Service
@TaskStepBeskrivelse(taskStepType = "type", beskrivelse = "beskrivelse")
class IverksettTask(
    private val iverksettingService: IverksettingService
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        TODO("Not yet implemented")
    }

}

// TODO vent på status fra iverksettTask
@Service
class IverksettingService(
    private val iverksettingRepository: IverksettingRepository,
    private val iverksettingHistorikkRepository: IverksettingHistorikkRepository,
    private val iverksettClient: IverksettClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // denne lages når man har en
    @Transactional
    fun opprettIverksetting(saksbehandling: Saksbehandling) {
        // deaktivere utbetalinger for fagsak
        val sisteIverksetting = iverksettingRepository.finnSisteForFagsak(saksbehandling.fagsakId)
        if (sisteIverksetting != null) {
            val status = sisteIverksetting.status
            when(status) {
                IverksettingStatus.OK -> {}
                IverksettingStatus.UBEHANDLET -> {
                    iverksettingRepository.update(sisteIverksetting)
                }
                IverksettingStatus.SENDT -> TODO()
                IverksettingStatus.UAKTUELL -> {
                    logger.warn("Siste iverksetting=${sisteIverksetting.id} har status=$status")
                }
            }
        }

        iverksettingRepository.insert(
            Iverksetting(
                fagsakId = saksbehandling.fagsakId,
                behandlingId = saksbehandling.id,
                utbetalingsdato = LocalDate.now()
            )
        )
    }

    fun finnAktuelleIverksettinger(dato: LocalDate): List<UUID> {
        return iverksettingRepository.finnIverksettingerSomSkalUtbetales(dato = dato)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun iverksett(id: UUID) {
        val iverksetting = iverksettingRepository.findByIdOrThrow(id)
        if (iverksetting.status != IverksettingStatus.UBEHANDLET) {
            logger.warn("Iverksetting=${iverksetting.id} har status=${iverksetting.status} - ignorerer iverksetting")
            return
        }
        // opprett hentstatus task
        oppdaterStatus(iverksetting, IverksettingStatus.SENDT)
        // send til iverksett
    }

    @Transactional
    fun sjekkStatus(id: UUID) {
        val iverksetting = iverksettingRepository.findByIdOrThrow(id)
        feilHvis(iverksetting.status != IverksettingStatus.SENDT) {
            "Iverksetting=${iverksetting.id} har status=${iverksetting.status} - ignorerer iverksetting"
        }
        val status = iverksettClient.sjekkStatus(id)
        when (status) {
            IverksattStatus.VENTER -> TODO("Try again")
            IverksattStatus.FEILET -> error("feilet")
            IverksattStatus.OK -> {
                oppdaterStatus(iverksetting, IverksettingStatus.OK)
                // TODO opprett for neste måned?
            }
        }
    }

    @Transactional
    fun oppdaterStatus(iverksetting: Iverksetting, nyStatus: IverksettingStatus) {
        iverksettingHistorikkRepository.insert(iverksetting.tilHistorikk())
        iverksettingRepository.update(iverksetting.copy(status = nyStatus, opprettet = LocalDateTime.now()))
    }
}

@Service
class IverksettClient {
    fun sjekkStatus(iverksettId: UUID): IverksattStatus {
        TODO("")
    }
}

data class Iverksetting(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val behandlingId: UUID,
    val utbetalingsdato: LocalDate,
    val status: IverksettingStatus = IverksettingStatus.UBEHANDLET,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    @Version
    val version: Int = 0
) {
    fun tilHistorikk() = IverksettingHistorikk(id = id, opprettet = opprettet, status = status)
}

data class IverksettingHistorikk(
    @Id
    val id: UUID,
    val opprettet: LocalDateTime,
    val status: IverksettingStatus,
)

enum class IverksettingStatus {
    UBEHANDLET,
    SENDT,
    OK,
    UAKTUELL,
}

enum class IverksattStatus {
    VENTER,
    OK,
    FEILET
}