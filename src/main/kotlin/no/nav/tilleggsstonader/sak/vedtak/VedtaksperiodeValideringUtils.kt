package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
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
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val perioderSomIkkeGirRett =
            (vilkårperioder.målgrupper + vilkårperioder.aktiviteter)
                .filter { it.type.girIkkeRettPåStønadsperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
        vedtaksperioder.forEach { validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(perioderSomIkkeGirRett, it) }
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        vilkårperioder: List<Vilkårperiode>,
        vedtaksperiode: Vedtaksperiode,
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
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
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
}
