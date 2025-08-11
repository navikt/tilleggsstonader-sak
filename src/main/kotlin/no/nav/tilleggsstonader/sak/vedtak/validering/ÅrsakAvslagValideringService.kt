package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.formaterListe
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Service

@Service
class ÅrsakAvslagValideringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {
    fun validerAvslagErGyldig(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        validerAvslagÅrsakAktivitet(behandlingId, årsakerAvslag)
        if (ÅrsakAvslag.IKKE_I_MÅLGRUPPE in årsakerAvslag) {
            validerAvslagGrunnetIkkeIMålgruppe(behandlingId)
        }
        validerAvslagPåStønadsvilkår(behandlingId, årsakerAvslag, stønadstype)
    }

    private fun validerAvslagGrunnetIkkeIMålgruppe(behandlingId: BehandlingId) {
        val målgrupper = vilkårperiodeService.hentVilkårperioder(behandlingId).målgrupper

        brukerfeilHvis(målgrupper.none { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med årsak '${ÅrsakAvslag.IKKE_I_MÅLGRUPPE.visningsnavn()}' uten å legge inn minst én målgruppe som ikke er oppfylt."
        }
    }

    private fun validerAvslagÅrsakAktivitet(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
    ) {
        val årsakerKnyttetTilAktivitet =
            listOf(
                ÅrsakAvslag.INGEN_AKTIVITET,
                ÅrsakAvslag.HAR_IKKE_UTGIFTER,
                ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND,
            )

        val aktuelleÅrsaker = årsakerAvslag.intersect(årsakerKnyttetTilAktivitet)

        if (aktuelleÅrsaker.isEmpty()) return

        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter

        brukerfeilHvis(aktiviteter.none { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med disse årsakene uten å legge minst én aktivitet der resultatet er 'ikke oppfylt': ${årsakerAvslag.formaterListe()}"
        }
    }

    private fun validerAvslagPåStønadsvilkår(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
        stønadstype: Stønadstype,
    ) {
        val årsakerBasertPåStønadsvilkår =
            when (stønadstype) {
                Stønadstype.BARNETILSYN -> listOf(ÅrsakAvslag.HAR_IKKE_UTGIFTER, ÅrsakAvslag.MANGELFULL_DOKUMENTASJON)
                Stønadstype.BOUTGIFTER ->
                    listOf(
                        ÅrsakAvslag.MANGELFULL_DOKUMENTASJON,
                        ÅrsakAvslag.HAR_IKKE_MERUTGIFTER,
                        ÅrsakAvslag.RETT_TIL_BOSTØTTE,
                    )

                Stønadstype.LÆREMIDLER -> return
                Stønadstype.DAGLIG_REISE_TSO -> TODO()
                Stønadstype.DAGLIG_REISE_TSR -> TODO()
            }

        val aktuelleÅrsaker = årsakerAvslag.intersect(årsakerBasertPåStønadsvilkår)

        if (aktuelleÅrsaker.isEmpty()) return

        val stønadsvilkår = vilkårService.hentVilkår(behandlingId)

        brukerfeilHvis(stønadsvilkår.none { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }) {
            "Kan ikke avslå med disse årsakene uten minst ett ikke-oppfylt vilkår for ${stønadstype.visningsnavnStønadsvilkår()}: ${årsakerAvslag.formaterListe()}"
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
