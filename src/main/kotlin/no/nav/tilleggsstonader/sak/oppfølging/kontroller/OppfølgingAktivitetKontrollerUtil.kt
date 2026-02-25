package no.nav.tilleggsstonader.sak.oppfølging.kontroller

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.oppfølging.Kontroll
import no.nav.tilleggsstonader.sak.oppfølging.PeriodeForKontroll
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårAktivitet
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingRegisterAktiviteter
import no.nav.tilleggsstonader.sak.oppfølging.ÅrsakKontroll
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Finner endringer i aktivitet i forhold til hvilke vedtaksperioder som finnes
 */
object OppfølgingAktivitetKontrollerUtil {
    fun finnEndringer(
        vedtaksperioder: List<Vedtaksperiode>,
        registerAktiviteter: OppfølgingRegisterAktiviteter,
        aktiviteter: List<OppfølgingInngangsvilkårAktivitet>,
    ): List<PeriodeForKontroll> = vedtaksperioder.map { it.finnEndringer(registerAktiviteter, aktiviteter) }

    private fun Vedtaksperiode.finnEndringer(
        oppfølgingRegisterAktiviteter: OppfølgingRegisterAktiviteter,
        inngangsvilkår: List<OppfølgingInngangsvilkårAktivitet>,
    ): PeriodeForKontroll {
        val endringerAktivitet = finnEndringIAktivitet(oppfølgingRegisterAktiviteter, inngangsvilkår)
        return PeriodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            type = this.aktivitet,
            endringer = endringerAktivitet,
        )
    }

    private fun Vedtaksperiode.finnEndringIAktivitet(
        oppfølgingRegisterAktiviteter: OppfølgingRegisterAktiviteter,
        aktiviteter: List<OppfølgingInngangsvilkårAktivitet>,
    ): List<Kontroll> {
        val kontroller =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableListOf() // Skal ikke kontrolleres
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK ->
                    finnEndringIRegisteraktivitetEllerAlle(this, aktiviteter, oppfølgingRegisterAktiviteter.tiltak)

                AktivitetType.UTDANNING ->
                    finnEndringIRegisteraktivitetEllerAlle(
                        this,
                        aktiviteter,
                        oppfølgingRegisterAktiviteter.utdanningstiltak,
                    )
            }

        val ingenTreff = kontroller.any { it.årsak == ÅrsakKontroll.INGEN_TREFF }
        if (ingenTreff && oppfølgingRegisterAktiviteter.alleAktiviteter.any { it.inneholder(this) }) {
            return kontroller + Kontroll(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE)
        }
        return kontroller
    }

    /**
     * Kontrollerer om det er endringer i registeraktivitet
     * * Kontrollerer om en registeraktivitet inneholder hele vedtaksperioden
     * * Kontrollerer om
     */
    private fun finnEndringIRegisteraktivitetEllerAlle(
        vedtaksperiode: Vedtaksperiode,
        aktiviteter: List<OppfølgingInngangsvilkårAktivitet>,
        registerperioder: List<Periode<LocalDate>>,
    ): List<Kontroll> {
        val kontroller = mutableListOf<Kontroll>()

        aktiviteter.forEach { aktivitet ->
            val register = aktivitet.datoperiodeAktivitet
            if (register != null && register.inneholder(vedtaksperiode)) {
                // Har overlapp mellom register-data og vedtaksperiode
                // returnerer tom liste for finnKontrollerAktivitet
                return mutableListOf()
            }
            kontroller.addAll(kontrollerEndringerMotRegisterAktivitet(vedtaksperiode, aktivitet, register))
        }
        val kontrollMotAlleRegisterperioder = finnKontroller(vedtaksperiode, registerperioder)
        kontroller.addAll(kontrollMotAlleRegisterperioder)
        return kontroller.distinct()
    }

    /**
     * Kontrollerer om endring i registeraktivitet påvirker snittet av en [Vedtaksperiode] og [OppfølgingInngangsvilkårAktivitet]
     * En registeraktivitet kan ha endret seg, men det er ikke sikkert endringen påvirker vedtaksperioden
     * Hvis man har flere aktiviteter som løper parallellt og en av de
     *
     * @param registerperiode er trukket ut fra [aktivitet] men er not null
     */
    private fun kontrollerEndringerMotRegisterAktivitet(
        vedtaksperiode: Vedtaksperiode,
        aktivitet: OppfølgingInngangsvilkårAktivitet,
        registerperiode: Datoperiode?,
    ): List<Kontroll> {
        val snitt = vedtaksperiode.beregnSnitt(aktivitet)
        if (registerperiode != null && snitt != null) {
            // Kontrollerer om registeraktiviteten endret seg mott snittet av vedtaksperioden og aktiviteten
            return OppfølgingKontrollerUtil.finnEndringFomTom(snitt, registerperiode)
        }
        val finnerSnittMenManglerRegisteraktivitet =
            registerperiode == null && snitt != null && aktivitet.kildeId != null
        if (finnerSnittMenManglerRegisteraktivitet) {
            return listOf(Kontroll(ÅrsakKontroll.FINNER_IKKE_REGISTERAKTIVITET))
        }
        return emptyList()
    }

    private fun finnKontroller(
        vedtaksperiode: Vedtaksperiode,
        registerperioder: List<Periode<LocalDate>>,
    ): List<Kontroll> {
        val snitt = registerperioder.mapNotNull { vedtaksperiode.beregnSnitt(it) }.singleOrNull()

        if (snitt == null) {
            return listOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
        }
        if (snitt.fom == vedtaksperiode.fom && snitt.tom == vedtaksperiode.tom) {
            return emptyList() // Snitt er lik vedtaksperiode -> skal ikke kontrolleres
        }
        return OppfølgingKontrollerUtil.finnEndringFomTom(vedtaksperiode, snitt)
    }
}
