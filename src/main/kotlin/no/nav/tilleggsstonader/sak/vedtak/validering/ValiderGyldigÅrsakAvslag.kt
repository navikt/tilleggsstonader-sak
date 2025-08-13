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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
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
        validerAvslagSomGjelderVilkårperioder(behandlingId, årsakerAvslag, stønadstype)
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

    private fun validerAvslagSomGjelderVilkårperioder(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        validerAvslagSomGjelderVilkårperioder(stønadstype, årsakerAvslag, vilkårperioder.målgrupper, Avslagskategori.MÅLGRUPPE)
        validerAvslagSomGjelderVilkårperioder(stønadstype, årsakerAvslag, vilkårperioder.aktiviteter, Avslagskategori.AKTIVITET)
    }

    private fun validerAvslagSomGjelderVilkårperioder(
        stønadstype: Stønadstype,
        årsakerAvslag: List<ÅrsakAvslag>,
        vilkårperioder: List<Vilkårperiode>,
        kategori: Avslagskategori,
    ) {
        val aktuelleÅrsaker = årsakerAvslag.intersect(gyldigeAvslagsårsaker(stønadstype, gjelder = kategori))
        if (aktuelleÅrsaker.isEmpty()) return
        val type =
            when (kategori) {
                Avslagskategori.MÅLGRUPPE -> "målgruppe"
                Avslagskategori.AKTIVITET -> "aktivitet"
                else -> error("Ukjent kategori for vilkårperioder: $kategori")
            }
        brukerfeilHvis(vilkårperioder.none { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med følgende årsaker uten å legge inn minst én $type som ikke er oppfylt: ${
                aktuelleÅrsaker.toList().formaterListe()
            }"
        }
    }

    private fun validerAvslagSomGjelderStønadsvilkår(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val stønadsvilkårÅrsaker = gyldigeAvslagsårsaker(stønadstype, gjelder = Avslagskategori.STØNADSVILKÅR)
        val aktuelleÅrsaker = årsakerAvslag.intersect(stønadsvilkårÅrsaker)
        if (aktuelleÅrsaker.isEmpty()) return
        val stønadsvilkår = vilkårService.hentVilkår(behandlingId)
        brukerfeilHvis(stønadsvilkår.none { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }) {
            "Kan ikke avslå med følgende årsaker uten minst ett ikke-oppfylt vilkår for ${stønadstype.visningsnavnStønadsvilkår()}: ${
                aktuelleÅrsaker.toList().formaterListe()
            }"
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
