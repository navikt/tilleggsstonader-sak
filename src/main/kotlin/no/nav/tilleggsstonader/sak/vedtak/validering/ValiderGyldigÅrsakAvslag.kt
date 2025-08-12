package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslagskategori
import no.nav.tilleggsstonader.sak.vedtak.domain.formaterListe
import no.nav.tilleggsstonader.sak.vedtak.domain.gyldigeAvslagsårsaker
import no.nav.tilleggsstonader.sak.vedtak.domain.gyldigeÅrsakerForStønadstype
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Component

@Component
class ValiderGyldigÅrsakAvslag(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {
    fun validerAvslagErGyldig(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        validerÅrsakerErGyldigeForStønadstype(årsakerAvslag, stønadstype)

        validerAvslagSomGjelderAktivitet(behandlingId, årsakerAvslag, stønadstype)
        validerAvslagSomGjelderMålgruppe(behandlingId, årsakerAvslag, stønadstype)
        validerAvslagSomGjelderStønadsvilkår(behandlingId, årsakerAvslag, stønadstype)
    }

    private fun validerÅrsakerErGyldigeForStønadstype(
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val ugyldigeÅrsaker = årsakerAvslag - gyldigeÅrsakerForStønadstype(stønadstype)

        feilHvis(ugyldigeÅrsaker.isNotEmpty()) {
            "Følgende avslagsårsaker er ikke gyldige for ${stønadstype.name}: ${ugyldigeÅrsaker.formaterListe()}"
        }
    }

    private fun validerAvslagSomGjelderMålgruppe(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val målgruppeÅrsaker = gyldigeAvslagsårsaker(forStønadstype = stønadstype, basertPå = Avslagskategori.MÅLGRUPPE)
        val aktuelleÅrsaker = årsakerAvslag.intersect(målgruppeÅrsaker)
        if (aktuelleÅrsaker.isEmpty()) return
        val målgrupper = vilkårperiodeService.hentVilkårperioder(behandlingId).målgrupper
        brukerfeilHvis(målgrupper.none { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med følgende årsaker uten å legge inn minst én målgruppe som ikke er oppfylt: ${aktuelleÅrsaker.toList().formaterListe()}"
        }
    }

    private fun validerAvslagSomGjelderAktivitet(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val aktivitetÅrsaker = gyldigeAvslagsårsaker(forStønadstype = stønadstype, basertPå = Avslagskategori.AKTIVITET)
        val aktuelleÅrsaker = årsakerAvslag.intersect(aktivitetÅrsaker)
        if (aktuelleÅrsaker.isEmpty()) return
        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter
        brukerfeilHvis(aktiviteter.none { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med følgende årsaker uten å legge minst én aktivitet der resultatet er 'ikke oppfylt': ${aktuelleÅrsaker.toList().formaterListe()}"
        }
    }

    private fun validerAvslagSomGjelderStønadsvilkår(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val stønadsvilkårÅrsaker = gyldigeAvslagsårsaker(forStønadstype = stønadstype, basertPå = Avslagskategori.STØNADSVILKÅR)
        val aktuelleÅrsaker = årsakerAvslag.intersect(stønadsvilkårÅrsaker)
        if (aktuelleÅrsaker.isEmpty()) return
        val stønadsvilkår = vilkårService.hentVilkår(behandlingId)
        brukerfeilHvis(stønadsvilkår.none { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }) {
            "Kan ikke avslå med følgende årsaker uten minst ett ikke-oppfylt vilkår for ${stønadstype.visningsnavnStønadsvilkår()}: ${aktuelleÅrsaker.toList().formaterListe()}"
        }
    }
}

fun Stønadstype.visningsnavnStønadsvilkår() =
    when (this) {
        Stønadstype.BARNETILSYN -> "pass barn"
        Stønadstype.BOUTGIFTER -> "bolig/overnatting"
        Stønadstype.DAGLIG_REISE_TSO -> TODO()
        Stønadstype.DAGLIG_REISE_TSR -> TODO()
        Stønadstype.LÆREMIDLER -> error("læremidler har ikke stønadsvilkår")
    }
