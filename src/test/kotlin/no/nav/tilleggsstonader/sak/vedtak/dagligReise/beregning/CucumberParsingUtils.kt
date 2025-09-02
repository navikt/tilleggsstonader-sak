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
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.OffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.oppfylteDelvilkårDagligReiseOffentligTransport

fun mapBeregningsresultatForPeriode(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        BeregningsresultatForPeriode(
            grunnlag =
                Beregningsgrunnlag(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    prisEnkeltbillett = 0,
                    pris30dagersbillett = 0,
                    antallReisedagerPerUke = 0,
                    antallReisedager = 0,
                    vedtaksperioder = emptyList(),
                ),
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
        )
    }

fun dummyBehandling(
    behandlingId: BehandlingId,
    steg: StegType = StegType.BEREGNE_YTELSE,
): Saksbehandling =
    saksbehandling(
        id = behandlingId,
        steg = steg,
        fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
        forrigeIverksatteBehandlingId = null,
        type = BehandlingType.FØRSTEGANGSBEHANDLING,
    )

fun mapTilVilkår(
    rad: Map<String, String>,
    behandlingId: BehandlingId,
): OpprettVilkårDto =
    OpprettVilkårDto(
        vilkårType = VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT,
        barnId = null,
        behandlingId = behandlingId,
        delvilkårsett = oppfylteDelvilkårDagligReiseOffentligTransport().map { it.tilDto() },
        fom = parseDato(DomenenøkkelFelles.FOM, rad),
        tom = parseDato(DomenenøkkelFelles.TOM, rad),
        utgift = null,
        erFremtidigUtgift = false,
        offentligTransport =
            OffentligTransport(
                reisedagerPerUke =
                    parseInt(
                        DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE,
                        rad,
                    ),
                prisEnkelbillett = parseInt(DomenenøkkelOffentligtransport.PRIS_ENKELTBILLETT, rad),
                prisTrettidagersbillett =
                    parseInt(
                        DomenenøkkelOffentligtransport.PRIS_TRETTI_DAGERS_BILLETT,
                        rad,
                    ),
            ),
    )
