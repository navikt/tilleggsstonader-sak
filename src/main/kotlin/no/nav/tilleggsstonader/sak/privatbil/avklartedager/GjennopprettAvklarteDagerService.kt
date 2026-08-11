package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.libs.feil.singleEllerFeil
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus.ENDRET
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.springframework.stereotype.Service

@Service
class GjennopprettAvklarteDagerService(
    private val avklartKjørelisteService: AvklartKjørelisteService,
    private val avklartKjørtUkeRepository: AvklartKjørtUkeRepository,
    private val kjørelisteService: KjørelisteService,
) {

    /**
     * Gjenoppretter uker som ble slettet i historikken for fagsaken, men som nå
     * er innenfor rammevedtaket igjen i ny behandling.
     *
     * Saksbehandler sin vurdering nullstilles ved at de gjenopprettede ukene
     * får status [AvklartKjørtUkeStatus.NY], slik at de må vurderes på nytt.
     *
     * Dersom kun deler av uka var slettet tidligere får uka status [ENDRET],
     * og de nye dagene får status [AvklartKjørtDagStatus.NY] og må vurderes på nytt.
     */
    fun gjenopprettTidligereSlettedeDagerSomNåErInnenforRammevedtak(
        fagsakId: FagsakId,
        behandlingId: BehandlingId,
        rammevedtak: RammevedtakPrivatBil?,
    ) {
        if (rammevedtak == null) {
            return
        }

        val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)
        val avklarteDager = avklarteUker.flatMap { it.dager }
        val kjørelisteUkerPåFagsak = hentKjørelisteUkerPåFagsak(fagsakId)

        val (ukerSomFinnesIAvklarteUker, ukerSomIkkeFinnesIAvklarteUker) =
            kjørelisteUkerPåFagsak.partition { it.finnesI(avklarteUker) }

        val heleUkerSomSkalGjennopprettes =
            ukerSomIkkeFinnesIAvklarteUker.mapNotNull { slettetUke ->
                gjenopprettUkeHvisInnenforRammevedtak(
                    ukeSomSkalGjenopprettes = slettetUke,
                    behandlingId = behandlingId,
                    rammevedtak = rammevedtak,
                )
            }

        val ukerMedEnkeltdagerSomSkalGjennopprettes =
            ukerSomFinnesIAvklarteUker
                .filter { it.harDagerSomIkkeErAvklart(avklarteDager) }
                .mapNotNull { delvisSlettetUke ->
                    gjennopprettEnkeltdagerHvisInnenforRammevedtak(
                        ukeSomSkalGjenopprettes = delvisSlettetUke,
                        avklarteUker = avklarteUker,
                        rammevedtak = rammevedtak,
                    )
                }

        if (heleUkerSomSkalGjennopprettes.isNotEmpty()) {
            avklartKjørtUkeRepository.insertAll(heleUkerSomSkalGjennopprettes)
        }
        if (ukerMedEnkeltdagerSomSkalGjennopprettes.isNotEmpty()) {
            avklartKjørtUkeRepository.updateAll(ukerMedEnkeltdagerSomSkalGjennopprettes)
        }
    }

    private fun gjennopprettEnkeltdagerHvisInnenforRammevedtak(
        ukeSomSkalGjenopprettes: KjørelisteUke,
        avklarteUker: List<AvklartKjørtUke>,
        rammevedtak: RammevedtakPrivatBil,
    ): AvklartKjørtUke? {
        val avklartUkeSomSkalOppdateres =
            avklarteUker.singleEllerFeil(
                predicate = { avklartUke -> ukeSomSkalGjenopprettes.erSammeUkeOgReise(avklartUke) },
            ) { "Forventet en uke som skal oppdateres. Fant ${avklarteUker.size} stk." }

        val rammevedtakForReise =
            rammevedtak.reiser.find { it.reiseId == ukeSomSkalGjenopprettes.reiseId } ?: return null
        val reisedagerInnenforNyttRammevedtak =
            ukeSomSkalGjenopprettes.reisedager.filter { rammevedtakForReise.grunnlag.inneholder(it.dato) }
        if (reisedagerInnenforNyttRammevedtak.isEmpty()) return null

        val avvikForUke = utledAvvikForUke(rammevedtakForReise, reisedagerInnenforNyttRammevedtak)

        val (reisedagerSomSkalAvklaresPåNytt, reisedagerSomErAvklartFraTidligre) =
            reisedagerInnenforNyttRammevedtak
                .partition { kjørelisteDag -> !kjørelisteDag.finnesI(avklartUkeSomSkalOppdateres.dager) }

        val avklarteDagerIDenneBehandlingen = reisedagerSomSkalAvklaresPåNytt.map { utledAvklartDag(it, avvikForUke) }
        val avklarteDagerFraForrgieBehandling =
            avklartUkeSomSkalOppdateres.dager
                .filter { avklartDag -> avklartDag.finnesI(reisedagerSomErAvklartFraTidligre) }
        val alleAvklarteDager = avklarteDagerIDenneBehandlingen + avklarteDagerFraForrgieBehandling

        return avklartUkeSomSkalOppdateres.copy(
            avklartKjørtUkeStatus = ENDRET,
            fom = alleAvklarteDager.minOf { it.dato },
            tom = alleAvklarteDager.maxOf { it.dato },
            status = utledAutomatiskStatusForUke(alleAvklarteDager, avvikForUke),
            typeAvvik = avvikForUke,
            dager = alleAvklarteDager.toSet(),
        )
    }

    private fun gjenopprettUkeHvisInnenforRammevedtak(
        ukeSomSkalGjenopprettes: KjørelisteUke,
        behandlingId: BehandlingId,
        rammevedtak: RammevedtakPrivatBil,
    ): AvklartKjørtUke? {
        val rammevedtakForReise =
            rammevedtak.reiser.find { it.reiseId == ukeSomSkalGjenopprettes.reiseId } ?: return null
        val reisedagerInnenforNyttRammevedtak =
            ukeSomSkalGjenopprettes.reisedager.filter { rammevedtakForReise.grunnlag.inneholder(it.dato) }
        if (reisedagerInnenforNyttRammevedtak.isEmpty()) return null

        return utledAvklartUke(
            behandlingId = behandlingId,
            kjørelisteId = ukeSomSkalGjenopprettes.kjørelisteId,
            ukeIÅr = ukeSomSkalGjenopprettes.uke,
            reisedager = reisedagerInnenforNyttRammevedtak,
            rammevedtak = rammevedtakForReise,
        )
    }

    private fun hentKjørelisteUkerPåFagsak(fagsakId: FagsakId): List<KjørelisteUke> =
        kjørelisteService
            .hentForFagsakId(fagsakId)
            .flatMap { kjøreliste ->
                kjøreliste.data.reisedager
                    .groupBy { it.dato.tilUkeIÅr() }
                    .map { (uke, reisedager) ->
                        KjørelisteUke(
                            kjørelisteId = kjøreliste.id,
                            reiseId = kjøreliste.data.reiseId,
                            uke = uke,
                            reisedager = reisedager,
                        )
                    }
            }.groupBy { ReiseUke(it.reiseId, it.uke) }
            .map { (reiseUke, kjørelisteUker) ->
                kjørelisteUker.singleEllerFeil {
                    "Fant ingen eller duplikate kjørelister for reise=${reiseUke.reiseId} og uke=${reiseUke.uke}"
                }
            }
}


private fun KjørelisteDag.finnesI(reisedager: Set<AvklartKjørtDag>): Boolean {
    val reisedagerDatoer = reisedager.map { it.dato }
    return dato in reisedagerDatoer
}

private fun AvklartKjørtDag.finnesI(reisedager: List<KjørelisteDag>): Boolean {
    val reisedagerDatoer = reisedager.map { it.dato }
    return dato in reisedagerDatoer
}

private data class ReiseUke(
    val reiseId: ReiseId,
    val uke: UkeIÅr,
)

private data class KjørelisteUke(
    val kjørelisteId: KjørelisteId,
    val reiseId: ReiseId,
    val uke: UkeIÅr,
    val reisedager: List<KjørelisteDag>,
) {
    fun finnesI(avklarteUker: List<AvklartKjørtUke>): Boolean {
        val reisedagerDatoer = avklarteUker.map { ReiseUke(it.reiseId, it.uke) }
        return ReiseUke(reiseId, uke) in reisedagerDatoer
    }

    fun harDagerSomIkkeErAvklart(avklarteDager: List<AvklartKjørtDag>): Boolean {
        val avklarteDatoer = avklarteDager.map { it.dato }.toSet()
        return reisedager.any { it.harKjørt && it.dato !in avklarteDatoer }
    }

    fun erSammeUkeOgReise(avklartKjørtUke: AvklartKjørtUke): Boolean =
        ReiseUke(reiseId, uke) == ReiseUke(avklartKjørtUke.reiseId, avklartKjørtUke.uke)
}
