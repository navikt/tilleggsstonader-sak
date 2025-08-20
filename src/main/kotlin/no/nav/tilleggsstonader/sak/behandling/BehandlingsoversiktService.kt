package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDetaljer
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingsoversiktDto
import no.nav.tilleggsstonader.sak.behandling.dto.FagsakMedBehandlinger
import no.nav.tilleggsstonader.sak.behandling.dto.Vedtaksperiode
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingsoversiktService(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakService: VedtakService,
) {
    fun hentOversikt(fagsakPersonId: FagsakPersonId): BehandlingsoversiktDto {
        val fagsak = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return BehandlingsoversiktDto(
            fagsakPersonId = fagsakPersonId,
            tilsynBarn = hentFagsakMedBehandlinger(fagsak.barnetilsyn),
            læremidler = hentFagsakMedBehandlinger(fagsak.læremidler),
            boutgifter = hentFagsakMedBehandlinger(fagsak.boutgifter),
            dagligReiseTso = hentFagsakMedBehandlinger(fagsak.dagligReiseTso),
            dagligReiseTsr = hentFagsakMedBehandlinger(fagsak.dagligReiseTsr),
        )
    }

    private fun hentFagsakMedBehandlinger(fagsak: Fagsak?): FagsakMedBehandlinger? {
        if (fagsak == null) return null
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        return FagsakMedBehandlinger(
            fagsakId = fagsak.id,
            eksternFagsakId = fagsak.eksternId.id,
            stønadstype = fagsak.stønadstype,
            erLøpende = fagsakService.erLøpende(fagsak.id),
            behandlinger =
                behandlinger.map {
                    val vedtak = vedtakService.hentVedtak(behandlingId = it.id)

                    BehandlingDetaljer(
                        id = it.id,
                        forrigeIverksatteBehandlingId = it.forrigeIverksatteBehandlingId,
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
                        henlagtBegrunnelse = it.henlagtBegrunnelse,
                        vedtaksperiode =
                            if (it.resultat != BehandlingResultat.HENLAGT) {
                                vedtak?.let { v -> slåSammenVedtaksperioder(v) }
                            } else {
                                null
                            },
                        opphørsdato = vedtak?.takeIfType<Opphør>()?.opphørsdato,
                    )
                },
        )
    }

    /**
     * Slår sammen alle vedtaksperioder som finnes i en behandling slik at oversikten kun viser en periode.
     * Hvis det er innvilget flere vedtaksperioder med mellomrom i samme behandling, vil disse vises
     * som en sammenhengende periode, med en fom = første fom-dato og tom = siste tom-dato.
     */
    private fun slåSammenVedtaksperioder(vedtak: Vedtak): Vedtaksperiode {
        val vedtaksperioder = vedtak.vedtaksperioderHvisFinnes()?.avkortPerioderFør(vedtak.tidligsteEndring) ?: emptyList()
        val minFom = vedtaksperioder.minOfOrNull { it.fom }
        val maksTom = vedtaksperioder.maxOfOrNull { it.tom }

        return Vedtaksperiode(fom = minFom, tom = maksTom)
    }
}
