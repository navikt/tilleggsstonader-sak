package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepository: error.NonExistentClass,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val vedtakService: TilsynBarnVedtakService,
    private val vilkårperiodeService: VilkårperiodeService,


    ) {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: ZonedDateTime) {

        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        vedtaksstatistikkRepository.lagreVedtaksstatistikk(
            VedtaksstatistikkDvh(
                fagsakId = fagsakId,
                behandlingId = behandlingId,
                eksternBehandlingId = behandlingService.hentEksternBehandlingId(behandlingId).id,
                relatertBehandlingId = hentRelatertBehandlingId(behandlingId),
                adressebeskyttelse = hentAdressebeskyttelse(personIdent),
                tidspunktVedtak =  hendelseTidspunkt,
                målgruppe = MålgruppeDvh.mapFraVilkårsperioder(vilkårsperioder.målgrupper),
                aktivitet =
            )

    }

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraAdressebeskyttelseGradering(
        personService.hentPersonKortBolk(
            listOf(personIdent)
        ).values.single().adressebeskyttelse.gradering()
    )

    private fun hentRelatertBehandlingId(behandlingId: UUID) =
        behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId?.let {
            behandlingService.hentEksternBehandlingId(it)
        }?.id

}
