package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AdressebeskyttelseDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingÅrsakDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StønadstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtakResultatDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import org.springframework.stereotype.Service

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepositoryV2: VedtaksstatistikkRepositoryV2,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val iverksettService: IverksettService,
    private val vedtakService: VedtakService,
    private val barnRepository: BarnRepository,
) {
    fun lagreVedtaksstatistikkV2(
        behandlingId: BehandlingId,
        fagsakId: FagsakId,
    ) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtak =
            vedtakService.hentVedtak(behandlingId)
                ?: throw IllegalStateException("Kan ikke sende vedtaksstatistikk uten vedtak")
        val vedtakstidspunkt =
            behandling.vedtakstidspunkt
                ?: throw IllegalStateException("Behandlingen må ha et vedtakstidspunkt for å sende vedtaksstatistikk")
        val søkerIdent = behandlingService.hentAktivIdent(behandlingId)
        val andelTilkjentYtelse = iverksettService.hentAndelTilkjentYtelse(behandlingId)
        val barn = barnRepository.findByBehandlingId(behandlingId)

        vedtaksstatistikkRepositoryV2.insert(
            VedtaksstatistikkV2(
                fagsakId = fagsakId,
                stønadstype = StønadstypeDvh.fraDomene(behandling.stønadstype),
                behandlingId = behandlingId,
                eksternFagsakId = behandling.eksternFagsakId,
                eksternBehandlingId = behandling.eksternId,
                relatertBehandlingId = hentRelatertBehandlingId(behandling),
                adressebeskyttelse = hentAdressebeskyttelse(søkerIdent),
                tidspunktVedtak = vedtakstidspunkt,
                søkerIdent = søkerIdent,
                behandlingType = BehandlingTypeDvh.fraDomene(behandling.type),
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandling.årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandling.resultat),
                vedtaksperioder = VedtaksperioderDvh.fraDomene(vedtak, barn),
                utbetalinger = UtbetalingerDvh.fraDomene(andelTilkjentYtelse, vedtak),
                årsakerAvslag = ÅrsakAvslagDvh.fraDomene(vedtak.takeIfType<Avslag>()?.data?.årsaker),
                årsakerOpphør = ÅrsakOpphørDvh.fraDomene(vedtak.takeIfType<Opphør>()?.data?.årsaker),
            ),
        )
    }

    private fun hentAdressebeskyttelse(personIdent: String) =
        AdressebeskyttelseDvh.fraDomene(
            personService
                .hentPersonKortBolk(
                    listOf(personIdent),
                ).values
                .single()
                .adressebeskyttelse
                .gradering(),
        )

    private fun hentRelatertBehandlingId(behandling: Saksbehandling) =
        behandling.forrigeIverksatteBehandlingId
            ?.let {
                behandlingService.hentEksternBehandlingId(it)
            }?.id
}
