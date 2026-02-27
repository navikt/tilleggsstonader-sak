package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.splitPerUkeMedHelg
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service

@Service
class MittNavVarselService(
    private val kjørelisteService: KjørelisteService,
    private val vedtakRepository: VedtakRepository,
) {
    fun skalOppretteKjørelisteVarselTask(behandling: Saksbehandling): Boolean {
        if (!behandling.stønadstype.gjelderDagligReise()) {
            return false
        }
        if (behandling.resultat != BehandlingResultat.INNVILGET && behandling.resultat != BehandlingResultat.OPPHØRT) {
            return false
        }

        val rammevedtak =
            vedtakRepository
                .findByIdOrThrow(behandling.id)
                .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
                // Hvis null er det fordi det er offentlig transport
                .data.rammevedtakPrivatBil ?: return false

        val innsendtKjøreliste = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        val innsendtKjørelisteMap = innsendtKjøreliste.associateBy { it.data.reiseId }

        return rammevedtak
            .reiser
            .map { reise ->
                val fom = reise.grunnlag.fom
                val tom = reise.grunnlag.tom
                val periode = Datoperiode(fom, tom).splitPerUkeMedHelg()

                val rammevedtakUker = periode.map { it.fom.ukenummer() }
                val innsendtKjørelisteUker =
                    innsendtKjørelisteMap[reise.reiseId]?.data?.reisedager?.map { it.dato.ukenummer() } ?: emptyList()

                return rammevedtakUker.filter { it !in innsendtKjørelisteUker }.isNotEmpty()
            }.isNotEmpty()
    }
}
