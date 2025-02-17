package no.nav.tilleggsstonader.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

object RegisteraktivitetCucumberUtil {
    enum class StønadsperiodeNøkler(
        override val nøkkel: String,
    ) : Domenenøkkel {
        AKTIVITET("Aktivitet"),
        MÅLGRUPPE("Målgruppe"),
    }

    fun mapStønadsperioder(
        behandlingId: BehandlingId,
        dataTable: DataTable,
    ) = dataTable.mapRad { rad ->
        Stønadsperiode(
            behandlingId = behandlingId,
            fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
            tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
            målgruppe = parseValgfriEnum<MålgruppeType>(StønadsperiodeNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            aktivitet =
                parseValgfriEnum<AktivitetType>(StønadsperiodeNøkler.AKTIVITET, rad)
                    ?: AktivitetType.TILTAK,
            status = StønadsperiodeStatus.NY,
        )
    }
}
