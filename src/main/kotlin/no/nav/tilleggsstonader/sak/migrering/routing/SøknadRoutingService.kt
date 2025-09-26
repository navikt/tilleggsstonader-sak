package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.UnleashUtil.getVariantWithNameOrDefault
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.HarAktivtVedtakDto
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
    fun sjekkRoutingForPerson(routingContext: RoutingContext): SøknadRoutingResponse {
        val skalBehandlesINyLøsning = skalBehandlesINyLøsning(routingContext)
        logger.info(
            "routing - " +
                "stønadstype=${routingContext.søknadstype} " +
                "skalBehandlesINyLøsning=$skalBehandlesINyLøsning",
        )
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning)
    }

    fun harLagretRouting(
        ident: String,
        søknadstype: Søknadstype,
    ): Boolean {
        // TODO: Trenger vi å strukture dette litt annerledes slik at vi slipper strategi.ident osv?🤔
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(ident, søknadstype)
        return søknadRouting != null
    }

    private fun skalBehandlesINyLøsning(context: RoutingContext): Boolean {
        if (harLagretRouting(context.ident, context.søknadstype)) {
            logger.info("routing - søknadstype=${context.søknadstype} harLagretRouting=true")
            return true
        }

        when (context) { // TODO: Dette er kode som burde ligge inni hver strategi i stedet for ?🤔
            is SkalRouteAlleSøkereTilNyLøsning -> {
                logger.info("routing - søknadstype=${context.søknadstype} skalRuteAlleSøkere=true")
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
        val arenaStatuser =
            context.søknadstype.tilStønadstyper().map {
                arenaService.hentStatus(context.ident, it)
            }
        if (harAktivtVedtakIArena(context.søknadstype, arenaStatuser).any { it }) {
            return false
        }
        if (context.søknadstype == Søknadstype.DAGLIG_REISE) {
            if (harAktivtAapVedtak(ytelseService.harAktivtAapVedtak(context.ident))) {
                lagreRouting(context, arenaStatuser)
                return true
            } else {
                return false
            }
        }

        lagreRouting(context, arenaStatuser)
        return true
    }

    private fun maksAntallErNådd(contekst: SkalRouteEnkelteSøkereTilNyLøsning): Boolean {
        val maksAntall = unleashService.getVariantWithNameOrDefault(contekst.toggleId, "antall", 0)
        val antall = søknadRoutingRepository.countByType(contekst.søknadstype)
        logger.info("routing - stønadstype=${contekst.søknadstype} antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun harAktivtVedtakIArena(
        søknadstype: Søknadstype,
        arenaStatuser: Collection<ArenaStatusDto>,
    ): List<Boolean> =
        arenaStatuser.map { arenaStatus ->
            arenaStatus.vedtak.harAktivtVedtak
                .also { loggArenaStatus(arenaStatus, søknadstype, it) }
        }

    private fun loggArenaStatus(
        arenaStatus: ArenaStatusDto,
        søknadstype: Søknadstype,
        harGyldigStatus: Boolean,
    ) {
        val harAktivtVedtak = arenaStatus.vedtak.harAktivtVedtak
        val harVedtakUtenUtfall = arenaStatus.vedtak.harVedtakUtenUtfall
        val harVedtak = arenaStatus.vedtak.harVedtak
        val harAktivSakUtenVedtak = arenaStatus.sak.harAktivSakUtenVedtak

        logger.info(
            "routing - søknadstype=$søknadstype harGyldigStatusArena=$harGyldigStatus - " +
                "harAktivSakUtenVedtak=$harAktivSakUtenVedtak " +
                "harVedtak=$harVedtak " +
                "harAktivtVedtak=$harAktivtVedtak " +
                "harVedtakUtenUtfall=$harVedtakUtenUtfall " +
                "vedtakTom=${arenaStatus.vedtak.vedtakTom}",
        )
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

    // Nå blir brukere som har en behandlig på daglig reise (enten det er TSO eller TSR) rutet til ny søknad
    private fun harBehandling(context: RoutingContext): Boolean {
        val harBehandling =
            context.søknadstype
                .tilStønadstyper()
                .mapNotNull { fagsakService.finnFagsak(personIdenter = setOf(context.ident), stønadstype = it) }
                .map { behandlingService.hentBehandlinger(fagsakId = it.id) }
                .isNotEmpty()

        logger.info("routing - stønadstype=${context.søknadstype} harBehandling=$harBehandling")
        return harBehandling
    }

    private fun harAktivtAapVedtak(vedtakStatus: HarAktivtVedtakDto): Boolean =
        vedtakStatus.type == TypeYtelsePeriode.AAP && vedtakStatus.harAktivtVedtak
}
