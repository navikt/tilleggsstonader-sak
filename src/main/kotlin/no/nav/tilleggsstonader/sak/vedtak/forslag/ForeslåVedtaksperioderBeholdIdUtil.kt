package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.util.min
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID

object ForeslåVedtaksperioderBeholdIdUtil {
    fun beholdTidligereIdnForVedtaksperioder(
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val vedtaksperiodeBeregner =
            lagVedtaksperiodeBeregner(
                forrigeVedtaksperioder = forrigeVedtaksperioder,
                forslag = forslag,
                tidligsteEndring = tidligsteEndring,
            )
        return vedtaksperiodeBeregner.beregnNyeVedtaksperioder()
    }

    /**
     * Vi skal ikke foreslå perioder før revurder fra, der må vi beholde alle perioder som tidligere
     */
    private fun lagVedtaksperiodeBeregner(
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): VedtaksperiodeBeregner {
        val kanEndrePerioderFraOgMed = min(tidligsteEndring, forrigeVedtaksperioder.maxOfOrNull { it.tom }?.plusDays(1))

        val forrigeVedtaksperioderSkalIkkeEndres =
            if (kanEndrePerioderFraOgMed != null) {
                forrigeVedtaksperioder.avkortFraOgMed(kanEndrePerioderFraOgMed.minusDays(1))
            } else {
                forrigeVedtaksperioder
            }

        val forslagEtterTidligsteEndring =
            if (kanEndrePerioderFraOgMed != null) {
                forslag.avkortPerioderFør(kanEndrePerioderFraOgMed)
            } else {
                forslag
            }
        return VedtaksperiodeBeregner(
            forrigeVedtaksperioderSkalIkkeEndres = forrigeVedtaksperioderSkalIkkeEndres,
            forrigeVedtaksperioder = forrigeVedtaksperioder,
            forslagEtterTidligsteEndring = forslagEtterTidligsteEndring,
        )
    }

    private data class VedtaksperiodeBeregner(
        private val forrigeVedtaksperioderSkalIkkeEndres: List<Vedtaksperiode>,
        private val forrigeVedtaksperioder: List<Vedtaksperiode>,
        private val forslagEtterTidligsteEndring: List<Vedtaksperiode>,
    ) {
        private val idnSomIkkeSkalMerges = forrigeVedtaksperioder.map { it.id }.toSet()

        fun beregnNyeVedtaksperioder(): List<Vedtaksperiode> {
            val nyttForslag =
                ForeslåVedtaksperioderBeholdId(
                    forrigeVedtaksperioder = forrigeVedtaksperioder,
                    initielleForslag = forslagEtterTidligsteEndring,
                ).beholdTidligereIdnForVedtaksperioder()
            return mergeFOrrigeMedNyttForslagHvisLikeMedSammeId(nyttForslag = nyttForslag)
                .korrigerIdnPåDuplikat()
                .mergeNyeVedtaksperioder()
        }

        /**
         * Slår sammen forrige vedtaksperioder og nye forslag
         * Hvis man har en periode
         * 01.01.01 - 31.01.01 og man revurderer fra 1 feb, men man får ny målgruppe/aktivitet så skal man ikke forlenge den første perioden
         * Den første perioden finnes i [forrigeVedtaksperioderSkalIkkeEndres] samtidig har den blitt sendt inn til forslag, så ID på perioden finnes også i forslag
         * Det håndteres gjennom å sjekke at ID'n ikke finnes flere ganger. Hvis de finnes flere ganger, så genereres en ny ID for den perioden.
         */
        private fun mergeFOrrigeMedNyttForslagHvisLikeMedSammeId(nyttForslag: List<Vedtaksperiode>): List<Vedtaksperiode> =
            (forrigeVedtaksperioderSkalIkkeEndres + nyttForslag)
                .mergeSammenhengende { v1, v2 ->
                    v1.id == v2.id && v1.erSammenhengendeMedLikMålgruppeOgAktivitet(v2)
                }

        /**
         * Hvis man ikke slått sammen perioder med lik ID pga ulik målgruppe eller aktivitet så kan det oppstå duplikate ID'er
         * Det korrigeres ved å generere en ny ID for de som har duplikat
         */
        private fun List<Vedtaksperiode>.korrigerIdnPåDuplikat(): List<Vedtaksperiode> {
            val idnSomErBrukte = mutableSetOf<UUID>()
            return this.map {
                val idFinnesIkkeFraFør = idnSomErBrukte.add(it.id)
                if (idFinnesIkkeFraFør) {
                    it
                } else {
                    it.copy(id = UUID.randomUUID())
                }
            }
        }

        /**
         * Pga at man kun slår sammen de som har lik ID og sen korrigerer idn på vedtaksperioder som har duplikate
         * så merges alle nye sammenhengende vedtaksperioder sammen
         */
        private fun List<Vedtaksperiode>.mergeNyeVedtaksperioder() =
            mergeSammenhengende { v1, v2 ->
                !idnSomIkkeSkalMerges.contains(v1.id) &&
                    !idnSomIkkeSkalMerges.contains(v2.id) &&
                    v1.erSammenhengendeMedLikMålgruppeOgAktivitet(v2)
            }
    }
}

/**
 * Når vedtaksperioder brukes i revurderinger trenger man å beholde id'n til vedtaksperioden for å kunne tracke endringer.
 *
 * Bruker en [ArrayDeque] for å håndtere tilfeller der et forslag overlapper flere forrige vedtaksperioder.
 * Et tidligere id skal ikke gjenbrukes flere ganger
 *
 * I tilfelle nytt forslag overlapper med forrige vedtaksperiode:
 * - Periode før snitt får nytt id
 * - Snitt beholder tidligere id
 * - Periode etter snitt får nytt id
 *
 * Plassert som en private class for at den ikke skal kalles på flere ganger.
 * Bruk av klasse gjør det enklere å splitte ut metoder som er avhengig av state i klassen.
 * [beholdTidligereIdnForVedtaksperioder] er avhengig av state i klassen
 */
private class ForeslåVedtaksperioderBeholdId(
    private val forrigeVedtaksperioder: List<Vedtaksperiode>,
    private val initielleForslag: List<Vedtaksperiode>,
) {
    private val nyttForslag = mutableListOf<Vedtaksperiode>()

    /**
     * Gjenbrukte ID'er for vedtaksperioder skal ikke gjenbrukes flere ganger
     */
    private val gjenbrukteIdn = mutableSetOf<UUID>()

    /**
     * Bruker nytt forslag delvis overlapper med forrige vedtaksperiode.
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
            val forrigeVedtaksperiodeMedSnitt = beregnSnittForForslag(forslag)

            if (forrigeVedtaksperiodeMedSnitt != null) {
                val snitt = forrigeVedtaksperiodeMedSnitt.snitt
                val forrigeVedtaksperiodeId = forrigeVedtaksperiodeMedSnitt.forrigeVedtaksperiode.id

                gjenbrukteIdn.add(forrigeVedtaksperiodeId)
                nyttForslag.add(snitt.copy(id = forrigeVedtaksperiodeId))

                leggTilForslagHvisBegynnerFørSnitt(forslag, snitt)
                håndterForslagSomSlutterEtterSnitt(forslag, snitt)
            } else {
                nyttForslag.add(forslag)
            }
        }
    }

    private fun leggTilForslagHvisBegynnerFørSnitt(
        forslag: Vedtaksperiode,
        snitt: Vedtaksperiode,
    ) {
        if (forslag.fom < snitt.fom) {
            nyttForslag.add(forslag.medNyId(tom = snitt.fom.minusDays(1)))
        }
    }

    /**
     * Et forslag som slutter etter snittet legges til i stacken for å sjekke om det overlapper med andre vedtaksperioder
     */
    private fun håndterForslagSomSlutterEtterSnitt(
        forslag: Vedtaksperiode,
        snitt: Vedtaksperiode,
    ) {
        if (forslag.tom > snitt.tom) {
            stack.add(forslag.medNyId(fom = snitt.tom.plusDays(1)))
        }
    }

    /**
     * Finner første snitt mellom forslag og forrige vedtaksperiode
     * Skal ikke gjenbruke et ID flere ganger, så filtrerer ut ID'er som allerede er gjenbrukt.
     */
    private fun beregnSnittForForslag(delAvForslag: Vedtaksperiode): ForrigeVedtaksperiodeMedSnitt? =
        forrigeVedtaksperioder
            .asSequence()
            .filterNot { gjenbrukteIdn.contains(it.id) }
            .mapNotNull { forrigeVedtaksperiode ->
                delAvForslag
                    .beregnSnitt(forrigeVedtaksperiode)
                    ?.let { snitt -> ForrigeVedtaksperiodeMedSnitt(forrigeVedtaksperiode, snitt) }
            }.firstOrNull()

    private fun Vedtaksperiode.medNyId(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
    ): Vedtaksperiode = this.copy(id = UUID.randomUUID(), fom = fom, tom = tom)

    private data class ForrigeVedtaksperiodeMedSnitt(
        val forrigeVedtaksperiode: Vedtaksperiode,
        val snitt: Vedtaksperiode,
    )
}
