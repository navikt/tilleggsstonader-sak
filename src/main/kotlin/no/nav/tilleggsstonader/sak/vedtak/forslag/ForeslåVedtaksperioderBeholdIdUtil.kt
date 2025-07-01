package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID

/**
 *  Når foreslå vedtaksperioder brukes i revurderinger trenger man å beholde id'n til vedtaksperioden for å kunne tracke endringer.
 *
 * Bruker en [ArrayDeque] for å håndtere tilfeller der et forslag overlapper flere tidligere vedtaksperioder.
 * Et tidligere id skal ikke gjenbrukes flere ganger
 */
object ForeslåVedtaksperioderBeholdIdUtil {
    fun beholdTidligereIdnForVedtaksperioder(
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> =
        ForeslåVedtaksperioderBeholdId(
            tidligereVedtaksperioder = tidligereVedtaksperioder,
            initielleForslag = forslag,
        ).beholdTidligereIdnForVedtaksperioder()

    fun beholdTidligereIdnForVedtaksperioderLæremidler(
        tidligereVedtaksperioder: List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
    ): List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode> =
        ForeslåVedtaksperioderBeholdId(
            tidligereVedtaksperioder = tidligereVedtaksperioder.map { it.tilFellesDomeneVedtaksperiode() },
            initielleForslag = forslag,
        ).beholdTidligereIdnForVedtaksperioder()
            .map {
                no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode(
                    id = it.id,
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                )
            }
}

/**
 * Plassert som en private class for at den ikke skal kalles på flere ganger.
 * Bruk av klasse gjør det enklere å splitte ut metoder som er avhengig av state i klassen.
 * [beholdTidligereIdnForVedtaksperioder] er avhengig av state i klassen
 */
private class ForeslåVedtaksperioderBeholdId(
    private val tidligereVedtaksperioder: List<Vedtaksperiode>,
    private val initielleForslag: List<Vedtaksperiode>,
) {
    private val nyttForslag = mutableListOf<Vedtaksperiode>()

    /**
     * Gjenbrukte ID'er for vedtaksperioder skal ikke gjenbrukes flere ganger
     */
    private val gjenbrukteIdn = mutableSetOf<UUID>()

    /**
     * Bruker nytt forslag delvis overlapper med tidligere vedtaksperiode.
     */
    private val stack = ArrayDeque<Vedtaksperiode>()

    fun beholdTidligereIdnForVedtaksperioder(): List<Vedtaksperiode> {
        initielleForslag.forEach { initielltForslag ->
            håndterInitieltForslag(initielltForslag)
        }
        return nyttForslag.sorted()
    }

    private fun håndterInitieltForslag(initielltForslag: Vedtaksperiode) {
        stack.add(initielltForslag)

        while (!stack.isEmpty()) {
            val forslag = stack.removeFirst()
            val tidligereVedtaksperiodeMedSnitt = beregnSnittForForslag(forslag)

            if (tidligereVedtaksperiodeMedSnitt != null) {
                val snitt = tidligereVedtaksperiodeMedSnitt.snitt
                val tidligereVedtaksperiodeId = tidligereVedtaksperiodeMedSnitt.tidligereVedtaksperiode.id

                gjenbrukteIdn.add(tidligereVedtaksperiodeId)
                nyttForslag.add(snitt.copy(id = tidligereVedtaksperiodeId))

                håndterForslagSomBegynnerFørSnitt(forslag, snitt)
                håndterForslagSomSlutterEtterSnitt(forslag, snitt)
            } else {
                nyttForslag.add(forslag)
            }
        }
    }

    private fun håndterForslagSomSlutterEtterSnitt(
        forslag: Vedtaksperiode,
        snitt: Vedtaksperiode,
    ) {
        if (forslag.tom > snitt.tom) {
            stack.add(forslag.medNyId(fom = snitt.tom.plusDays(1)))
        }
    }

    private fun håndterForslagSomBegynnerFørSnitt(
        forslag: Vedtaksperiode,
        snitt: Vedtaksperiode,
    ) {
        if (forslag.fom < snitt.fom) {
            nyttForslag.add(forslag.medNyId(tom = snitt.fom.minusDays(1)))
        }
    }

    /**
     * Finner første snitt mellom forslag og tidligere vedtaksperiode
     * Skal ikke gjenbruke et ID flere ganger, så filtrerer ut ID'er som allerede er gjenbrukt.
     */
    private fun beregnSnittForForslag(delAvForslag: Vedtaksperiode): TidligereVedtaksperiodeMedSnitt? =
        tidligereVedtaksperioder
            .asSequence()
            .filterNot { gjenbrukteIdn.contains(it.id) }
            .mapNotNull { tidligereVedtaksperiode ->
                beregnSnitt(delAvForslag, tidligereVedtaksperiode)
                    ?.let { snitt -> TidligereVedtaksperiodeMedSnitt(tidligereVedtaksperiode, snitt) }
            }.firstOrNull()

    /**
     * Beregner snitt mellom del av forslag og tidligere vedtaksperiode.
     * Hvis tidligere vedtaksperiode er siste i listen og del av forslag overlapper,
     * så skal man beholde ID fra tidligere vedtaksperiode, og forlenge denne til TOM på nye forslaget.
     */
    private fun beregnSnitt(
        delAvForslag: Vedtaksperiode,
        tidligereVedtaksperiode: Vedtaksperiode,
    ): Vedtaksperiode? {
        val erSisteTidligerePeriode = tidligereVedtaksperiode == tidligereVedtaksperioder.last()
        return if (erSisteTidligerePeriode && delAvForslag.overlapper(tidligereVedtaksperiode)) {
            delAvForslag.copy(fom = maxOf(tidligereVedtaksperiode.fom, delAvForslag.fom))
        } else {
            delAvForslag.beregnSnitt(tidligereVedtaksperiode)
        }
    }

    private fun Vedtaksperiode.medNyId(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
    ): Vedtaksperiode = this.copy(id = UUID.randomUUID(), fom = fom, tom = tom)

    private data class TidligereVedtaksperiodeMedSnitt(
        val tidligereVedtaksperiode: Vedtaksperiode,
        val snitt: Vedtaksperiode,
    )
}
