package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.util.UUID

object ForeslåVedtaksperioderBeholdIdUtil {
    fun beholdTidligereIdnForVedtaksperioder(
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> {
        val perioder = mutableListOf<Vedtaksperiode>()
        val tidligereVedtaksperioderSomErGjenbrukt = mutableSetOf<UUID>()
        forslag.forEach { nyVedtaksperiode ->

            val stack = ArrayDeque<Vedtaksperiode>()
            stack.add(nyVedtaksperiode)

            while (!stack.isEmpty()) {
                val poppedNyVedtaksperiode = stack.removeFirst()
                val tidligereVedtaksperiodeMedSnitt =
                    tidligereVedtaksperioder
                        .asSequence()
                        .filterNot { tidligereVedtaksperioderSomErGjenbrukt.contains(it.id) }
                        .mapNotNull { tidligereVedtaksperiode ->
                            poppedNyVedtaksperiode
                                .beregnSnitt(tidligereVedtaksperiode)
                                ?.let { snitt -> TidligereVedtaksperiodeMedSnitt(tidligereVedtaksperiode, snitt) }
                        }.firstOrNull()

                if (tidligereVedtaksperiodeMedSnitt != null) {
                    val snitt = tidligereVedtaksperiodeMedSnitt.snitt
                    perioder.add(snitt.copy(id = tidligereVedtaksperiodeMedSnitt.tidligereVedtaksperiode.id))
                    tidligereVedtaksperioderSomErGjenbrukt.add(tidligereVedtaksperiodeMedSnitt.tidligereVedtaksperiode.id)

                    if (poppedNyVedtaksperiode.fom < snitt.fom) {
                        perioder.add(poppedNyVedtaksperiode.copy(id = UUID.randomUUID(), tom = snitt.fom.minusDays(1)))
                    }
                    if (poppedNyVedtaksperiode.tom > snitt.tom) {
                        stack.add(poppedNyVedtaksperiode.copy(id = UUID.randomUUID(), fom = snitt.tom.plusDays(1)))
                    }
                } else {
                    perioder.add(poppedNyVedtaksperiode)
                }
            }
        }
        return perioder.sorted()
    }

    private data class TidligereVedtaksperiodeMedSnitt(
        val tidligereVedtaksperiode: Vedtaksperiode,
        val snitt: Vedtaksperiode,
    )
}
