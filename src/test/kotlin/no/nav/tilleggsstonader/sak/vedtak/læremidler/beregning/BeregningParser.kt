package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

fun mapAktiviteter(
    behandlingId: BehandlingId,
    dataTable: DataTable,
) = dataTable.mapRad { rad ->
    aktivitet(
        behandlingId = behandlingId,
        fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
        tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        faktaOgVurdering =
            faktaOgVurderingAktivitetLæremidler(
                type =
                    parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad)
                        ?: AktivitetType.TILTAK,
                prosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
                studienivå =
                    parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad)
                        ?: Studienivå.HØYERE_UTDANNING,
            ),
    )
}

fun mapBeregningsresultat(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        BeregningsresultatForMåned(
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
            grunnlag =
                Beregningsgrunnlag(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    studienivå = parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad) ?: Studienivå.HØYERE_UTDANNING,
                    studieprosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
                    sats = parseBigDecimal(BeregningNøkler.SATS, rad).toInt(),
                    satsBekreftet = parseValgfriBoolean(BeregningNøkler.BEKREFTET_SATS, rad) ?: true,
                    utbetalingsdato = parseDato(BeregningNøkler.UTBETALINGSDATO, rad),
                    målgruppe = parseValgfriEnum<MålgruppeType>(BeregningNøkler.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
                    aktivitet = parseValgfriEnum<AktivitetType>(BeregningNøkler.AKTIVITET, rad) ?: AktivitetType.TILTAK,
                ),
        )
    }
