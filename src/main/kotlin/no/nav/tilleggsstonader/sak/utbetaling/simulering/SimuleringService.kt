package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class SimuleringService(
    private val simuleringClient: SimuleringClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val utbetalingV3Mapper: UtbetalingV3Mapper,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentLagretSimulering(behandlingId: BehandlingId): Simuleringsresultat? = simuleringsresultatRepository.findByIdOrNull(behandlingId)

    fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette simulering for behandling=$behandlingId fordi den har har status ${saksbehandling.status.visningsnavn()}."
        }
        logger.info("Sletter simulering for behandling=$behandlingId")
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    @Transactional
    fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()

        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke hente og lagre simuleringsresultat for behandling=${saksbehandling.id} fordi den har har status ${saksbehandling.status.visningsnavn()}."
        }

        val resultat = simulerMedTilkjentYtelse(saksbehandling)

        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = resultat?.let { SimuleringKontraktTilDomeneMapper.map(it) },
                ingenEndringIUtbetaling =
                    resultat == null || resultat.oppsummeringer.isNullOrEmpty() || resultat.status == "OK_UTEN_ENDRING",
            ),
        )
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): SimuleringResponseDto? {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)

        return simuleringClient.simuler(
            utbetalingV3Mapper.lagSimuleringDtoer(
                saksbehandling,
                tilkjentYtelse.andelerTilkjentYtelse,
            ),
        )
    }

    // Duration of the varsel
    fun setVarselTidspunkt() {
    }

    fun skalSendeVarsel(behandlingId: BehandlingId): String? {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (fagsak.stønadstype) {
            Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR ->
                håndterDagligReiseVarsel(fagsak)

            Stønadstype.BOUTGIFTER, Stønadstype.LÆREMIDLER, Stønadstype.BARNETILSYN -> TODO()
        }
    }

    fun håndterDagligReiseVarsel(fagsak: Fagsak): String? {
        val dagensDato = LocalDate.now()
        val alleFagsaker =
            fagsakService
                .finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId)
        val relevanteFagsaker =
            listOfNotNull(alleFagsaker.dagligReiseTso, alleFagsaker.dagligReiseTsr)
        return if (erVarselRelevant(relevanteFagsaker, dagensDato)) {
            "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
        } else {
            null
        }
    }

    private fun erVarselRelevant(
        fagsaker: List<Fagsak>,
        dagensDato: LocalDate,
    ): Boolean {
        return fagsaker.any { relevantFagsak ->

            val behandlingId =
                behandlingService
                    .finnSisteIverksatteBehandling(relevantFagsak.id)
                    ?.id ?: return@any false

            val tilkjentYtelse =
                tilkjentYtelseService.hentForBehandling(behandlingId)
            val erFredagEllerHelg =
                dagensDato.dayOfWeek == DayOfWeek.FRIDAY ||
                    dagensDato.dayOfWeek == DayOfWeek.SATURDAY ||
                    dagensDato.dayOfWeek == DayOfWeek.SUNDAY

            tilkjentYtelse.andelerTilkjentYtelse.any { andel ->
                val iverksettingDato =
                    andel.iverksetting?.iverksettingTidspunkt?.toLocalDate()

                val utbetalingsdato = andel.utbetalingsdato

                iverksettingDato?.isEqual(dagensDato) == true && utbetalingsdato.let { !it.isAfter(dagensDato) }
            }
        }
    }
}
