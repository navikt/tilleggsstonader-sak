package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvisIkke
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

fun validerManuellKjøreliste(
    innsendtKjøreliste: InnsendtKjøreliste,
    rammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
    eksisterendeKjørelister: List<Kjøreliste>,
) {
    brukerfeilHvis(innsendtKjøreliste.reisedager.any { it.dato > LocalDate.now() }) {
        "Kan ikke registrere kjøreliste for dager som er fremover i tid"
    }

    validerInnsendtKjørelisteMotRammevedtak(
        rammevedtakForReise = rammevedtakForReise,
        innsendtKjøreliste = innsendtKjøreliste,
    )

    validerDagerIkkeTidligereInnsendt(
        innsendtKjøreliste = innsendtKjøreliste,
        eksisterendeKjørelister = eksisterendeKjørelister,
    )
}

private fun validerInnsendtKjørelisteMotRammevedtak(
    rammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
    innsendtKjøreliste: InnsendtKjøreliste,
) {
    feilHvis(rammevedtakForReise == null) {
        "Fant ikke rammevedtak for reise ${innsendtKjøreliste.reiseId}"
    }

    brukerfeilHvisIkke(rammevedtakForReise.grunnlag.inneholder(innsendtKjøreliste)) {
        "Perioden for innsendt kjøreliste er utenfor rammevedtaket"
    }

    validerFullstendigeUkerInnsendt(rammevedtakForReise, innsendtKjøreliste)
}

private fun validerFullstendigeUkerInnsendt(
    rammevedtakForReise: RammevedtakForReiseMedPrivatBil,
    innsendtKjøreliste: InnsendtKjøreliste,
) {
    val rammevedtakGruppertPåUker = rammevedtakForReise.grunnlag.alleDatoerGruppertPåUke()
    val innsendtKjørelisteGruppertPåUker = innsendtKjøreliste.reisedager.map { it.dato }.groupBy { it.tilUkeIÅr() }

    innsendtKjørelisteGruppertPåUker.map { (uke, innsendteDager) ->
        val antallDagerIRammeUke = rammevedtakGruppertPåUker.get(uke)?.size

        brukerfeilHvis(antallDagerIRammeUke != innsendteDager.size) {
            "Uke ${uke.ukenummer} er sendt inn ufullstendig."
        }
    }
}

private fun validerDagerIkkeTidligereInnsendt(
    innsendtKjøreliste: InnsendtKjøreliste,
    eksisterendeKjørelister: List<Kjøreliste>,
) {
    brukerfeilHvis(eksisterendeKjørelister.any { it.data.overlapper(innsendtKjøreliste) }) {
        "Innsendte dager overlapper med tidligere innsendte kjørelister for reise ${innsendtKjøreliste.reiseId}"
    }
}

fun validerOppdaterKjøreliste(
    ukerSomOppdateres: Map<UkeIÅr, List<KjørelisteDag>>,
    oppdaterteReisedager: List<KjørelisteDag>,
    reiseId: ReiseId,
    rammevedtakForReise: RammevedtakForReiseMedPrivatBil?,
    andreKjørelister: List<Kjøreliste>,
) {
    ukerSomOppdateres.forEach { (uke, dager) ->
        brukerfeilHvis(dager.any { it.dato.tilUkeIÅr() != uke }) {
            "Oppdaterte dager må tilhøre uke ${uke.ukenummer}"
        }
    }

    validerManuellKjøreliste(
        innsendtKjøreliste = InnsendtKjøreliste(reiseId = reiseId, reisedager = oppdaterteReisedager),
        rammevedtakForReise = rammevedtakForReise,
        eksisterendeKjørelister = andreKjørelister,
    )
}
