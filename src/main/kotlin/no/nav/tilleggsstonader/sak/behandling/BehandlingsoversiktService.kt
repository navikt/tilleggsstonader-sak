package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDetaljer
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingsoversiktDto
import no.nav.tilleggsstonader.sak.behandling.dto.FagsakMedBehandlinger
import no.nav.tilleggsstonader.sak.behandling.dto.Vedtaksperiode
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.util.max
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingsoversiktService(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
) {

    fun hentOversikt(fagsakPersonId: FagsakPersonId): BehandlingsoversiktDto {
        val fagsak = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return BehandlingsoversiktDto(
            fagsakPersonId = fagsakPersonId,
            tilsynBarn = hentFagsakMedBehandlinger(fagsak.barnetilsyn),
            læremidler = hentFagsakMedBehandlinger(fagsak.læremidler),
        )
    }

    private fun hentFagsakMedBehandlinger(fagsak: Fagsak?): FagsakMedBehandlinger? {
        if (fagsak == null) return null
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        val vedtaksperioder = hentVedtaksperioder(fagsak, behandlinger)

        return FagsakMedBehandlinger(
            fagsakId = fagsak.id,
            eksternFagsakId = fagsak.eksternId.id,
            stønadstype = fagsak.stønadstype,
            erLøpende = fagsakService.erLøpende(fagsak.id),
            behandlinger = behandlinger.map {
                BehandlingDetaljer(
                    id = it.id,
                    forrigeBehandlingId = it.forrigeBehandlingId,
                    fagsakId = it.fagsakId,
                    steg = it.steg,
                    kategori = it.kategori,
                    type = it.type,
                    status = it.status,
                    sistEndret = it.sporbar.endret.endretTid,
                    resultat = it.resultat,
                    opprettet = it.sporbar.opprettetTid,
                    opprettetAv = it.sporbar.opprettetAv,
                    behandlingsårsak = it.årsak,
                    vedtaksdato = it.vedtakstidspunkt,
                    henlagtÅrsak = it.henlagtÅrsak,
                    revurderFra = it.revurderFra,
                    vedtaksperiode = vedtaksperioder[it.id],
                )
            },
        )
    }

    /**
     * Denne henter alle vedtaken for de behandlinger som finnes
     * Ønsket om å vise vedtaksperiode i behandlingsoversikten føles ikke helt landet.
     * Man burde kanskje haft en vedtaksperiode på behandling eller direkt på vedtaket for å enkelt hente ut informasjonen
     */
    private fun hentVedtaksperioder(
        fagsak: Fagsak,
        behandlinger: List<Behandling>,
    ): Map<BehandlingId, Vedtaksperiode?> {
        val revurderFraPåBehandlingId = behandlinger.associate { it.id to it.revurderFra }
        return if (fagsak.stønadstype == Stønadstype.BARNETILSYN) {
            vedtakRepository.findAllById(behandlinger.map { it.id })
                .associateBy { it.behandlingId }
                .mapValues { (behandlingId, vedtak) ->
                    utledVedstaksperiodeForBehandling(vedtak, revurderFraPåBehandlingId[behandlingId])
                }
        } else {
            emptyMap()
        }
    }

    /**
     * Alle stønadsperioder blir lagret ned i en revurdering.
     * Dvs hvis jag revurderer fra 15 april, så lagrer vi ned perioder før det og.
     *
     * Av den grunnen hentes min(it.fom) og bruker max(minFom, revurderFra),
     * då revurderFra egentlige gjøres etter minFom på stønadsperioden
     *
     * I tilfelle man kun har opphørt perioder, så vil revurderFra kunne være etter maksTom,
     * av den grunnen settes tom=max(maksTom, revurderFra)
     */
    private fun utledVedstaksperiodeForBehandling(
        vedtak: Vedtak,
        revurdererFra: LocalDate?,
    ): Vedtaksperiode? {
        val perioder = when (vedtak.data) {
            is InnvilgelseTilsynBarn -> vedtak.data.beregningsresultat
            is OpphørTilsynBarn -> vedtak.data.beregningsresultat
            is AvslagTilsynBarn -> return null
        }.perioder
        val stønadsperioder = perioder.flatMap { it.grunnlag.stønadsperioderGrunnlag }.map { it.stønadsperiode }
        val minFom = stønadsperioder.minOfOrNull { it.fom }
        val maksTom = stønadsperioder.maxOfOrNull { it.tom }
        return Vedtaksperiode(fom = max(minFom, revurdererFra), tom = max(maksTom, revurdererFra))
    }
}
