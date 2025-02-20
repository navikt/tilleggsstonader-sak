package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

object TilsynBarnVedtaksperiodeValideringUtils {
    fun validerVedtaksperioderEksisterer(vedtaksperioder: List<VedtaksperiodeBeregning>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
        }
    }

    fun validerIngenOverlappMellomVedtaksperioder(vedtaksperioder: List<VedtaksperiodeBeregning>) {
        brukerfeilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder kan ikke overlappe"
        }
    }

    fun validerUtgiftHeleVedtaksperioden(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
        brukerfeilHvisIkke(
            erUtgiftperiodeSomInneholderVedtaksperiode(
                vedtaksperioder = vedtaksperioder,
                utgifter = utgifter,
            ),
        ) {
            "Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden"
        }
    }

    private fun erUtgiftperiodeSomInneholderVedtaksperiode(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ): Boolean {
        val sammenslåtteUtgiftPerioder =
            utgifter.values
                .flatMap {
                    it.map { Datoperiode(fom = it.fom.atDay(1), tom = it.tom.atEndOfMonth()) }
                }.sorted()
                .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

        return vedtaksperioder.any { vedtaksperiode ->
            sammenslåtteUtgiftPerioder.any { utgiftPeriode -> utgiftPeriode.inneholder(vedtaksperiode) }
        }
    }

    /**
     * Vedtaksperioder kan ikke overlappe med vilkårperioder som ikke gir rett på stønad,
     * eks 100% sykepenger eller INGEN_MÅLGRUPPE
     */
    fun validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
        vilkårperioder: Vilkårperioder,
        vedtaksperioder: List<VedtaksperiodeBeregning>,
    ) {
        val perioderSomIkkeGirRett =
            (vilkårperioder.målgrupper + vilkårperioder.aktiviteter)
                .filter { it.type.girIkkeRettPåStønadsperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
        vedtaksperioder.forEach { validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(perioderSomIkkeGirRett, it) }
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        vilkårperioder: List<Vilkårperiode>,
        vedtaksperiode: VedtaksperiodeBeregning,
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
        vedtaksperiode: VedtaksperiodeBeregning,
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

        validerVedtaksperiodeErInnenfor18og67år(fødselsdato, vedtaksperiode)
    }

    private fun validerVedtaksperiodeErInnenfor18og67år(
        fødselsdato: LocalDate?,
        vedtaksperiode: VedtaksperiodeBeregning,
    ) {
        if (fødselsdato != null && vedtaksperiode.målgruppe.gjelderNedsattArbeidsevne()) {
            val dato18år = fødselsdato.plusYears(18)
            brukerfeilHvis(vedtaksperiode.fom < dato18år) {
                "Periode kan ikke begynne før søker fyller 18 år (${dato18år.norskFormat()})"
            }
            val dato67år = fødselsdato.plusYears(67)
            brukerfeilHvis(vedtaksperiode.tom >= dato67år) {
                "Periode kan ikke slutte etter søker fylt 67 år (${dato67år.norskFormat()})"
            }
        }
    }

    fun validerIngenEndringerFørRevurderFra(
        vedtaksperioder: List<Vedtaksperiode>,
        vedtaksperioderForrigeBehandling: List<Vedtaksperiode>?,
        revurderFra: LocalDate?,
    ) {
        if (revurderFra == null) return

        val vedtaksperioderFørRevurderFra = vedtaksperioder.filter { it.fom < revurderFra }
        val vedtaksperioderForrigeBehandlingFørRevurderFra =
            vedtaksperioderForrigeBehandling?.filter { it.fom < revurderFra }
        val vedtaksperioderMap = vedtaksperioderFørRevurderFra.associateBy { it.id }

        if (vedtaksperioderForrigeBehandlingFørRevurderFra.isNullOrEmpty()) {
            brukerfeilHvis(vedtaksperioder.any { it.fom < revurderFra }) {
                "Det er ikke tillat å legge til nye perioder før revurder fra dato"
            }
        } else {
            val vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom =
                vedtaksperioderForrigeBehandlingFørRevurderFra.map { vedtaksperiodeForrigeBehandling ->
                    val nyVedtaksperiode = vedtaksperioderMap[vedtaksperiodeForrigeBehandling.id]

                    if (nyVedtaksperiode != null &&
                        nyVedtaksperiode.tom > vedtaksperiodeForrigeBehandling.tom &&
                        vedtaksperiodeForrigeBehandling.tom > revurderFra
                    ) {
                        vedtaksperiodeForrigeBehandling.copy(tom = nyVedtaksperiode.tom)
                    } else {
                        vedtaksperiodeForrigeBehandling
                    }
                }
            brukerfeilHvis(vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom != vedtaksperioderFørRevurderFra) {
                "Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato"
            }
        }
    }
}
