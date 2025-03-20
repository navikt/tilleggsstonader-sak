package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode as VedtaksperiodeLæremidler

object VedtaksperiodeValideringUtils {
    fun validerIngenEndringerFørRevurderFra(
        innsendteVedtaksperioder: List<PeriodeMedId>,
        vedtaksperioderForrigeBehandling: List<PeriodeMedId>?,
        revurderFra: LocalDate?,
    ) {
        if (revurderFra == null) return

        val innsendteVedtaksperioderFørRevurderFra = innsendteVedtaksperioder.filter { it.fom < revurderFra }
        val vedtaksperioderForrigeBehandlingFørRevurderFra =
            vedtaksperioderForrigeBehandling?.filter { it.fom < revurderFra }
        val innsendteVedtaksperioderMap = innsendteVedtaksperioderFørRevurderFra.associateBy { it.id }

        if (vedtaksperioderForrigeBehandlingFørRevurderFra.isNullOrEmpty()) {
            brukerfeilHvis(innsendteVedtaksperioder.any { it.fom < revurderFra }) {
                "Det er ikke tillat å legge til nye perioder før revurder fra dato"
            }
        } else {
            val vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom =
                vedtaksperioderForrigeBehandlingFørRevurderFra.map { vedtaksperiodeForrigeBehandling ->
                    val tilhørendeInnsendtVedtaksperiode =
                        innsendteVedtaksperioderMap[vedtaksperiodeForrigeBehandling.id]

                    if (tilhørendeInnsendtVedtaksperiode != null &&
                        // revurderFra.minusDays(1) tillater endringer dagen før revurder fra som trengs i opphør
                        tilhørendeInnsendtVedtaksperiode.tom >= revurderFra.minusDays(1) &&
                        vedtaksperiodeForrigeBehandling.tom >= revurderFra.minusDays(1)
                    ) {
                        vedtaksperiodeForrigeBehandling.kopier(
                            fom = vedtaksperiodeForrigeBehandling.fom,
                            tom = tilhørendeInnsendtVedtaksperiode.tom,
                        )
                    } else {
                        vedtaksperiodeForrigeBehandling
                    }
                }
            brukerfeilHvis(
                vedtaksperioderForrigeBehandlingFørRevurderFraMedOppdatertTom.erUlik(
                    innsendteVedtaksperioderFørRevurderFra,
                ),
            ) {
                "Det er ikke tillat å legge til, endre eller slette vedtaksperioder fra før revurder fra dato"
            }
        }
    }

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

    private fun List<PeriodeMedId>.erUlik(other: List<PeriodeMedId>) = this.tilSammenlikningsSet() != other.tilSammenlikningsSet()

    private fun List<PeriodeMedId>.tilSammenlikningsSet() =
        map {
            when (it) {
                is VedtaksperiodeLæremidler -> it.copy(status = VedtaksperiodeStatus.NY)
                else -> it
            }
        }.toSet()

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

        validerVedtaksperiodeErInnenfor18og67år(fødselsdato, vedtaksperiode)
    }

    private fun validerVedtaksperiodeErInnenfor18og67år(
        fødselsdato: LocalDate?,
        vedtaksperiode: Vedtaksperiode,
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
}
