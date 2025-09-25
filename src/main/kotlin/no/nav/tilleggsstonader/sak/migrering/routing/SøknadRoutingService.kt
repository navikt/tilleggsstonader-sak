package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
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
                "stønadstype=${context.søknadstype} " +
                "skalBehandlesINyLøsning=$skalBehandlesINyLøsning",
        )
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning)
    }

    fun harLagretRouting(
        ident: String,
        stønadstypeRouting: Søknadstype,
    ): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(ident, stønadstypeRouting)
        return søknadRouting != null
    }

    private fun skalBehandlesINyLøsning(context: RoutingContext): Boolean {
        if (harLagretRouting(context.ident, context.søknadstype)) {
            logger.info("routing - stønadstype=${context.søknadstype} harLagretRouting=true")
            return true
        }

        when (context) {
            is SkalRouteAlleSøkereTilNyLøsning -> {
                logger.info("routing - stønadstype=${context.søknadstype} skalRuteAlleSøkere=true")
                lagreRouting(context, mapOf("ruterAlleSøkere" to true))
                return true
            }

            is SkalRouteEnkelteSøkereTilNyLøsning -> {
                return vurderRoutingTilNyLøsning(context)
            }
        }
    }

    private fun vurderRoutingTilNyLøsning(context: SkalRouteEnkelteSøkereTilNyLøsning): Boolean {
        if (harBehandling(context)) {
            lagreRouting(context, mapOf("harBehandling" to true))
            return true
        }
        if (maksAntallErNådd(context)) {
            return false
        }
        val arenaStatus = arenaService.hentStatus(context.ident, context.søknadstype.tilStønadstyper().first())
        val målgruppeAAP = arenaService.hentVedtak(FagsakPersonId.fromString(context.ident))

        if (harGyldigStateIArena(context, arenaStatus)) {
            lagreRouting(context, arenaStatus)
            return true
        }
        return false
    }

    private fun maksAntallErNådd(context: SkalRouteEnkelteSøkereTilNyLøsning): Boolean {
        val maksAntall = unleashService.getVariantWithNameOrDefault(context.toggleId, "antall", 0)
        val antall = søknadRoutingRepository.countByType(context.søknadstype)
        logger.info("routing - stønadstype=${context.søknadstype} antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun harGyldigStateIArena(
        context: SkalRouteEnkelteSøkereTilNyLøsning,
        arenaStatus: ArenaStatusDto,
    ): Boolean {
        val harAktivtVedtak = arenaStatus.vedtak.harAktivtVedtak
        val harVedtakUtenUtfall = arenaStatus.vedtak.harVedtakUtenUtfall
        val harVedtak = arenaStatus.vedtak.harVedtak
        val harAktivSakUtenVedtak = arenaStatus.sak.harAktivSakUtenVedtak

        val søknadsType = context.søknadstype
        val harGyldigStatus = context.harGyldigStateIArena(arenaStatus)

        logger.info(
            "routing - søknadsType=$søknadsType harGyldigStatusArena=$harGyldigStatus - " +
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
                type = context.søknadstype,
                detaljer = JsonWrapper(objectMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun harBehandling(context: RoutingContext): Boolean {
        val harBehandling =
            context.søknadstype
                .tilStønadstyper()
                .map { it ->
                    fagsakService.finnFagsak(setOf(context.ident), it)
                }.isNotEmpty()

        logger.info("routing - stønadstype=${context.søknadstype} harBehandling=$harBehandling")
        return harBehandling
    }
}
