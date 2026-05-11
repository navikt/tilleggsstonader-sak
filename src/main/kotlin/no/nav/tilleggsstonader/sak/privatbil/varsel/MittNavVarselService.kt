package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.stereotype.Service

@Service
class MittNavVarselService(
    private val kjørelisteService: KjørelisteService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
) {
    /**
     * Sjekker om brukeren skal varsles om kjørelister.
     * Brukes fra scheduled jobb som kjører mandag kl 10.
     */
    fun skalSendeKjørelisteVarselForForrigeUke(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling) || !erGjeldendeIverksatteBehandling(behandling)) return false

        val forrigeUke = UkeIÅr.forrigeUke()
        val forrigeUkeDatoPeriode =
            forrigeUke.alleDager().let {
                Datoperiode(it.first(), it.last())
            }

        val reiserMedPeriodeForrigeUke =
            finnRammevedtakPrivatBil(behandling)
                ?.reiser
                ?.filter { it.grunnlag.inneholder(forrigeUkeDatoPeriode) }
                ?: emptyList()

        val kjørelisterGruppertPåReiseId = kjørelisterGruppertPåReiseId(behandling.fagsakId)

        return reiserMedPeriodeForrigeUke
            .any { reise -> !kjørelisterGruppertPåReiseId.harLevertForUke(reise.reiseId, forrigeUke) }
    }

    fun skalSendeKjørelistevarselVedFerdigstillingAvBehandling(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        if (behandling.type == BehandlingType.KJØRELISTE) return false
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling) || !erGjeldendeIverksatteBehandling(behandling)) return false

        val rammevedtakReiser = finnRammevedtakPrivatBil(behandling)?.reiser ?: emptyList()
        val kjørelister = kjørelisterGruppertPåReiseId(behandling.fagsakId)

        return rammevedtakReiser.any {
            finnesTidligereUkerUtenInnsendtKjøreliste(it, kjørelister[it.reiseId] ?: emptyList())
        }
    }

    private fun finnesTidligereUkerUtenInnsendtKjøreliste(
        rammeForReiseMedPrivatBil: RammeForReiseMedPrivatBil,
        kjørelisterForReise: List<Kjøreliste>,
    ): Boolean {
        val nåværendeUke = UkeIÅr.nå()
        val alleUkerIRammevedtak =
            rammeForReiseMedPrivatBil.grunnlag
                .alleDatoerGruppertPåUke()
                .keys
                .filter { it < nåværendeUke }
        val alleInnsendteUker =
            kjørelisterForReise
                .flatMap { it.data.alleDatoer() }
                .map { it.tilUkeIÅr() }
                .toSet()

        return (alleUkerIRammevedtak - alleInnsendteUker).isNotEmpty()
    }

    private fun erBehandlingInnvilgelseEllerOpphørDagligReise(behandling: Saksbehandling) =
        behandling.stønadstype.gjelderDagligReise() &&
            (behandling.resultat == BehandlingResultat.INNVILGET || behandling.resultat == BehandlingResultat.OPPHØRT)

    private fun erGjeldendeIverksatteBehandling(behandling: Saksbehandling) =
        behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)?.id == behandling.id

    private fun finnRammevedtakPrivatBil(behandling: Saksbehandling): RammevedtakPrivatBil? =
        vedtakService
            .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandling.id)
            .data.rammevedtakPrivatBil

    private fun kjørelisterGruppertPåReiseId(fagsakId: FagsakId): Map<ReiseId, List<Kjøreliste>> =
        kjørelisteService.hentForFagsakId(fagsakId).groupBy { it.data.reiseId }
}

private fun Map<ReiseId, List<Kjøreliste>>.harLevertForUke(
    reiseId: ReiseId,
    uke: UkeIÅr,
): Boolean =
    this[reiseId]?.any {
        it.data
            .alleDatoer()
            .map { dato -> dato.tilUkeIÅr() }
            .contains(uke)
    } == true
