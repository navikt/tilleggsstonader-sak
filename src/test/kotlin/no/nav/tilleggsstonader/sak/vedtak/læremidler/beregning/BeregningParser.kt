package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

fun mapAktiviteter(behandlingId: BehandlingId, dataTable: DataTable) = dataTable.mapRad { rad ->
    aktivitet(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(
            type = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                ?: AktivitetType.TILTAK,
            prosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
            studienivå = parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad)
                ?: Studienivå.HØYERE_UTDANNING,

        ),
    )
}
