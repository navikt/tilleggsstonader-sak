package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.UnleashUtil.getVariantWithNameOrDefault
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SøknadRoutingService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val arenaService: ArenaService,
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sjekkRoutingForPerson(context: RoutingContext): SøknadRoutingResponse {
        val skalBehandlesINyLøsning = skalBehandlesINyLøsning(context)
        logger.info(
            "routing - " +
                "stønadstype=${context.stønadstype} " +
                "skalBehandlesINyLøsning=$skalBehandlesINyLøsning",
        )
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning)
    }

    fun harLagretRouting(
        ident: String,
        stønadstype: Stønadstype,
    ): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(ident, stønadstype)
        return søknadRouting != null
    }

    private fun skalBehandlesINyLøsning(context: RoutingContext): Boolean {
        if (harLagretRouting(context.ident, context.stønadstype)) {
            logger.info("routing - stønadstype=${context.stønadstype} harLagretRouting=true")
            return true
        }

        when (context) {
            is SkalRouteAlleSøkereTilNyLøsning -> {
                logger.info("routing - stønadstype=${context.stønadstype} skalRuteAlleSøkere=true")
                lagreRouting(context, mapOf("ruterAlleSøkere" to true))
                return true
            }
            is FeatureTogglet -> {
                return kontrollerFeatureToggle(context)
            }
        }
    }

    private fun kontrollerFeatureToggle(context: FeatureTogglet): Boolean {
        if (harBehandling(context)) {
            lagreRouting(context, mapOf("harBehandling" to true))
            return true
        }

        if (maksAntallErNådd(context)) {
            return false
        }
        val arenaStatus = arenaService.hentStatus(context.ident, context.stønadstype)
        if (harGyldigStateIArena(context, arenaStatus)) {
            lagreRouting(context, arenaStatus)
            return true
        }
        return false
    }

    private fun maksAntallErNådd(context: FeatureTogglet): Boolean {
        val maksAntall = unleashService.getVariantWithNameOrDefault(context.toggleId, "antall", 0)
        val antall = søknadRoutingRepository.countByType(context.stønadstype)
        logger.info("routing - stønadstype=${context.stønadstype} antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun harGyldigStateIArena(
        context: FeatureTogglet,
        arenaStatus: ArenaStatusDto,
    ): Boolean {
        val harAktivtVedtak = arenaStatus.vedtak.harAktivtVedtak
        val harVedtakUtenUtfall = arenaStatus.vedtak.harVedtakUtenUtfall
        val harVedtak = arenaStatus.vedtak.harVedtak
        val harAktivSakUtenVedtak = arenaStatus.sak.harAktivSakUtenVedtak

        val stønadstype = context.stønadstype
        val harGyldigStatus = context.harGyldigStateIArena(arenaStatus)

        logger.info(
            "routing - stønadstype=$stønadstype harGyldigStatusArena=$harGyldigStatus - " +
                "harAktivSakUtenVedtak=$harAktivSakUtenVedtak " +
                "harVedtak=$harVedtak " +
                "harAktivtVedtak=$harAktivtVedtak " +
                "harVedtakUtenUtfall=$harVedtakUtenUtfall " +
                "vedtakTom=${arenaStatus.vedtak.vedtakTom}",
        )
        return harGyldigStatus
    }

    private fun lagreRouting(
        context: RoutingContext,
        detaljer: Any,
    ) {
        søknadRoutingRepository.insert(
            SøknadRouting(
                ident = context.ident,
                type = context.stønadstype,
                detaljer = JsonWrapper(objectMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun harBehandling(context: RoutingContext): Boolean {
        val harBehandling = (
            fagsakService
                .finnFagsak(setOf(context.ident), context.stønadstype)
                ?.let { behandlingService.hentBehandlinger(it.id).isNotEmpty() }
                ?: false
        )
        logger.info("routing - stønadstype=${context.stønadstype} harBehandling=$harBehandling")
        return harBehandling
    }
}
