package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
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
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import org.springframework.stereotype.Service

@Service
class SkjemaRoutingService(
    private val routingRepository: SkjemaRoutingRepository,
    private val fagsakService: FagsakService,
    private val arenaService: ArenaService,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
    private val ytelseService: YtelseService,
    private val personService: PersonService,
) {
    fun harLagretRouting(
        ident: String,
        skjematype: Skjematype,
    ) = routingRepository.findByIdentAndType(ident, skjematype) != null

    fun skalRoutesTilNyLøsning(
        ident: String,
        skjematype: Skjematype,
    ): Boolean {
        val routingStrategi = bestemRoutingStrategi(skjematype)

        return when (routingStrategi) {
            RoutingStrategi.SendAlleBrukereTilNyLøsning -> {
                true
            }

            is RoutingStrategi.SendEnkelteBrukereTilNyLøsning -> {
                skalBrukerRoutesTilNyLøsning(ident, skjematype, routingStrategi)
            }
        }.also { loggRoutingResultatet(skjematype, it) }
    }

    private fun skalBrukerRoutesTilNyLøsning(
        ident: String,
        skjematype: Skjematype,
        kontekst: RoutingStrategi.SendEnkelteBrukereTilNyLøsning,
    ): Boolean {
        if (kontekst.kreverUgradertAdresse && harFortroligEllerStrengtFortroligAdresse(ident)) {
            return false
        }
        if (harLagretRouting(ident, skjematype)) {
            logger.info("routing - skjematype=$skjematype harLagretRouting=true")
            return true
        }
        if (harBehandling(ident, skjematype)) {
            lagreRouting(ident, skjematype, mapOf("harBehandling" to true))
            return true
        }
        if (maksAntallErNådd(skjematype, toggleId = kontekst.featureToggleMaksAntall)) {
            return false
        }
        if (kontekst.kreverAtSøkerErUtenAktivtVedtakIArena && harAktivtVedtakIArena(skjematype, ident)) {
            return false
        }
        if (kontekst.kreverAktivtAapVedtak && harAktivtAapVedtak(ident)) {
            lagreRouting(ident, skjematype, mapOf("harAktivAAP" to true))
            return true
        }

        return false
    }

    private fun harFortroligEllerStrengtFortroligAdresse(ident: String): Boolean =
        personService.hentAdressebeskyttelse(ident).søker.adressebeskyttelse.let { adressebeskyttelse ->
            adressebeskyttelse == AdressebeskyttelseGradering.FORTROLIG ||
                adressebeskyttelse == AdressebeskyttelseGradering.STRENGT_FORTROLIG
        }

    private fun maksAntallErNådd(
        skjematype: Skjematype,
        toggleId: ToggleId,
    ): Boolean {
        val maksAntall = unleashService.getVariantWithNameOrDefault(toggleId, "antall", 0)
        val antall = routingRepository.countByType(skjematype)
        logger.info("routing - stønadstype=$skjematype antallIDatabase=$antall toggleMaksAntall=$maksAntall")
        return antall >= maksAntall
    }

    private fun harAktivtVedtakIArena(
        skjematype: Skjematype,
        ident: String,
    ): Boolean =
        skjematype.tilStønadstyper().any { stønadstype ->
            val arenaStatus = arenaService.hentStatus(ident, stønadstype)
            return arenaStatus.vedtak.harAktivtVedtak
                .also { loggArenaStatus(arenaStatus, skjematype, it) }
        }

    private fun lagreRouting(
        ident: String,
        skjematype: Skjematype,
        detaljer: Any,
    ) {
        routingRepository.insert(
            SkjemaRouting(
                ident = ident,
                type = skjematype,
                detaljer = JsonWrapper(jsonMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun loggRoutingResultatet(
        skjematype: Skjematype,
        skalBehandlesINyLøsning: Boolean,
    ) {
        logger.info(
            "routing - " +
                "stønadstype=$skjematype " +
                "skalBehandlesINyLøsning=$skalBehandlesINyLøsning",
        )
    }

    private fun harBehandling(
        ident: String,
        skjematype: Skjematype,
    ): Boolean {
        val harBehandling =
            skjematype
                .tilStønadstyper()
                .mapNotNull { fagsakService.finnFagsak(personIdenter = setOf(ident), stønadstype = it) }
                .map { behandlingService.hentBehandlinger(fagsakId = it.id) }
                .isNotEmpty()

        logger.info("routing - skjematype=$skjematype harBehandling=$harBehandling")
        return harBehandling
    }

    private fun loggArenaStatus(
        arenaStatus: ArenaStatusDto,
        skjematype: Skjematype,
        harGyldigStatus: Boolean,
    ) {
        with(arenaStatus) {
            logger.info(
                "routing - skjematype=$skjematype harGyldigStatusArena=$harGyldigStatus " +
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
