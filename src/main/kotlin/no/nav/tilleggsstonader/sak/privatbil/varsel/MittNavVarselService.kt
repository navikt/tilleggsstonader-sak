package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.util.erFørNåværendeUke
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.splitPerUkeMedHelg
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MittNavVarselService(
    private val kjørelisteService: KjørelisteService,
    private val vedtakService: VedtakService,
) {
    fun skalSendeKjørelisteVarsel(behandling: Saksbehandling): Boolean {
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling)) return false

        val rammevedtak = finnRammevedtakPrivatBil(behandling) ?: return false
        val innsendtKjørelisteMap = finnInnsendtKjørelisteMap(behandling)

        return rammevedtak.reiser
            .any { reise ->
                val ikkeInnsendteUker =
                    finnRammevedtakUker(reise) - finnInnsendteKjørelisteUker(reise, innsendtKjørelisteMap)
                ikkeInnsendteUker.any { it.erFørNåværendeUke() }
            }
    }

    fun skalSendeKjørelisteForNesteUke(behandling: Saksbehandling): Boolean {
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling)) return false

        val rammevedtak = finnRammevedtakPrivatBil(behandling) ?: return false
        val innsendtKjørelisteMap = finnInnsendtKjørelisteMap(behandling)

        return rammevedtak.reiser
            .any { reise ->
                (finnRammevedtakUker(reise) - finnInnsendteKjørelisteUker(reise, innsendtKjørelisteMap)).isNotEmpty()
            }
    }

    private fun erBehandlingInnvilgelseEllerOpphørDagligReise(behandling: Saksbehandling) =
        behandling.stønadstype.gjelderDagligReise() &&
            (behandling.resultat == BehandlingResultat.INNVILGET || behandling.resultat == BehandlingResultat.OPPHØRT)

    private fun finnRammevedtakPrivatBil(behandling: Saksbehandling): RammevedtakPrivatBil? =
        vedtakService
            .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandling.id)
            .data.rammevedtakPrivatBil

    private fun finnInnsendtKjørelisteMap(behandling: Saksbehandling): Map<UUID, Kjøreliste> {
        val innsendtKjøreliste = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        return innsendtKjøreliste.associateBy { it.data.reiseId.id }
    }

    private fun finnRammevedtakUker(reise: RammeForReiseMedPrivatBil) =
        Datoperiode(reise.grunnlag.fom, reise.grunnlag.tom).splitPerUkeMedHelg().map { it.fom.tilUkeIÅr() }.toSet()

    private fun finnInnsendteKjørelisteUker(
        reise: RammeForReiseMedPrivatBil,
        innsendtKjørelisteMap: Map<UUID, Kjøreliste>,
    ) = innsendtKjørelisteMap[reise.reiseId.id]
        ?.data
        ?.reisedager
        ?.map { it.dato.tilUkeIÅr() }
        ?.toSet() ?: emptySet()
}
