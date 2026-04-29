package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto

@BehandlingTestdataDslMarker
class KjørelisteTestdataDsl {
    internal var periode: Datoperiode? = null
    internal var kjørteDager: List<KjørtDag> = mutableListOf()
    internal var reiseIdProvider: ((List<VilkårDagligReiseDto>) -> ReiseId)? = { it.first().reiseId }

    fun reiseId(provider: (List<VilkårDagligReiseDto>) -> ReiseId) {
        reiseIdProvider = provider
    }

    internal fun build(reiserMedPrivatBil: List<VilkårDagligReiseDto>): KjørelisteSkjema {
        feilHvis(periode == null) {
            "Periode må være satt for å opprette kjøreliste"
        }

        val reiseId = reiseIdProvider!!.invoke(reiserMedPrivatBil)

        return KjørelisteSkjemaUtil.kjørelisteSkjema(
            reiseId = reiseId.toString(),
            periode = periode!!,
            dagerKjørt = kjørteDager,
        )
    }
}
