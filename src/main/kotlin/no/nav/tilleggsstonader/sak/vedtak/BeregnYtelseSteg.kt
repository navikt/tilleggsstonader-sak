package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Splitter opp BeregnYtelseSteg for ulike stønadstyper
 * Denne håndterer sletting av tidligere vedtak og andeler
 */
abstract class BeregnYtelseSteg<DTO : Any>(
    private val stønadstype: Stønadstype,
    open val unleashService: UnleashService,
    open val vedtakRepository: VedtakRepository,
    open val tilkjentYtelseService: TilkjentYtelseService,
    open val simuleringService: SimuleringService,
) : BehandlingSteg<DTO> {
    val logger = LoggerFactory.getLogger(javaClass)

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: DTO,
    ) {
        logger.info("Lagrer vedtak for behandling=${saksbehandling.id} vedtak=${data::class.simpleName}")
        validerStønadstype(saksbehandling)
        nullstillEksisterendeVedtakPåBehandling(saksbehandling)
        lagreVedtak(saksbehandling, data)
    }

    protected abstract fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: DTO,
    )

    private fun nullstillEksisterendeVedtakPåBehandling(saksbehandling: Saksbehandling) {
        vedtakRepository.deleteById(saksbehandling.id)
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    private fun validerStønadstype(saksbehandling: Saksbehandling) {
        feilHvisIkke(saksbehandling.stønadstype == stønadstype) {
            "${this::class.java.simpleName} tillater kun $stønadstype"
        }
    }

    protected fun revurderFraEllerOpphørsdato(
        revurderFra: LocalDate?,
        opphørsdato: LocalDate?,
    ) = if (unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) {
        feilHvis(opphørsdato == null) {
            "opphørsdato er påkrevd for opphør"
        }
        opphørsdato
    } else {
        feilHvis(revurderFra == null) {
            "revurderFra-dato er påkrevd for opphør"
        }
        revurderFra
    }

    override fun stegType(): StegType = StegType.BEREGNE_YTELSE
}
