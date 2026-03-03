package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.splitPerUkeMedHelg
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MittNavVarselService(
    private val kjørelisteService: KjørelisteService,
    private val vedtakRepository: VedtakRepository,
) {
    fun skalSendeKjørelisteVarsel(behandling: Saksbehandling): Boolean {
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling)) return false

        val rammevedtak = hentRammevedtakPrivatBil(behandling) ?: return false
        val innsendtKjørelisteMap = hentInnsendtKjørelisteMap(behandling)

        return rammevedtak.reiser
            .map { reise ->
                val ikkeInsendteUker =
                    hentRammevedtakUker(reise) - hentInnsendteKjørelisteUker(reise, innsendtKjørelisteMap)
                return ikkeInsendteUker.any { it < LocalDate.now().tilUkeIÅr() }
            }.isNotEmpty()
    }

    fun skalSendeKjørelisteForNesteUke(behandling: Saksbehandling): Boolean {
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling)) return false

        val rammevedtak = hentRammevedtakPrivatBil(behandling) ?: return false
        val innsendtKjørelisteMap = hentInnsendtKjørelisteMap(behandling)

        return rammevedtak.reiser
            .map { reise ->
                val ikkeInsendteUker =
                    hentRammevedtakUker(reise) - hentInnsendteKjørelisteUker(reise, innsendtKjørelisteMap)
                return ikkeInsendteUker.any { it > LocalDate.now().tilUkeIÅr() }
            }.isNotEmpty()
    }

    private fun erBehandlingInnvilgelseEllerOpphørDagligReise(behandling: Saksbehandling) =
        behandling.stønadstype.gjelderDagligReise() &&
            (behandling.resultat == BehandlingResultat.INNVILGET || behandling.resultat == BehandlingResultat.OPPHØRT)

    private fun hentRammevedtakPrivatBil(behandling: Saksbehandling): RammevedtakPrivatBil? =
        vedtakRepository
            .findByIdOrThrow(behandling.id)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
            .data.rammevedtakPrivatBil

    private fun hentInnsendtKjørelisteMap(behandling: Saksbehandling): Map<UUID, Kjøreliste> {
        val innsendtKjøreliste = kjørelisteService.hentForFagsakId(behandling.fagsakId)
        return innsendtKjøreliste.associateBy { it.data.reiseId.id }
    }

    private fun hentRammevedtakUker(reise: RammeForReiseMedPrivatBil) =
        Datoperiode(reise.grunnlag.fom, reise.grunnlag.tom).splitPerUkeMedHelg().map { it.fom.tilUkeIÅr() }.toSet()

    private fun hentInnsendteKjørelisteUker(
        reise: RammeForReiseMedPrivatBil,
        innsendtKjørelisteMap: Map<UUID, Kjøreliste>,
    ) = innsendtKjørelisteMap[reise.reiseId.id]
        ?.data
        ?.reisedager
        ?.map { it.dato.tilUkeIÅr() }
        ?.toSet() ?: emptySet()
}
