package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import java.time.LocalDate

fun validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(
    rammeForReise: RammeForReiseMedPrivatBil,
    kjøreliste: Kjøreliste,
) {
    feilHvisIkke(rammeForReise.grunnlag.inneholder(kjøreliste.data)) {
        "Kjøreliste er ikke innenfor rammevedtaket"
    }
}

fun validerOppdatertAvklartKjørtUke(
    oppdaterteDager: List<AvklartKjørtDag>,
    fomUkeSomSkalOppdateres: LocalDate,
    rammevedtak: RammeForReiseMedPrivatBil,
    innsendteKjørelisteDager: List<KjørelisteDag>,
) {
    validerAntallDagerGodkjentInnenforRammevedtak(oppdaterteDager, rammevedtak)
    validerInnsendteDagerErInnenforUken(fomUkeSomSkalOppdateres, oppdaterteDager)

    oppdaterteDager.forEach { oppdatertDag ->
        oppdatertDag.validerGyldigeVerdier()

        val innsendtKjørelisteDag = innsendteKjørelisteDager.find { it.dato == oppdatertDag.dato }
        oppdatertDag.validerBegrunnelse(innsendtKjørelisteDag)
    }
}

private fun validerAntallDagerGodkjentInnenforRammevedtak(
    oppdaterteDager: List<AvklartKjørtDag>,
    rammevedtak: RammeForReiseMedPrivatBil,
) {
    val antallDagerSomDekkes = oppdaterteDager.count { it.godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA }

    brukerfeilHvis(antallDagerSomDekkes > rammevedtak.grunnlag.reisedagerPerUke) {
        "Antall godkjente reisedager kan ikke være høyere enn antall dager godkjent i rammevedtak"
    }
}

private fun validerInnsendteDagerErInnenforUken(
    fomUke: LocalDate,
    oppdaterteDager: List<AvklartKjørtDag>,
) {
    brukerfeilHvis(oppdaterteDager.any { it.dato.tilUkeIÅr() != fomUke.tilUkeIÅr() }) {
        "Alle dager må være innenfor uken som skal oppdateres"
    }
}

private fun AvklartKjørtDag.validerGyldigeVerdier() {
    brukerfeilHvis(godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.IKKE_VURDERT) {
        "Alle dager som oppdateres må være huket av som enten godkjent eller ikke godkjent"
    }

    brukerfeilHvis(parkeringsutgift != null && godkjentGjennomførtKjøring != GodkjentGjennomførtKjøring.JA) {
        "Parkeringsutgift kan kun settes dersom kjøring for dag er godkjent"
    }
}

private fun AvklartKjørtDag.validerBegrunnelse(innsendtKjøreliste: KjørelisteDag?) {
    if (begrunnelse != null) return // Begrunnelse er oppgitt, ingen validering nødvendig

    brukerfeilHvis(parkeringsutgift != null && parkeringsutgift > 100) {
        "Må oppgi begrunnelse for parkeringsutgift over 100 for dag ${dato.norskFormat()}"
    }

    brukerfeilHvis(innsendtKjøreliste == null) {
        "Må oppgi begrunnelse for å endre dag ${dato.norskFormat()} når det ikke finnes en opprinnelig kjøreliste for dagen"
    }

    brukerfeilHvis(godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA && !innsendtKjøreliste.harKjørt) {
        "Må oppgi begrunnelse for å godkjenne kjøring når bruker ikke har oppgitt å ha kjørt for dag ${dato.norskFormat()}"
    }

    brukerfeilHvis(godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.NEI && innsendtKjøreliste.harKjørt) {
        "Må oppgi begrunnelse for å ikke godkjenne kjøring når bruker har oppgitt å ha kjørt for dag ${dato.norskFormat()}"
    }

    brukerfeilHvis(parkeringsutgift != innsendtKjøreliste.parkeringsutgift) {
        "Må oppgi begrunnelse for å endring av parkeringsutgift på dag ${dato.norskFormat()}"
    }

    brukerfeilHvis(godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA && avvik.isNotEmpty()) {
        "Må oppgi begrunnelse for å godkjenne kjøring når det tidligere var avvik på dag ${dato.norskFormat()}"
    }
}
