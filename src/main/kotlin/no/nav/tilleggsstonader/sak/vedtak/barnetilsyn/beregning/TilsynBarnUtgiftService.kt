package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningMåned
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class TilsynBarnUtgiftService(
    private val vilkårService: VilkårService,
) {
    fun hentUtgifterTilBeregning(behandlingId: BehandlingId): Map<BarnId, List<UtgiftBeregningMåned>> =
        vilkårService
            .hentOppfyltePassBarnVilkår(behandlingId)
            .groupBy { it.barnId ?: error("Vilkår=${it.id} type=${it.type} for tilsyn barn mangler barnId") }
            .mapValues { (_, values) -> values.map { mapUtgiftBeregning(it) } }

    private fun mapUtgiftBeregning(it: Vilkår): UtgiftBeregningMåned {
        val fom = it.fom
        val tom = it.tom
        val utgift = it.utgift
        feilHvis(fom == null || tom == null || utgift == null) {
            "Forventer at fra-dato, til-dato og utgift er satt. Gå tilbake til Pass barn-fanen, og legg til datoer og utgifter der. For utviklerteamet: dette gjelder vilkår=${it.id}."
        }
        feilHvisIkke(fom.erFørsteDagIMåneden()) {
            "Noe er feil. Fom skal være satt til første dagen i måneden"
        }
        feilHvisIkke(tom.erSisteDagIMåneden()) {
            "Noe er feil. Tom skal være satt til siste dagen i måneden"
        }
        return UtgiftBeregningMåned(
            fom = YearMonth.from(fom),
            tom = YearMonth.from(tom),
            utgift = utgift,
        )
    }
}
