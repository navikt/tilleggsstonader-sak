package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.Executors

@RestController
@RequestMapping("/admin/vedtak/migrer")
@Unprotected
class VedtaksperiodeMigreringsController(
    val vedtakRepository: VedtakRepository,
    val unleashService: UnleashService,
    private val transactionHandler: TransactionHandler,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @GetMapping()
    fun migrerINyTråd(): String {
        logger.info("Vi traff admin/vedtak/migrer")

        if (!unleashService.isEnabled(Toggle.KAN_BRUKE_VEDTAKSPERIODER_TILSYN_BARN)) {
            logger.info("Vedtaksperioder for tilsyn barn er skrudd av")
            return "Unleash-toggelen er avskrudd"
        }

        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        logger.info("Starter ny tråd for migrering")
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                logger.info("Starter transaksjon")
                transactionHandler.runInNewTransaction {
                    logger.info("Kaller på migreringsfunksjonen")
                    migrer()
                    logger.info("Migrering ferdig")
                }
            } catch (e: Exception) {
                secureLogger.error("Feilet jobb", e)
            } finally {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            }
        }

        return "Unleash-toggelen er påskrudd"
    }

    private fun migrer() {
        logger.info("Starter migrering av vedtaksperioder for innvilgelser...")
        hentInnvilgelser().forEach {
            if (it.data.vedtaksperioder == null) {
                val beregningsresultat = it.data.beregningsresultat
                val vedtaksperioder = mapTilVedtaksperiode(beregningsresultat.perioder)

                val nyttVedtak = it.copy(data = it.data.copy(vedtaksperioder = vedtaksperioder))

                vedtakRepository.update(nyttVedtak)
            }
        }

        logger.info("Starter migrering av vedtaksperioder for opphør...")
        hentOpphør().forEach {
            if (it.data.vedtaksperioder == null) {
                val beregningsresultat = it.data.beregningsresultat
                val vedtaksperioder = mapTilVedtaksperiode(beregningsresultat.perioder)

                val nyttVedtak = it.copy(data = it.data.copy(vedtaksperioder = vedtaksperioder))

                vedtakRepository.update(nyttVedtak)
            }
        }

        logger.info("... migrering av vedtaksperioder er ferdig! \uD83E\uDD73")
    }

    private fun hentInnvilgelser(): List<GeneriskVedtak<InnvilgelseTilsynBarn>> {
        val innvilgelseTilsynBarn =
            vedtakRepository
                .findAll()
                .filter { it.data is InnvilgelseTilsynBarn } as List<GeneriskVedtak<InnvilgelseTilsynBarn>>
        return innvilgelseTilsynBarn
    }

    private fun hentOpphør(): List<GeneriskVedtak<OpphørTilsynBarn>> {
        val innvilgelseTilsynBarn =
            vedtakRepository
                .findAll()
                .filter { it.data is OpphørTilsynBarn } as List<GeneriskVedtak<OpphørTilsynBarn>>
        return innvilgelseTilsynBarn
    }
}

fun Vedtaksperiode.erLikOgPåfølgesAv(other: Vedtaksperiode): Boolean {
    val erLik =
        this.aktivitet == other.aktivitet &&
            this.målgruppe == other.målgruppe
    return erLik && this.påfølgesAv(other)
}

fun mapTilVedtaksperiode(beregningsresultat: List<BeregningsresultatForMåned>): List<Vedtaksperiode> =
    beregningsresultat
        .flatMap { tilVedtaksperioder(it) }
        .sorted()
        .mergeSammenhengende(
            skalMerges = { s1, s2 -> s1.erLikOgPåfølgesAv(s2) },
            merge = { s1: Vedtaksperiode, s2: Vedtaksperiode ->
                s1.copy(
                    fom = minOf(s1.fom, s2.fom),
                    tom = maxOf(s1.tom, s2.tom),
                )
            },
        )

private fun tilVedtaksperioder(beregningsresultat: BeregningsresultatForMåned) =
    beregningsresultat.grunnlag.vedtaksperiodeGrunnlag
        .map { it.vedtaksperiode }
        .map { vedtaksperiode ->
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom,
                målgruppe = vedtaksperiode.målgruppe,
                aktivitet = vedtaksperiode.aktivitet,
            )
        }
