package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.EksempelRegel
import java.time.LocalDate
import java.time.YearMonth

object VilkårTestUtils {

    fun opprettVilkårsvurderinger(
        behandling: Behandling,
        barn: List<BehandlingBarn>,
        fom: LocalDate? = YearMonth.now().atDay(1),
        tom: LocalDate? = YearMonth.now().atEndOfMonth(),
        status: VilkårStatus = VilkårStatus.NY,
    ): List<Vilkår> {
        val hovedregelMetadata =
            HovedregelMetadata(
                barn = barn,
                behandling = mockk(),
            )
        val delvilkårsett = EksempelRegel().initiereDelvilkår(hovedregelMetadata)
        return listOf(
            vilkår(
                fom = fom,
                tom = tom,
                status = status,
                resultat = Vilkårsresultat.OPPFYLT,
                type = VilkårType.PASS_BARN,
                behandlingId = behandling.id,
                barnId = barn.first().id,
                delvilkår = delvilkårsett,
            ),
        )
    }

    fun List<String>.tilBehandlingBarn(behandling: Behandling) =
        this.map { behandlingBarn(behandlingId = behandling.id, personIdent = it) }
}