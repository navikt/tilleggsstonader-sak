package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.iverksett.tilIverksettDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt.KonsistensavstemmingTilkjentYtelseDto
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilkjentYtelseService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val fagsakService: FagsakService,
) {

    fun hentForBehandlingEllerNull(behandlingId: UUID): TilkjentYtelse? {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
    }

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")
    }

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse)
    }

    fun harLøpendeUtbetaling(behandlingId: UUID): Boolean {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?.let { it.andelerTilkjentYtelse.any { andel -> andel.stønadTom.isAfter(LocalDate.now()) } } ?: false
    }

    fun finnTilkjentYtelserTilKonsistensavstemming(
        stønadstype: Stønadstype,
        datoForAvstemming: LocalDate,
    ): List<KonsistensavstemmingTilkjentYtelseDto> {
        val tilkjentYtelser = tilkjentYtelseRepository.finnTilkjentYtelserTilKonsistensavstemming(stønadstype, datoForAvstemming)

        return tilkjentYtelser.chunked(PdlClient.MAKS_ANTALL_IDENTER).map { mapTilDto(it, datoForAvstemming) }.flatten()
    }

    private fun mapTilDto(
        tilkjenteYtelser: List<TilkjentYtelse>,
        datoForAvstemming: LocalDate,
    ): List<KonsistensavstemmingTilkjentYtelseDto> {
        val behandlinger = behandlingService.hentBehandlinger(tilkjenteYtelser.map { it.behandlingId }.toSet())
            .associateBy { it.id }

        val fagsakerMedOppdatertPersonIdenter =
            fagsakService.fagsakerMedOppdatertePersonIdenter(behandlinger.map { it.value.fagsakId })
                .associateBy { it.id }

        return tilkjenteYtelser.map { tilkjentYtelse ->
            val behandling = behandlinger[tilkjentYtelse.behandlingId]
                ?: error("Finner ikke behandling for behandlingId=${tilkjentYtelse.behandlingId}")
            val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.stønadTom.isEqualOrAfter(datoForAvstemming) }
                .filter { it.beløp > 0 }
                .map { it.tilIverksettDto() }

            val fagsakMedOppdatertPersonIdent = fagsakerMedOppdatertPersonIdenter[behandling.fagsakId]
                ?: error("Finner ikke fagsak for fagsakId=${behandling.fagsakId}")

            KonsistensavstemmingTilkjentYtelseDto(
                behandlingId = tilkjentYtelse.behandlingId,
                eksternBehandlingId = behandling.eksternId.id,
                eksternFagsakId = fagsakMedOppdatertPersonIdent.eksternId.id,
                personIdent = fagsakMedOppdatertPersonIdent.hentAktivIdent(),
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            )
        }
    }

    fun slettTilkjentYtelseForBehandling(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering"
        }
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let { tilkjentYtelseRepository.deleteById(it.id) }
    }
}
