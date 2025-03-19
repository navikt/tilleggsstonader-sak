package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
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

    fun sjekkRoutingForPerson(
        request: IdentStønadstype,
        sjekkSkalRuteAlleSøkere: Boolean = true,
    ): SøknadRoutingResponse {
        val skalBehandlesINyLøsning = skalBehandlesINyLøsning(request, sjekkSkalRuteAlleSøkere)
        logger.info("routing - stønadstype=${request.stønadstype} skalBehandlesINyLøsning=$skalBehandlesINyLøsning")
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning)
    }

    fun harLagretRouting(request: IdentStønadstype): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(request.ident, request.stønadstype)
        return søknadRouting != null
    }

    private fun skalBehandlesINyLøsning(
        request: IdentStønadstype,
        sjekkSkalRuteAlleSøkere: Boolean,
    ): Boolean {
        if (harLagretRouting(request)) {
            logger.info("routing - stønadstype=${request.stønadstype} harLagretRouting=true")
            return true
        }

        if (sjekkSkalRuteAlleSøkere && skalRuteAlleSøkereTilNyLøsning(request.stønadstype)) {
            lagreRouting(request, mapOf("ruterAlleSøkere" to true))
            return true
        }

        if (harBehandling(request)) {
            lagreRouting(request, mapOf("harBehandling" to true))
            return true
        }

        if (skalStoppesPgaFeatureToggle(request.stønadstype)) {
            return false
        }

        val arenaStatus = arenaService.hentStatus(request.ident, request.stønadstype)
        if (harGyldigStateIArena(request.stønadstype, arenaStatus)) {
            lagreRouting(request, arenaStatus)
            return true
        }
        return false
    }

    /**
     * Ønsker å sette at alle skal rutes til ny løsning, uten å sjekke status i Arena då det tar unødvendig lang tid
     */
    private fun skalRuteAlleSøkereTilNyLøsning(stønadstype: Stønadstype): Boolean {
        val skalRutes =
            when (stønadstype) {
                Stønadstype.BARNETILSYN -> true
                Stønadstype.LÆREMIDLER -> true
                Stønadstype.BOUTGIFTER -> false
            }
        logger.info("routing - stønadstype=$stønadstype skalRuteAlleSøkere=true")
        return skalRutes
    }

    private fun skalStoppesPgaFeatureToggle(stønadstype: Stønadstype): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> false // tilsyn barn skal ikke stoppes med feature toggle
            Stønadstype.LÆREMIDLER -> maksAntallErNådd(stønadstype)
            Stønadstype.BOUTGIFTER -> maksAntallErNådd(stønadstype)
        }

    private fun maksAntallErNådd(stønadstype: Stønadstype): Boolean {
        val maksAntall = maksAntall(stønadstype)
        val antall = søknadRoutingRepository.countByType(stønadstype)
        logger.info("routing - stønadstype=$stønadstype antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun maksAntall(stønadstype: Stønadstype) =
        unleashService.getVariantWithNameOrDefault(stønadstype.maksAntallToggle(), "antall", 0)

    private fun Stønadstype.maksAntallToggle() =
        when (this) {
            Stønadstype.LÆREMIDLER -> Toggle.SØKNAD_ROUTING_LÆREMIDLER
            Stønadstype.BOUTGIFTER -> Toggle.SØKNAD_ROUTING_BOUTGIFTER
            else -> error("Har ikke maksAntalLToggle for stønadstype=$this")
        }

    private fun harGyldigStateIArena(
        stønadstype: Stønadstype,
        arenaStatus: ArenaStatusDto,
    ): Boolean {
        val harAktivtVedtak = arenaStatus.vedtak.harAktivtVedtak
        val harVedtakUtenUtfall = arenaStatus.vedtak.harVedtakUtenUtfall
        val harVedtak = arenaStatus.vedtak.harVedtak
        val harAktivSakUtenVedtak = arenaStatus.sak.harAktivSakUtenVedtak

        val harGyldigStatus =
            when (stønadstype) {
                Stønadstype.BARNETILSYN -> !harAktivtVedtak
                Stønadstype.LÆREMIDLER -> !harAktivtVedtak
                Stønadstype.BOUTGIFTER -> !harAktivtVedtak
            }

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
        request: IdentStønadstype,
        detaljer: Any,
    ) {
        søknadRoutingRepository.insert(
            SøknadRouting(
                ident = request.ident,
                type = request.stønadstype,
                detaljer = JsonWrapper(objectMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun harBehandling(request: IdentStønadstype): Boolean {
        val harBehandling = (
            fagsakService
                .finnFagsak(setOf(request.ident), request.stønadstype)
                ?.let { behandlingService.hentBehandlinger(it.id).isNotEmpty() }
                ?: false
        )
        logger.info("routing - stønadstype=${request.stønadstype} harBehandling=$harBehandling")
        return harBehandling
    }
}
