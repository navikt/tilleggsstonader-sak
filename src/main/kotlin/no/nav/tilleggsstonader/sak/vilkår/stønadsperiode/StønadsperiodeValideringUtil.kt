package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.formattertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.mergeSammenhengendeOppfylteVilkårperioder
import java.time.LocalDate

object StønadsperiodeValideringUtil {

    /**
     * Validering av stønadsperioder som kjøres når man endrer vilkårperiode trenger ikke å validere fødselsdatoet.
     * Det er tilstrekkelig at det gjøres vid validering av stønadsperioder.
     */
    fun validerStønadsperioderVedEndringAvVilkårperiode(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
    ) = validerStønadsperioder(stønadsperioder, vilkårperioder, null)

    /**
     * @param fødselsdato er nullable då alle behandlinger ikke har [fødselsdato] i grunnlagsdata fra før
     */
    fun validerStønadsperioder(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
        fødselsdato: LocalDate?,
    ) {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteVilkårperioder()

        stønadsperioder.forEach { validerStønadsperiode(it, målgrupper, aktiviteter, fødselsdato) }
    }

    private fun validerIkkeOverlappendeStønadsperioder(stønadsperioder: List<StønadsperiodeDto>) {
        stønadsperioder.sortedBy { it.fom }.fold(listOf<StønadsperiodeDto>()) { acc, periode ->
            val last = acc.lastOrNull()
            if (last != null) {
                brukerfeilHvis(last.tom >= periode.fom) {
                    "Stønadsperiode ${last.formattertPeriodeNorskFormat()} og ${periode.formattertPeriodeNorskFormat()} overlapper"
                }
            }
            acc + periode
        }
    }

    fun validerStønadsperiode(
        stønadsperiode: StønadsperiodeDto,
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        fødselsdato: LocalDate?,
    ) {
        brukerfeilHvisIkke(stønadsperiode.målgruppe.gyldigeAktiviter.contains(stønadsperiode.aktivitet)) {
            "Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig"
        }

        validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
            målgruppePerioderPerType + aktivitetPerioderPerType,
            stønadsperiode,
        )

        val målgrupper = målgruppePerioderPerType[stønadsperiode.målgruppe]
            ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
        val aktiviteter = aktivitetPerioderPerType[stønadsperiode.aktivitet]
            ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt")

        målgrupper.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.målgruppe} i perioden ${stønadsperiode.formattertPeriodeNorskFormat()}")
        aktiviteter.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.aktivitet} i perioden ${stønadsperiode.formattertPeriodeNorskFormat()}")

        validerStønadsperiodeErInnenfor18og67år(fødselsdato, stønadsperiode)
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        stønadsperiode: StønadsperiodeDto,
    ) {
        målgruppePerioderPerType.entries
            .filter { it.key.girIkkeRettPåStønadsperiode() }
            .forEach { (type, perioder) ->
                perioder.firstOrNull { it.overlapper(stønadsperiode) }?.let {
                    brukerfeil(
                        "Stønadsperiode ${stønadsperiode.formattertPeriodeNorskFormat()} overlapper " +
                            "med $type(${it.formattertPeriodeNorskFormat()}) som ikke gir rett på stønad",
                    )
                }
            }
    }

    private fun validerStønadsperiodeErInnenfor18og67år(
        fødselsdato: LocalDate?,
        stønadsperiode: StønadsperiodeDto,
    ) {
        if (fødselsdato != null && stønadsperiode.målgruppe.gjelderNedsattArbeidsevne()) {
            val dato18år = fødselsdato.plusYears(18)
            brukerfeilHvis(stønadsperiode.fom < dato18år) {
                "Periode kan ikke begynne før søker fyller 18 år (${dato18år.norskFormat()})"
            }
            val dato67år = fødselsdato.plusYears(67)
            brukerfeilHvis(stønadsperiode.tom >= dato67år) {
                "Periode kan ikke slutte etter søker fylt 67 år (${dato67år.norskFormat()})"
            }
        }
    }
}
