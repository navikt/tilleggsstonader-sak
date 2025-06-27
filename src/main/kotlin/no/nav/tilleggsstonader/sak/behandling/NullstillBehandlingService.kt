package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagerBrevRepository
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NullstillBehandlingService(
    private val behandlingService: BehandlingService,
    private val gjennbrukDataRevurderingService: GjennbrukDataRevurderingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
    private val vilkårRepository: VilkårRepository,
    private val vedtakRepository: VedtakRepository,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val vedtaksbrevRepository: VedtaksbrevRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun nullstillBehandling(behandling: Behandling) {
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen kan ikke nullstilles fordi den har status ${behandling.status.visningsnavn()}."
        }
        logger.info("Nullstiller behandling=${behandling.id}")

        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.INNGANGSVILKÅR)

        slettDataIBehandling(behandling.id)

        gjenbrukData(behandling)
    }

    /**
     * Sletting av vilkårperiodegrunnlag gjøres i en egen metode då det alltid skal gjøres når endrer revurder-fra-datoet
     * sånn at man får hentet data fra nytt revurder-fra-dato
     * Gjøres direkte via repository for å ikke lage en publik metode som gjør det mulig å slette grunnlaget i
     */
    @Transactional
    fun slettVilkårperiodegrunnlag(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette vilkårperiodegrunnlag fordi behandlingen har status ${behandling.status.visningsnavn()}."
        }
        logger.info("Sletter vilkårperiodegrunnlag behandling=$behandlingId")

        vilkårperioderGrunnlagRepository.deleteById(behandlingId)
    }

    /**
     * Nullstiller ikke barn, då barn gjenbrukes og har ev. med nye fra ny søknad
     */
    private fun slettDataIBehandling(behandlingId: BehandlingId) {
        vilkårperiodeRepository.findByBehandlingId(behandlingId).let(vilkårperiodeRepository::deleteAll)
        vilkårRepository.findByBehandlingId(behandlingId).let(vilkårRepository::deleteAll)

        vedtakRepository.deleteById(behandlingId)
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)?.let(tilkjentYtelseRepository::delete)
        simuleringsresultatRepository.deleteById(behandlingId)
        mellomlagerBrevRepository.deleteById(behandlingId)
        vedtaksbrevRepository.deleteById(behandlingId)
    }

    private fun gjenbrukData(behandling: Behandling) {
        val behandlingIdForGjenbruk =
            gjennbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling)
                ?: error("Fant ingen behandling å gjenbruke data fra")
        val barnMap = gjennbrukDataRevurderingService.finnNyttIdForBarn(behandling.id, behandlingIdForGjenbruk)
        gjennbrukDataRevurderingService.gjenbrukData(behandling, behandlingIdForGjenbruk, barnMap)
    }
}
