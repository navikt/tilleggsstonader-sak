package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.oppfylteSvarOffentligtransport
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.util.UUID.randomUUID

fun mapBeregningsresultatForPeriode(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        BeregningsresultatForPeriode(
            grunnlag =
                BeregningsgrunnlagOffentligTransport(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    prisEnkeltbillett = 0,
                    prisSyvdagersbillett = 0,
                    pris30dagersbillett = 0,
                    antallReisedagerPerUke = 0,
                    antallReisedager = 0,
                    vedtaksperioder = emptyList(),
                ),
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
            billettdetaljer =
                mapOf(
                    Billettype.ENKELTBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.ENKELTBILLETT_ANTALL, rad) ?: 0),
                    Billettype.SYVDAGERSBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.SYVDAGERSBILLETT_ANTALL, rad) ?: 0),
                    Billettype.TRETTIDAGERSBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.TRETTIDAGERSBILLETT_ANTALL, rad) ?: 0),
                ).filterValues { it > 0 },
        )
    }

fun mapBeregningsresultatForPeriodeRevurdering(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        BeregningsresultatForPeriode(
            grunnlag =
                BeregningsgrunnlagOffentligTransport(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    prisEnkeltbillett = 0,
                    prisSyvdagersbillett = 0,
                    pris30dagersbillett = 0,
                    antallReisedagerPerUke = 0,
                    antallReisedager = 0,
                    vedtaksperioder =
                        listOf(
                            VedtaksperiodeGrunnlag(
                                id = randomUUID(),
                                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                                aktivitet = AktivitetType.TILTAK,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                antallReisedagerIVedtaksperioden = 0,
                            ),
                        ),
                ),
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
            billettdetaljer =
                mapOf(
                    Billettype.ENKELTBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.ENKELTBILLETT_ANTALL, rad) ?: 0),
                    Billettype.SYVDAGERSBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.SYVDAGERSBILLETT_ANTALL, rad) ?: 0),
                    Billettype.TRETTIDAGERSBILLETT to
                        (parseValgfriInt(DomenenøkkelFelles.TRETTIDAGERSBILLETT_ANTALL, rad) ?: 0),
                ).filterValues { it > 0 },
        )
    }

fun dummyBehandling(
    behandlingId: BehandlingId,
    steg: StegType = StegType.BEREGNE_YTELSE,
    forrigeIverksatteBehandlingId: BehandlingId? = null,
): Saksbehandling =
    saksbehandling(
        id = behandlingId,
        steg = steg,
        fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
        forrigeIverksatteBehandlingId = null,
        type = if (forrigeIverksatteBehandlingId != null) BehandlingType.REVURDERING else BehandlingType.FØRSTEGANGSBEHANDLING,
    )

fun mapTilVilkårDagligReise(rad: Map<String, String>): LagreDagligReise =
    LagreDagligReise(
        fom = parseDato(DomenenøkkelFelles.FOM, rad),
        tom = parseDato(DomenenøkkelFelles.TOM, rad),
        svar = oppfylteSvarOffentligtransport,
        fakta =
            FaktaOffentligTransport(
                reisedagerPerUke =
                    parseInt(
                        DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE,
                        rad,
                    ),
                prisEnkelbillett = parseValgfriInt(DomenenøkkelOffentligtransport.PRIS_ENKELTBILLETT, rad),
                prisSyvdagersbillett = parseValgfriInt(DomenenøkkelOffentligtransport.PRIS_SYV_DAGERS_BILLETT, rad),
                prisTrettidagersbillett =
                    parseValgfriInt(
                        DomenenøkkelOffentligtransport.PRIS_TRETTI_DAGERS_BILLETT,
                        rad,
                    ),
            ),
    )

fun mapAktiviteter(
    behandlingId: BehandlingId,
    dataTable: DataTable,
) = dataTable.mapRad { rad ->
    aktivitet(
        behandlingId = behandlingId,
        fom = parseDato(DomenenøkkelFelles.FOM, rad),
        tom = parseDato(DomenenøkkelFelles.TOM, rad),
        faktaOgVurdering =
            faktaOgVurderingAktivitetTilsynBarn(
                type =
                    parseValgfriEnum<AktivitetType>(DomenenøkkelFelles.AKTIVITET, rad)
                        ?: AktivitetType.TILTAK,
            ),
    )
}

fun mapMålgrupper(
    behandlingId: BehandlingId,
    dataTable: DataTable,
) = dataTable.mapRad { rad ->
    målgruppe(
        behandlingId = behandlingId,
        fom = parseDato(DomenenøkkelFelles.FOM, rad),
        tom = parseDato(DomenenøkkelFelles.TOM, rad),
        faktaOgVurdering =
            faktaOgVurderingMålgruppe(
                type = parseValgfriEnum<MålgruppeType>(DomenenøkkelFelles.MÅLGRUPPE, rad) ?: MålgruppeType.AAP,
            ),
        begrunnelse = "begrunnelse",
    )
}
