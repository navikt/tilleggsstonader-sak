package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID

/**
 * For å kunne foreslå vedtaksperioder i revurderinger trenger man å beholde id'n til vedtaksperioden for å kunne tracke endringer.
 *
 * Bruker en [ArrayDeque] for å håndtere tilfeller der et forslag overlapper flere tidligere vedtaksperioder.
 * Et tidligere id skal ikke gjenbrukes flere ganger
 */
object ForeslåVedtaksperioderBeholdIdUtil {
    fun beholdTidligereIdnForVedtaksperioder(
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        forslagFraVilkår: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> {
        val perioder = mutableListOf<Vedtaksperiode>()
        val gjenbrukteIdn = mutableSetOf<UUID>()
        forslagFraVilkår.forEach { initielltForslag ->

            val stack = ArrayDeque<Vedtaksperiode>()
            stack.add(initielltForslag)

            while (!stack.isEmpty()) {
                val forslag = stack.removeFirst()
                val tidligereVedtaksperiodeMedSnitt = snitt(forslag, tidligereVedtaksperioder, gjenbrukteIdn)

                if (tidligereVedtaksperiodeMedSnitt != null) {
                    val snitt = tidligereVedtaksperiodeMedSnitt.snitt
                    val tidligereVedtaksperiodeId = tidligereVedtaksperiodeMedSnitt.tidligereVedtaksperiode.id

                    gjenbrukteIdn.add(tidligereVedtaksperiodeId)
                    perioder.add(snitt.copy(id = tidligereVedtaksperiodeId))

                    // Hvis forslaget starter før snittet, legger vi til forslaget med nytt tom
                    if (forslag.fom < snitt.fom) {
                        perioder.add(forslag.medNyId(tom = snitt.fom.minusDays(1)))
                    }

                    // Hvis snittet slutter før forslaget, legger vi til resten av forslaget i stacken for å kunne sjekke videre
                    if (forslag.tom > snitt.tom) {
                        stack.add(forslag.medNyId(fom = snitt.tom.plusDays(1)))
                    }
                } else {
                    perioder.add(forslag)
                }
            }
        }
        return perioder.sorted()
    }

    /**
     * Finner første snitt mellom forslag og tidligere vedtaksperiode
     * Skal ikke gjenbruke et ID flere ganger, så filtrerer ut ID'er som allerede er gjenbrukt.
     */
    private fun snitt(
        delAvForslag: Vedtaksperiode,
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        gjenbrukteIdn: Set<UUID>,
    ): TidligereVedtaksperiodeMedSnitt? =
        tidligereVedtaksperioder
            .asSequence()
            .filterNot { gjenbrukteIdn.contains(it.id) }
            .mapNotNull { tidligereVedtaksperiode ->
                delAvForslag
                    .beregnSnitt(tidligereVedtaksperiode)
                    ?.let { snitt -> TidligereVedtaksperiodeMedSnitt(tidligereVedtaksperiode, snitt) }
            }.firstOrNull()

    private fun Vedtaksperiode.medNyId(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
    ): Vedtaksperiode = this.copy(id = UUID.randomUUID(), fom = fom, tom = tom)

    private data class TidligereVedtaksperiodeMedSnitt(
        val tidligereVedtaksperiode: Vedtaksperiode,
        val snitt: Vedtaksperiode,
    )
}
