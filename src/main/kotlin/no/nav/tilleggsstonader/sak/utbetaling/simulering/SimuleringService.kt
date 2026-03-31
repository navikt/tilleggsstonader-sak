package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import no.nav.tilleggsstonader.sak.util.forrigeVirkedag
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    fun skalSendeVarsel(behandlingId: BehandlingId): String? {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val alleFagsaker =
            fagsakService
                .finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId)
        return when (fagsak.stønadstype) {
            Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR ->
                håndterDagligReiseVarsel(alleFagsaker)

            Stønadstype.BOUTGIFTER, Stønadstype.LÆREMIDLER, Stønadstype.BARNETILSYN ->
                håndterTilsynbarnLæremidlerBoutgifterVarsel(alleFagsaker)
        }
    }

    fun håndterDagligReiseVarsel(alleFagsaker: Fagsaker): String? {
        val dagensDato = LocalDate.now()
        val relevanteFagsaker =
            listOfNotNull(alleFagsaker.dagligReiseTso, alleFagsaker.dagligReiseTsr)
        return if (erVarselRelevantForDagligReise(relevanteFagsaker, dagensDato)) {
            "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
        } else {
            null
        }
    }

    fun håndterTilsynbarnLæremidlerBoutgifterVarsel(alleFagsaker: Fagsaker): String? {
        val relevanteFagsaker =
            listOfNotNull(alleFagsaker.barnetilsyn, alleFagsaker.læremidler, alleFagsaker.boutgifter)
        val periode = Datoperiode(LocalDate.now().forrigeVirkedag(), LocalDate.now())
        return if (erVarselRelevantForTilsynBarnLæremidlerBoutgifter(relevanteFagsaker, periode)) {
            "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
        } else {
            null
        }
    }

    private fun erVarselRelevantForDagligReise(
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

            tilkjentYtelse.andelerTilkjentYtelse.any { andel ->
                val iverksettingDato =
                    andel.iverksetting?.iverksettingTidspunkt?.toLocalDate()

                iverksettingDato?.isEqual(dagensDato) == true
            }
        }
    }

    private fun erVarselRelevantForTilsynBarnLæremidlerBoutgifter(
        fagsaker: List<Fagsak>,
        periode: Datoperiode,
    ): Boolean {
        return fagsaker.any { relevantFagsak ->

            val behandlingId =
                behandlingService
                    .finnSisteIverksatteBehandling(relevantFagsak.id)
                    ?.id ?: return@any false

            val tilkjentYtelse =
                tilkjentYtelseService.hentForBehandling(behandlingId)

            tilkjentYtelse.andelerTilkjentYtelse.any { andel ->
                val iverksettingDato =
                    andel.iverksetting?.iverksettingTidspunkt?.toLocalDate()

                iverksettingDato != null && periode.inneholder(iverksettingDato)
            }
        }
    }
}
