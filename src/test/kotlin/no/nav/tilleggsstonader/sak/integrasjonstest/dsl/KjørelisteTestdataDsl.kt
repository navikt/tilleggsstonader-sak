package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import java.time.LocalDate

@BehandlingTestdataDslMarker
class KjørelisteTestdataDsl {
    internal var periode: Datoperiode? = null
    internal var kjørteDager: List<Pair<LocalDate, Int>> = mutableListOf() // dato med parkeringsutgifter
    internal var reiseIdProvider: ((List<VilkårDagligReiseDto>) -> ReiseId)? = { it.first().reiseId }

    fun reiseId(provider: (List<VilkårDagligReiseDto>) -> ReiseId) {
        reiseIdProvider = provider
    }

    internal fun build(reiserMedPrivatBil: List<VilkårDagligReiseDto>): KjørelisteSkjema {
        feilHvis(periode == null) {
            "Periode må være satt for å opprette kjøreliste"
        }

        val reiseId = reiseIdProvider!!.invoke(reiserMedPrivatBil)
        val dagerKjørt =
            kjørteDager.map { (dato, parkeringsutgift) ->
                KjørelisteSkjemaUtil.KjørtDag(dato, parkeringsutgift)
            }

        return KjørelisteSkjemaUtil.kjørelisteSkjema(
            reiseId = reiseId.toString(),
            periode = periode!!,
            dagerKjørt = dagerKjørt,
        )
    }
}
