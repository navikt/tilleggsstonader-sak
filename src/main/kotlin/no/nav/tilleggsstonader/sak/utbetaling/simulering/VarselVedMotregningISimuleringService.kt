package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.forrigeVirkedag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VarselVedMotregningISimuleringService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagEvtVarselForUtbetalingerPåFagsakerISammeFagområde(behandlingId: BehandlingId): String? {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        feilHvis(fagsak.utbetalPåNyttFagområde == null) {
            "Forventer at utbetalPåNyttFagområde skal være satt på fagsaken"
        }
        val alleFagsaker =
            fagsakService.finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId)

        val skalVarsleOmNyligeUtbetalingerInnenforSammeFagområde =
            if (fagsak.utbetalPåNyttFagområde) {
                finnesFagsakMedIverksatteAndelerInnenforPeriode(
                    fagsaker = alleFagsaker.alleFagsakerAvStønadstypeUavhengigAvTema(fagsak.stønadstype),
                    periode = Datoperiode(LocalDate.now(), LocalDate.now()),
                )
            } else {
                finnesFagsakMedIverksatteAndelerInnenforPeriode(
                    fagsaker = alleFagsaker.alleFagsakerMedUtbetalingPåGammeltFagområde(),
                    periode = Datoperiode(LocalDate.now().forrigeVirkedag(), LocalDate.now()),
                )
            }

        return if (skalVarsleOmNyligeUtbetalingerInnenforSammeFagområde) {
            "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
        } else {
            null
        }
    }

    private fun finnesFagsakMedIverksatteAndelerInnenforPeriode(
        fagsaker: List<Fagsak>,
        periode: Datoperiode,
    ): Boolean {
        return fagsaker.any { relevantFagsak ->
            val behandlingId =
                behandlingService
                    .finnSisteIverksatteBehandling(relevantFagsak.id)
                    ?.id ?: return@any false

            val tilkjentYtelse =
                tilkjentYtelseService.hentForBehandling(behandlingId)

            tilkjentYtelse.andelerTilkjentYtelse.any { andel ->
                val iverksettingDato =
                    andel.iverksetting?.iverksettingTidspunkt?.toLocalDate()

                iverksettingDato != null && periode.inneholder(iverksettingDato)
            }
        }
    }
}
