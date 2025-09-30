package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Søknadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilStønadstyper
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.UnleashUtil.getVariantWithNameOrDefault
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import org.springframework.stereotype.Service

@Service
class SøknadRoutingService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
    private val fagsakService: FagsakService,
    private val arenaService: ArenaService,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
    private val ytelseService: YtelseService,
) {
    fun harLagretRouting(
        ident: String,
        søknadstype: Søknadstype,
    ) = søknadRoutingRepository.findByIdentAndType(ident, søknadstype) != null

    fun skalRoutesTilNyLøsning(
        ident: String,
        søknadstype: Søknadstype,
    ): Boolean {
        val routingStrategi = bestemRoutingStrategi(søknadstype)

        return when (routingStrategi) {
            RoutingStrategi.RouteAlleSøkereTilNyLøsning -> true
            is RoutingStrategi.RouteEnkelteSøkereTilNyLøsning ->
                skalBrukerRoutesTilNyLøsning(ident, søknadstype, routingStrategi)
        }.also { loggRoutingResultatet(søknadstype, it) }
    }

    private fun skalBrukerRoutesTilNyLøsning(
        ident: String,
        søknadstype: Søknadstype,
        kontekst: RoutingStrategi.RouteEnkelteSøkereTilNyLøsning,
    ): Boolean {
        if (harLagretRouting(ident, søknadstype)) {
            logger.info("routing - søknadstype=$søknadstype harLagretRouting=true")
            return true
        }
        if (harBehandling(ident, søknadstype)) {
            lagreRouting(ident, søknadstype, mapOf("harBehandling" to true))
            return true
        }
        if (maksAntallErNådd(søknadstype, toggleId = kontekst.featureToggleMaksAntall)) {
            return false
        }
        if (kontekst.kreverAtSøkerErUtenAktivtVedtakIArena && harAktivtVedtakIArena(søknadstype, ident)) {
            return false
        }
        if (kontekst.kreverAktivtAapVedtak && harAktivtAapVedtak(ident)) {
            lagreRouting(ident, søknadstype, mapOf("harAktivAAP" to true))
            return true
        }

        return false
    }

    private fun maksAntallErNådd(
        søknadstype: Søknadstype,
        toggleId: ToggleId,
    ): Boolean {
        val maksAntall = unleashService.getVariantWithNameOrDefault(toggleId, "antall", 0)
        val antall = søknadRoutingRepository.countByType(søknadstype)
        logger.info("routing - stønadstype=$søknadstype antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun harAktivtVedtakIArena(
        søknadstype: Søknadstype,
        ident: String,
    ): Boolean =
        søknadstype.tilStønadstyper().any { stønadstype ->
            val arenaStatus = arenaService.hentStatus(ident, stønadstype)
            return arenaStatus.vedtak.harAktivtVedtak
                .also { loggArenaStatus(arenaStatus, søknadstype, it) }
        }

    private fun lagreRouting(
        ident: String,
        søknadstype: Søknadstype,
        detaljer: Any,
    ) {
        søknadRoutingRepository.insert(
            SøknadRouting(
                ident = ident,
                type = søknadstype,
                detaljer = JsonWrapper(objectMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun loggRoutingResultatet(
        søknadstype: Søknadstype,
        skalBehandlesINyLøsning: Boolean,
    ) {
        logger.info(
            "routing - " +
                "stønadstype=$søknadstype " +
                "skalBehandlesINyLøsning=$skalBehandlesINyLøsning",
        )
    }

    private fun harBehandling(
        ident: String,
        søknadstype: Søknadstype,
    ): Boolean {
        val harBehandling =
            søknadstype
                .tilStønadstyper()
                .mapNotNull { fagsakService.finnFagsak(personIdenter = setOf(ident), stønadstype = it) }
                .map { behandlingService.hentBehandlinger(fagsakId = it.id) }
                .isNotEmpty()

        logger.info("routing - søknadstype=$søknadstype harBehandling=$harBehandling")
        return harBehandling
    }

    private fun loggArenaStatus(
        arenaStatus: ArenaStatusDto,
        søknadstype: Søknadstype,
        harGyldigStatus: Boolean,
    ) {
        with(arenaStatus) {
            logger.info(
                "routing - søknadstype=$søknadstype harGyldigStatusArena=$harGyldigStatus " +
                    "harAktivSakUtenVedtak=${sak.harAktivSakUtenVedtak} " +
                    "harVedtak=${vedtak.harVedtak} " +
                    "harAktivtVedtak=${vedtak.harAktivtVedtak} " +
                    "harVedtakUtenUtfall=${vedtak.harVedtakUtenUtfall} " +
                    "vedtakTom=${vedtak.vedtakTom}",
            )
        }
    }

    private fun harAktivtAapVedtak(ident: String): Boolean =
        with(ytelseService.harAktivtAapVedtak(ident)) {
            return type == TypeYtelsePeriode.AAP && harAktivtVedtak
        }
}
