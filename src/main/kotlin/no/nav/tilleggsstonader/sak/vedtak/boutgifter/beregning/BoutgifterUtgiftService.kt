package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.springframework.stereotype.Service

@Service
class BoutgifterUtgiftService(
    private val vilkårService: VilkårService,
) {
    fun hentUtgifterTilBeregning(behandlingId: BehandlingId): BoutgifterPerUtgiftstype =
        vilkårService
            .hentOppfylteBoutgiftVilkår(behandlingId)
            .groupBy { TypeBoutgift.fraVilkårType(it.type) }
            .mapValues { (_, values) -> values.map { it.tilUtgiftBeregning() } }

    private fun Vilkår.tilUtgiftBeregning(): UtgiftBeregningBoutgifter {
        feilHvis(fom == null || tom == null || utgift == null) {
            "Forventer at fra-dato, til-dato og utgift er satt. Gå tilbake til Vilkår-fanen, og legg til datoer og utgifter der. For utviklerteamet: dette gjelder vilkår=$id."
        }
        feilHvis(type.erLøpendeUtgifterBo() && !fom.erFørsteDagIMåneden()) {
            "Noe er feil. Fom skal være satt til første dagen i måneden"
        }
        feilHvis(type.erLøpendeUtgifterBo() && !tom.erSisteDagIMåneden()) {
            "Noe er feil. Tom skal være satt til siste dagen i måneden"
        }
        return UtgiftBeregningBoutgifter(
            fom = fom,
            tom = tom,
            utgift = utgift,
            skalFåDekketFaktiskeUtgifter = skalFåDekketFaktiskeUtgifter(),
        )
    }

    private fun Vilkår.skalFåDekketFaktiskeUtgifter(): Boolean =
        this.delvilkårsett
            .firstOrNull { it.hovedregel == RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER }
            ?.let { delvilkår ->
                delvilkår.vurderinger
                    .single { it.regelId == RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER }
                    .svar == SvarId.JA
            } ?: false
}
