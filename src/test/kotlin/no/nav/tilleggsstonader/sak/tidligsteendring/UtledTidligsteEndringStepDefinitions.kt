package no.nav.tilleggsstonader.sak.tidligsteendring

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriString
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat

enum class TidligsteEndringFellesNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STATUS("Status"),
    TYPE("Type"),
    RESULTAT("Resultat"),
}

enum class VilkårperiodeNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STØNADSTYPE("Stønadstype"),
    AKTIVITETSDAGER("Aktivitetsdager"),
    TYPE("Type"),
    STUDIENIVÅ("Studienivå"),
    STUDIEPROSENT("Studieprosent"),
    KILDE_ID("Kilde Id"),
}

enum class VilkårNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    UTGIFT("Utgift"),
    ER_FREMTIDIG_UTGIFT("Er fremtidig utgift"),
    BARN_ID("BarnId"),
}

enum class VedtaksperiodeNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    AKTIVITET("Aktivitet"),
    MÅLGRUPPE("Målgruppe"),
}

@Suppress("unused", "ktlint:standard:function-naming")
class UtledTidligsteEndringStepDefinitions {
    var behandlingId = BehandlingId.random()
    var behandling = behandling(id = behandlingId)

    var vilkår = emptyList<Vilkår>()
    var vilkårForrigeBehandling = emptyList<Vilkår>()

    var aktiviteter = emptyList<GeneriskVilkårperiode<AktivitetFaktaOgVurdering>>()
    var aktiviteterForrigeBehandling = emptyList<GeneriskVilkårperiode<AktivitetFaktaOgVurdering>>()

    var målgrupper = emptyList<GeneriskVilkårperiode<MålgruppeFaktaOgVurdering>>()
    var målgrupperForrigeBehandling = emptyList<GeneriskVilkårperiode<MålgruppeFaktaOgVurdering>>()

    var vedtaksperioder = emptyList<Vedtaksperiode>()
    var vedtaksperioderForrigeBehandling = emptyList<Vedtaksperiode>()

    // barnid til ident
    var barnIder = mutableMapOf<String, BarnId>()
    var barnIderForrigeBehandling = mutableMapOf<String, BarnId>()

    var exception: Exception? = null
    var tidligsteEndring: TidligsteEndringResultat? = null

    @Gitt("følgende vilkår i forrige behandling - utledTidligsteEndring")
    fun `følgende vilkår i forrige behandling`(dataTable: DataTable) {
        vilkårForrigeBehandling = mapVilkår(dataTable, barnIderForrigeBehandling)
    }

    @Gitt("følgende vilkår i revurdering - utledTidligsteEndring")
    fun `følgende vilkår i revurdering`(dataTable: DataTable) {
        vilkår = mapVilkår(dataTable, barnIder)
    }

    @Gitt("følgende aktiviteter i forrige behandling - utledTidligsteEndring")
    fun `følgende aktiviteter i forrige behandling`(dataTable: DataTable) {
        aktiviteterForrigeBehandling = mapAktiviteter(dataTable)
    }

    @Gitt("følgende aktiviteter i revurdering - utledTidligsteEndring")
    fun `følgende aktiviteter i revurdering`(dataTable: DataTable) {
        aktiviteter = mapAktiviteter(dataTable)
    }

    @Gitt("følgende målgrupper i forrige behandling - utledTidligsteEndring")
    fun `følgende målgrupper i forrige behandling`(dataTable: DataTable) {
        målgrupperForrigeBehandling = mapMålgrupper(dataTable)
    }

    @Gitt("følgende målgrupper i revurdering - utledTidligsteEndring")
    fun `følgende målgrupper i revurdering`(dataTable: DataTable) {
        målgrupper = mapMålgrupper(dataTable)
    }

    @Gitt("følgende vedtaksperioder i forrige behandling - utledTidligsteEndring")
    fun `følgende vedtaksperioder i forrige behandling`(dataTable: DataTable) {
        vedtaksperioderForrigeBehandling = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende vedtaksperioder i revurdering - utledTidligsteEndring")
    fun `følgende vedtaksperioder i revurdering`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Når("utleder tidligste endring")
    fun `utleder tidligste endring`() {
        tidligsteEndring =
            TidligsteEndringIBehandlingUtleder(
                vilkår = vilkår,
                vilkårTidligereBehandling = vilkårForrigeBehandling,
                vilkårsperioder = Vilkårperioder(aktiviteter = aktiviteter, målgrupper = målgrupper),
                vilkårsperioderTidligereBehandling =
                    Vilkårperioder(
                        aktiviteter = aktiviteterForrigeBehandling,
                        målgrupper = målgrupperForrigeBehandling,
                    ),
                vedtaksperioder = vedtaksperioder,
                vedtaksperioderTidligereBehandling = vedtaksperioderForrigeBehandling,
                barnIdTilIdentMap =
                    barnIder.entries.associate { it.value to it.key } +
                        barnIderForrigeBehandling.entries.associate { it.value to it.key },
            ).utledTidligsteEndring()
    }

    @Så("forvent følgende dato for tidligste endring: {}")
    fun `forvent følgende dato for tidligste endring`(forventetDatoStr: String) {
        val forventetDato = parseDato(forventetDatoStr)
        assertThat(tidligsteEndring?.tidligsteEndring).isEqualTo(forventetDato)
    }

    @Så("forvent følgende dato for tidligste endring som påvirker utbetaling: {}")
    fun `forvent følgende dato for tidligste endring som påvirker utbetaling`(forventetDatoStr: String) {
        val forventetDato = parseDato(forventetDatoStr)
        assertThat(tidligsteEndring?.tidligsteEndringSomPåvirkerUtbetalinger).isEqualTo(forventetDato)
    }

    @Så("forvent ingen endring")
    fun `forvent ingen endring`() {
        assertThat(tidligsteEndring?.tidligsteEndring).isNull()
    }

    @Så("forvent ingen endring som påvirker utbetaling")
    fun `forvent ingen endring som påvirker utbetaling`() {
        assertThat(tidligsteEndring?.tidligsteEndringSomPåvirkerUtbetalinger).isNull()
    }

    private fun mapVilkår(
        dataTable: DataTable,
        barnIderForBehandlingMap: MutableMap<String, BarnId>,
    ) = dataTable.mapRad { rad ->
        val valgtBarnId = parseValgfriString(VilkårNøkler.BARN_ID, rad)
        vilkår(
            behandlingId = BehandlingId.random(),
            fom = parseDato(DomenenøkkelFelles.FOM, rad),
            tom = parseDato(DomenenøkkelFelles.TOM, rad),
            resultat = parseEnum(TidligsteEndringFellesNøkler.RESULTAT, rad),
            type = parseValgfriEnum<VilkårType>(TidligsteEndringFellesNøkler.TYPE, rad) ?: VilkårType.PASS_BARN,
            barnId =
                valgtBarnId?.let {
                    barnIderForBehandlingMap.getOrPut(it) { BarnId.random() }
                },
            status =
                parseValgfriEnum<VilkårStatus>(
                    TidligsteEndringFellesNøkler.STATUS,
                    rad,
                ) ?: VilkårStatus.NY,
            utgift = parseValgfriInt(VilkårNøkler.UTGIFT, rad) ?: 1000,
            erFremtidigUtgift = parseValgfriBoolean(VilkårNøkler.ER_FREMTIDIG_UTGIFT, rad) ?: false,
        )
    }

    private fun mapAktiviteter(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            aktivitet(
                behandlingId = behandlingId,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering = mapFaktaOgVurderingAktivitet(rad),
                resultat = parseEnum(TidligsteEndringFellesNøkler.RESULTAT, rad),
                status = parseEnum(TidligsteEndringFellesNøkler.STATUS, rad),
                kildeId = parseValgfriString(VilkårperiodeNøkler.KILDE_ID, rad),
            )
        }

    private fun mapMålgrupper(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            målgruppe(
                behandlingId = behandlingId,
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = parseEnum(VilkårperiodeNøkler.TYPE, rad)),
                resultat = parseEnum(TidligsteEndringFellesNøkler.RESULTAT, rad),
                status = parseEnum(TidligsteEndringFellesNøkler.STATUS, rad),
            )
        }

    private fun mapVedtaksperioder(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            vedtaksperiode(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                aktivitet = parseEnum(VedtaksperiodeNøkler.AKTIVITET, rad),
                målgruppe = parseEnum(VedtaksperiodeNøkler.MÅLGRUPPE, rad),
            )
        }

    private fun mapFaktaOgVurderingAktivitet(rad: Map<String, String>): AktivitetFaktaOgVurdering {
        val stønadstype: Stønadstype = parseEnum(VilkårperiodeNøkler.STØNADSTYPE, rad)

        return when (stønadstype) {
            Stønadstype.BARNETILSYN ->
                faktaOgVurderingAktivitetTilsynBarn(
                    type = parseEnum(VilkårperiodeNøkler.TYPE, rad),
                    aktivitetsdager =
                        parseInt(
                            VilkårperiodeNøkler.AKTIVITETSDAGER,
                            rad,
                        ),
                )

            Stønadstype.LÆREMIDLER ->
                faktaOgVurderingAktivitetLæremidler(
                    type = parseEnum(VilkårperiodeNøkler.TYPE, rad),
                    studienivå = parseEnum(VilkårperiodeNøkler.STUDIENIVÅ, rad),
                    prosent = parseInt(VilkårperiodeNøkler.STUDIEPROSENT, rad),
                )

            Stønadstype.BOUTGIFTER ->
                faktaOgVurderingAktivitetBoutgifter(
                    type = parseEnum(VilkårperiodeNøkler.TYPE, rad),
                )

            else -> error("Ukjent stønadstype: $stønadstype")
        }
    }
}
