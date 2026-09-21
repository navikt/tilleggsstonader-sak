package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilMålgruppe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Bestemmer om en journalpost skal rutes til TSO- eller TSR-varianten av en gitt stønadstype.
 *
 * Rekkefølge:
 * 1. Hvis journalposten ikke har en strukturert søknad rutes den basert på journalpostens tema.
 * 2. Hvis bruker har ytelser fra register (Arena m.fl.) i den aktuelle perioden, brukes disse til å
 *    avgjøre målgruppe.
 * 3. Hvis registeret ikke gir treff, faller vi tilbake på ytelsene bruker selv har oppgitt i søknaden.
 */
@Service
class BestemTemaForJournalpostService(
    private val ytelseService: YtelseService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun bestemStønadstype(
        journalpost: Journalpost,
        stønadstypeTso: Stønadstype,
        stønadstypeTsr: Stønadstype,
        fom: LocalDate,
        tom: LocalDate,
        målgrupperFraSøknad: Set<MålgruppeType>,
    ): Stønadstype {
        val målgrupperFraRegister = hentMålgrupperFraRegister(journalpost, fom, tom).toSet()
        val målgrupper = målgrupperFraRegister.takeIf { it.isNotEmpty() } ?: målgrupperFraSøknad

        logger.info(
            "Forsøker å finne stønadstype for journalpost ${journalpost.journalpostId}, " +
                "målgrupper fra register: $målgrupperFraRegister, målgrupper fra søknad: $målgrupperFraSøknad",
        )

        val stønadstype =
            if (målgrupper.all { it.kanBrukesForStønad(stønadstypeTso) }) {
                stønadstypeTso
            } else {
                stønadstypeTsr
            }

        logger.info("Stønadstype for ${journalpost.journalpostId}: $stønadstype")
        return stønadstype
    }

    private fun hentMålgrupperFraRegister(
        journalpost: Journalpost,
        fom: LocalDate,
        tom: LocalDate,
    ): List<MålgruppeType> {
        feilHvis(journalpost.bruker == null) {
            "Forventer at bruker skal være satt på journalpost"
        }

        return ytelseService
            .hentYtelser(
                ident = journalpost.bruker!!.id,
                fom = fom,
                tom = tom,
                typer = TypeYtelsePeriode.entries.toList(),
            ).also { validerResultat(it.kildeResultat) }
            .perioder
            .map { it.type.tilMålgruppe() }
    }

    private fun validerResultat(kildeResultat: List<YtelsePerioderDto.KildeResultatYtelse>) {
        val feiledeHentingerAvYtelse = kildeResultat.filter { it.resultat == ResultatKilde.FEILET }

        feilHvis(feiledeHentingerAvYtelse.isNotEmpty()) {
            "Feil ved henting av ytelser ${feiledeHentingerAvYtelse.map { it.type }}"
        }
    }
}
