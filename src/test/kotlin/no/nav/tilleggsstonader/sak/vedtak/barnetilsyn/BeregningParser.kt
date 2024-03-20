package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.util.UUID

enum class BeregningNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    MÅNED("Måned"),
    ANTALL_DAGER("Antall dager"),
    ANTALL_BARN("Antall barn"),
    UTGIFT("Utgift"),
    DAGSATS("Dagsats"),
    MÅNEDSBELØP("Månedsbeløp"),
    MAKSSATS("Makssats"),
    AKTIVITET("Aktivitet"),
    MÅLGRUPPE("Målgruppe"),
    AKTIVITETSDAGER("Aktivitetsdager"),
    ANTALL_AKTIVITETER("Antall aktiviteter"),
    FOM_UKE("Fom uke"),
    TOM_UKE("Tom uke"),
}

fun mapStønadsperioder(behandlingId: UUID, dataTable: DataTable) = dataTable.mapRad { rad ->
    Stønadsperiode(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        målgruppe = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
        aktivitet = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
            ?: AktivitetType.TILTAK,
    )
}

fun mapAktiviteter(behandlingId: UUID, dataTable: DataTable) = dataTable.mapRad { rad ->
    aktivitet(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        type = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
            ?: AktivitetType.TILTAK,
        aktivitetsdager = parseInt(BeregningNøkler.AKTIVITETSDAGER, rad),
    )
}
