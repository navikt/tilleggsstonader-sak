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
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): List<Vedtaksperiode> {
        val aktuelleVedtaksperioder =
            finnAktuelleVedtaksperioder(
                forrigeVedtaksperioder = forrigeVedtaksperioder,
                forslag = forslag,
                tidligsteEndring = tidligsteEndring,
            )
        val nyttForslag =
            ForeslåVedtaksperioderBeholdId(
                forrigeVedtaksperioder = aktuelleVedtaksperioder.forrigeVedtaksperioder,
                initielleForslag = aktuelleVedtaksperioder.forslagEtterTidligtEndring,
            ).beholdTidligereIdnForVedtaksperioder()
        return mergeHvisLikeOgSammeId(
            forrigeVedtaksperioderSkalIkkeEndres = aktuelleVedtaksperioder.forrigeVedtaksperioderSkalIkkeEndres,
            nyttForslag = nyttForslag,
        )
    }

    /**
     * Vi skal ikke foreslå perioder før revurder fra, der må vi beholde alle perioder som tidligere
     * Kan fjernes når man fjernet revurder fra
     */
    private fun finnAktuelleVedtaksperioder(
        forrigeVedtaksperioder: List<Vedtaksperiode>,
        forslag: List<Vedtaksperiode>,
        tidligsteEndring: LocalDate?,
    ): Vedtaksperioder {
        val forrigeVedtaksperioderSkalIkkeEndres =
            if (tidligsteEndring != null) {
                forrigeVedtaksperioder.avkortFraOgMed(tidligsteEndring.minusDays(1))
            } else {
                emptyList()
            }

        /**
         * Korrigerer tom-dato for den siste vedtaksperioden i listen
         * sånn at man forlenger den siste perioden med tom fra nytt forslag
         * Gjelder kun når man må forholde seg til revurder-fra
         */
        val forrigeVedtaksperioderMedKorrigertTomDato: List<Vedtaksperiode> =
            forrigeVedtaksperioder.dropLast(1) +
                listOfNotNull(
                    forrigeVedtaksperioder
                        .lastOrNull()
                        ?.let { it.copy(tom = it.tom.plusDays(1)) },
                )

        val forslagEtterTidligtEndring =
            if (tidligsteEndring != null) {
                forslag.avkortPerioderFør(tidligsteEndring)
            } else {
                forslag
            }
        return Vedtaksperioder(
            forrigeVedtaksperioderSkalIkkeEndres = forrigeVedtaksperioderSkalIkkeEndres,
            forrigeVedtaksperioder = forrigeVedtaksperioderMedKorrigertTomDato,
            forslagEtterTidligtEndring = forslagEtterTidligtEndring,
        )
    }

    private data class Vedtaksperioder(
        val forrigeVedtaksperioderSkalIkkeEndres: List<Vedtaksperiode>,
        val forrigeVedtaksperioder: List<Vedtaksperiode>,
        val forslagEtterTidligtEndring: List<Vedtaksperiode>,
    )

    /**
     * Slår sammen forrige vedtaksperioder og nye forslag
     * Hvis man har en periode
     * 01.01.01 - 31.01.01 og man revurderer fra 1 feb, men man får ny målgruppe/aktivitet så skal man ikke forlenge den første perioden
     * Den første perioden finnes i [forrigeVedtaksperioderSkalIkkeEndres] samtidig har den blitt sendt inn til forslag, så ID på perioden finnes også i forslag
     * Det håndteres gjennom å sjekke at ID'n ikke finnes flere ganger. Hvis de finnes flere ganger, så genereres en ny ID for den perioden.
     */
    private fun mergeHvisLikeOgSammeId(
        forrigeVedtaksperioderSkalIkkeEndres: List<Vedtaksperiode>,
        nyttForslag: List<Vedtaksperiode>,
    ): List<Vedtaksperiode> {
        val idn = mutableSetOf<UUID>()
        return (forrigeVedtaksperioderSkalIkkeEndres + nyttForslag)
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
                beregnSnitt(delAvForslag, forrigeVedtaksperiode)
                    ?.let { snitt -> ForrigeVedtaksperiodeMedSnitt(forrigeVedtaksperiode, snitt) }
            }.firstOrNull()

    /**
     * Beregner snitt mellom del av forslag og forrige vedtaksperiode.
     * Hvis forrige vedtaksperiode er siste i listen og del av forslag overlapper,
     * så skal man beholde ID fra forrige vedtaksperiode, og forlenge denne til TOM på nye forslaget.
     */
    private fun beregnSnitt(
        delAvForslag: Vedtaksperiode,
        forrigeVedtaksperiode: Vedtaksperiode,
    ): Vedtaksperiode? {
        val erSisteForrigePeriode = forrigeVedtaksperiode == forrigeVedtaksperioder.last()
        return if (erSisteForrigePeriode && delAvForslag.overlapper(forrigeVedtaksperiode)) {
            delAvForslag.copy(fom = maxOf(forrigeVedtaksperiode.fom, delAvForslag.fom))
        } else {
            delAvForslag.beregnSnitt(forrigeVedtaksperiode)
        }
    }

    private fun Vedtaksperiode.medNyId(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
    ): Vedtaksperiode = this.copy(id = UUID.randomUUID(), fom = fom, tom = tom)

    private data class ForrigeVedtaksperiodeMedSnitt(
        val forrigeVedtaksperiode: Vedtaksperiode,
        val snitt: Vedtaksperiode,
    )
}
