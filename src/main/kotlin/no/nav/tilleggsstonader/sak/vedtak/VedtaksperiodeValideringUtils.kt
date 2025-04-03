package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

object VedtaksperiodeValideringUtils {
    fun validerVedtaksperioderEksisterer(vedtaksperioder: List<Vedtaksperiode>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
        }
    }

    fun validerIngenOverlappMellomVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        brukerfeilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder kan ikke overlappe"
        }
    }

    /**
     * Vedtaksperioder kan ikke overlappe med vilkårperioder som ikke gir rett på stønad,
     * eks 100% sykepenger eller INGEN_MÅLGRUPPE
     */
    fun validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
        vilkårperioder: Vilkårperioder,
        vedtaksperioder: List<Periode<LocalDate>>,
    ) {
        val perioderSomIkkeGirRett =
            (vilkårperioder.målgrupper + vilkårperioder.aktiviteter)
                .filter { it.type.girIkkeRettPåStønadsperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
        vedtaksperioder.forEach { validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(perioderSomIkkeGirRett, it) }
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        vilkårperioder: List<Vilkårperiode>,
        vedtaksperiode: Periode<LocalDate>,
    ) {
        vilkårperioder
            .firstOrNull { vilkårperiode -> vilkårperiode.overlapper(vedtaksperiode) }
            ?.let {
                brukerfeil(
                    "Vedtaksperiode ${vedtaksperiode.formatertPeriodeNorskFormat()} overlapper " +
                        "med ${it.type}(${it.formatertPeriodeNorskFormat()}) som ikke gir rett på stønad",
                )
            }
    }

    fun validerEnkeltperiode(
        vedtaksperiode: Vedtaksperiode,
        målgruppePerioderPerType: Map<MålgruppeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<AktivitetType, List<Datoperiode>>,
        fødselsdato: LocalDate?,
    ) {
        brukerfeilHvisIkke(vedtaksperiode.målgruppe.gyldigeAktiviter.contains(vedtaksperiode.aktivitet)) {
            "Kombinasjonen av ${vedtaksperiode.målgruppe} og ${vedtaksperiode.aktivitet} er ikke gyldig"
        }

        val målgrupper =
            målgruppePerioderPerType[vedtaksperiode.målgruppe]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for ${vedtaksperiode.målgruppe} er oppfylt")
        val aktiviteter =
            aktivitetPerioderPerType[vedtaksperiode.aktivitet]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for ${vedtaksperiode.aktivitet} er oppfylt")

        målgrupper.firstOrNull { it.inneholder(vedtaksperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for ${vedtaksperiode.målgruppe} i perioden ${vedtaksperiode.formatertPeriodeNorskFormat()}",
            )
        aktiviteter.firstOrNull { it.inneholder(vedtaksperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for ${vedtaksperiode.aktivitet} i perioden ${vedtaksperiode.formatertPeriodeNorskFormat()}",
            )
    }

    /**
     * Kopi av [validerEnkeltperiode] over for å validere mot faktiske målgrupper
     */
    fun validerEnkeltperiode(
        vedtaksperiode: no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode,
        målgruppePerioderPerType: Map<FaktiskMålgruppe, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<AktivitetType, List<Datoperiode>>,
    ) {
        val målgruppe = vedtaksperiode.målgruppe ?: error("Vedtaksperiode mangler målgruppe")
        val aktivitet = vedtaksperiode.aktivitet ?: error("Vedtaksperiode mangler aktivitet")
        brukerfeilHvisIkke(målgruppe.gyldigeAktiviter.contains(aktivitet)) {
            "Kombinasjonen av $målgruppe og $aktivitet er ikke gyldig"
        }

        val målgrupper =
            målgruppePerioderPerType[målgruppe]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for $målgruppe er oppfylt")
        val aktiviteter =
            aktivitetPerioderPerType[aktivitet]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for $aktivitet er oppfylt")

        målgrupper.firstOrNull { it.inneholder(vedtaksperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for $målgruppe i perioden ${vedtaksperiode.formatertPeriodeNorskFormat()}",
            )
        aktiviteter.firstOrNull { it.inneholder(vedtaksperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for $aktivitet i perioden ${vedtaksperiode.formatertPeriodeNorskFormat()}",
            )
    }
}
