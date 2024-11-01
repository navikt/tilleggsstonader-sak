package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagerBrevRepository
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NullstillBehandlingService(
    private val behandlingService: BehandlingService,
    private val gjennbrukDataRevurderingService: GjennbrukDataRevurderingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårRepository: VilkårRepository,
    private val tilsynBarnVedtakRepository: TilsynBarnVedtakRepository,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val vedtaksbrevRepository: VedtaksbrevRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun nullstillBehandling(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandling er låst for videre redigering og kan ikke nullstilles"
        }
        logger.info("Nullstiller behandling=$behandlingId")

        behandlingService.oppdaterStegPåBehandling(behandlingId, StegType.INNGANGSVILKÅR)

        slettDataIBehandling(behandlingId)

        gjenbrukData(behandling)
    }

    /**
     * Nullstiller ikke barn, då barn gjenbrukes og har ev. med nye fra ny søknad
     */
    private fun slettDataIBehandling(behandlingId: BehandlingId) {
        vilkårperiodeRepository.findByBehandlingId(behandlingId).let(vilkårperiodeRepository::deleteAll)
        stønadsperiodeRepository.findAllByBehandlingId(behandlingId).let(stønadsperiodeRepository::deleteAll)
        vilkårRepository.findByBehandlingId(behandlingId).let(vilkårRepository::deleteAll)

        tilsynBarnVedtakRepository.deleteById(behandlingId)
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let(tilkjentYtelseRepository::delete)
        simuleringsresultatRepository.deleteById(behandlingId)
        mellomlagerBrevRepository.deleteById(behandlingId)
        vedtaksbrevRepository.deleteById(behandlingId)
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk = gjennbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling)
            ?: error("Kan ikke nullstille behandling som ikke har behandlingIdForGjenbruk")
        val barnMap = gjennbrukDataRevurderingService.finnNyttIdForBarn(behandling.id, behandlingIdForGjenbruk)
        gjennbrukDataRevurderingService.gjenbrukData(behandling, behandlingIdForGjenbruk, barnMap)
    }
}
