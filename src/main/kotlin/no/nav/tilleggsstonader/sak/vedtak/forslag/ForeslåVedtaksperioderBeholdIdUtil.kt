package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID

object ForeslåVedtaksperioderBeholdIdUtil {
    fun beholdTidligereIdnForVedtaksperioder(
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligstEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val aktuelleVedtaksperioder =
            finnAktuelleVedtaksperioder(
                tidligereVedtaksperioder = tidligereVedtaksperioder,
                forslag = forslag,
                tidligstEndring = tidligstEndring,
            )
        val nyttForslag =
            ForeslåVedtaksperioderBeholdId(
                tidligereVedtaksperioder = aktuelleVedtaksperioder.tidligereVedtaksperioder,
                initielleForslag = aktuelleVedtaksperioder.forslagEtterTidligtEndring,
            ).beholdTidligereIdnForVedtaksperioder()
        return mergeHvisLikeOgSammeId(
            tidligereVedtaksperioderSkalIkkeEndres = aktuelleVedtaksperioder.tidligereVedtaksperioderSkalIkkeEndres,
            nyttForslag = nyttForslag,
        )
    }

    /**
     * Vi skal ikke foreslå perioder før revurder fra, der må vi beholde alle perioder som tidligere
     * Kan fjernes når man fjernet revurder fra
     */
    private fun finnAktuelleVedtaksperioder(
        tidligereVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligstEndring: LocalDate?,
    ): Vedtaksperioder {
        val tidligereVedtaksperioderSkalIkkeEndres =
            if (tidligstEndring != null) {
                tidligereVedtaksperioder.avkortFraOgMed(tidligstEndring.minusDays(1))
            } else {
                emptyList()
            }

        /**
         * Korrigerer tom-dato for den siste vedtaksperioden i listen
         * sånn at man forlenger den siste perioden med tom fra nytt forslag
         * Gjelder kun når man må forholde seg til revurder-fra
         */
        val tidligereVedtaksperioderMedKorrigertTomDato: List<Vedtaksperiode> =
            tidligereVedtaksperioder.dropLast(1) +
                listOfNotNull(
                    tidligereVedtaksperioder
                        .lastOrNull()
                        ?.let { it.copy(tom = it.tom.plusDays(1)) },
                )

        val forslagEtterTidligtEndring =
            if (tidligstEndring != null) {
                forslag.avkortPerioderFør(tidligstEndring)
            } else {
                forslag
            }
        return Vedtaksperioder(
            tidligereVedtaksperioderSkalIkkeEndres = tidligereVedtaksperioderSkalIkkeEndres,
            tidligereVedtaksperioder = tidligereVedtaksperioderMedKorrigertTomDato,
            forslagEtterTidligtEndring = forslagEtterTidligtEndring,
        )
    }

    private data class Vedtaksperioder(
        val tidligereVedtaksperioderSkalIkkeEndres: List<Vedtaksperiode>,
        val tidligereVedtaksperioder: List<Vedtaksperiode>,
        val forslagEtterTidligtEndring: List<Vedtaksperiode>,
    )

    private fun List<Vedtaksperiode>.tilVedtaksperiodeLæremidler() =
        this.map {
            no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
            )
        }

    /**
     * Slår sammen tidligere vedtaksperioder og nye forslag
     * Hvis man har en periode
     * 01.01.01 - 31.01.01 og man revurderer fra 1 feb, men man får ny målgruppe/aktivitet så skal man ikke forlenge den første perioden
     * Den første perioden finnes i [tidligereVedtaksperioderSkalIkkeEndres] samtidig har den blitt sendt inn til forslag, så ID på perioden finnes også i forslag
     * Det håndteres gjennom å sjekke at ID'n ikke finnes flere ganger. Hvis de finnes flere ganger, så genereres en ny ID for den perioden.
     */
    private fun mergeHvisLikeOgSammeId(
        tidligereVedtaksperioderSkalIkkeEndres: List<Vedtaksperiode>,
        nyttForslag: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> {
        val idn = mutableSetOf<UUID>()
        return (tidligereVedtaksperioderSkalIkkeEndres + nyttForslag)
            .mergeSammenhengende(
                skalMerges = { v1, v2 ->
                    v1.id == v2.id &&
                        v1.målgruppe == v2.målgruppe &&
                        v1.aktivitet == v2.aktivitet &&
                        v1.overlapperEllerPåfølgesAv(v2)
                },
                merge = { v1, v2 -> v1.copy(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            ).map {
                val idFinnesIkkeFraFør = idn.add(it.id)
                if (idFinnesIkkeFraFør) {
                    it
                } else {
                    it.copy(id = UUID.randomUUID())
                }
            }
    }
}

/**
 * Når vedtaksperioder brukes i revurderinger trenger man å beholde id'n til vedtaksperioden for å kunne tracke endringer.
 *
 * Bruker en [ArrayDeque] for å håndtere tilfeller der et forslag overlapper flere tidligere vedtaksperioder.
 * Et tidligere id skal ikke gjenbrukes flere ganger
 *
 * I tilfelle nytt forslag overlapper med tidligere vedtaksperiode:
 * - Periode før snitt får nytt id
 * - Snitt beholder tidligere id
 * - Periode etter snitt får nytt id
 *
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
