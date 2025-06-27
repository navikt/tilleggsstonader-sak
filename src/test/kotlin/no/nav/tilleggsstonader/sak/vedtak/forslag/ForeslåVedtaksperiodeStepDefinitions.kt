package no.nav.tilleggsstonader.sak.vedtak.forslag

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilUUIDHolder.testIdTilUUID
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperioderV2Util
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

enum class DomenenøkkelForeslåVedtaksperioder(
    override val nøkkel: String,
) : Domenenøkkel {
    RESULTAT("Resultat"),
    TYPE("type"),
    AKTIVITET("aktivitet"),
    MÅLGRUPPE("målgruppe"),
}

@Suppress("ktlint:standard:function-naming", "unused")
class ForeslåVedtaksperiodeStepDefinitions {
    var aktiviteter: List<VilkårperiodeAktivitet> = emptyList()
    var målgrupper: List<VilkårperiodeMålgruppe> = emptyList()
    var vilkår: List<Vilkår> = emptyList()
    var tidligereVedtaksperioder = emptyList<Vedtaksperiode>()
    var resultat: List<Vedtaksperiode> = emptyList()
    var resultat2: List<Vedtaksperiode> = emptyList()
    var feil: ApiFeil? = null
    var idSomSkalIgnoreres = mutableSetOf<UUID>()

    @Gitt("følgende vilkårsperioder med aktiviteter for vedtaksforslag")
    fun `følgende vilkårsperioder med aktiviteter`(dataTable: DataTable) {
        aktiviteter = mapAktiviteter(dataTable)
    }

    @Gitt("følgende vilkårsperioder med målgrupper for vedtaksforslag")
    fun `følgende vilkårsperioder med målgrupper`(dataTable: DataTable) {
        målgrupper = mapMålgruppe(dataTable)
    }

    @Gitt("følgende vilkår for vedtaksforslag")
    fun `følgende vilkår`(dataTable: DataTable) {
        vilkår = mapVilkår(dataTable)
    }

    @Gitt("følgende tidligere vedtaksperioder for vedtaksforslag")
    fun `følgende tidligere vedtaksperioder`(dataTable: DataTable) {
        tidligereVedtaksperioder = mapVedtaksperioder(dataTable)

        // assert idn er unike
        val idn = tidligereVedtaksperioder.map { it.id }
        assertThat(idn).containsExactlyElementsOf(idn.distinct())
    }

    @Når("forslag til vedtaksperioder lages")
    fun `forslag til vedtaksperioder lages`() {
        try {
            resultat =
                ForeslåVedtaksperiode.finnVedtaksperiode(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                    vilkår,
                )
            resultat2 =
                ForeslåVedtaksperioderV2Util.foreslåPerioder(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                    vilkår,
                )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Når("forslag til vedtaksperioder behold id lages")
    fun `forslag til vedtaksperioder behold id lages`() {
        try {
            resultat =
                ForeslåVedtaksperiode.finnVedtaksperiodeV2(
                    Vilkårperioder(
                        målgrupper = målgrupper,
                        aktiviteter = aktiviteter,
                    ),
                    vilkår = vilkår,
                    tidligereVedtaksperioder = tidligereVedtaksperioder,
                )
        } catch (e: ApiFeil) {
            feil = e
        }
    }

    @Så("forvent følgende feil for vedtaksforsalg: {}")
    fun `forvent følgende feil`(feil: String) {
        assertThat(this.feil).isNotNull
        assertThat(this.feil?.message).isEqualTo(feil)
    }

    @Så("forvent følgende vedtaksperioder")
    fun `forvent følgende vedtaksperioder`(dataTable: DataTable) {
        assertThat(this.feil).isNull()

        val uuid = UUID.randomUUID()
        val forventetVedtaksperioderMedSammeId = mapVedtaksperioder(dataTable).map { it.copy(id = uuid) }
        val resultatMedSammeId = resultat.map { it.copy(id = uuid) }
        val resultatMedSammeId2 = resultat2.map { it.copy(id = uuid) }
        assertThat(resultatMedSammeId).isEqualTo(forventetVedtaksperioderMedSammeId)
        assertThat(resultatMedSammeId2).isEqualTo(forventetVedtaksperioderMedSammeId)
    }

    @Så("forvent følgende vedtaksperioder med riktig id")
    fun `forvent følgende vedtaksperioder med riktig id`(dataTable: DataTable) {
        assertThat(this.feil).isNull()
        val expected = mapVedtaksperioder(dataTable)

        expected.forEachIndexed { index, it ->
            if (resultat.size < index + 1) {
                throw Error(
                    "Feilet rad ${index + 1}. Forventer at resultatet har ${expected.size} rader resultat har ${resultat.size} rader",
                )
            }
            val actual = resultat[index]
            if (!idSomSkalIgnoreres.contains(it.id)) {
                assertThat(actual.id).isEqualTo(it.id)
            }
            if (idSomSkalIgnoreres.contains(it.id) && tidligereVedtaksperioder.any { it.id == actual.id }) {
                throw Error(
                    "Feilet rad ${index + 1}. Hvis actual inneholder en id som eksisterer i tidligere vedtaksperioder må den assertes riktig",
                )
            }
            assertThat(actual.fom).isEqualTo(it.fom)
            assertThat(actual.tom).isEqualTo(it.tom)
            assertThat(actual.aktivitet).isEqualTo(it.aktivitet)
            assertThat(actual.målgruppe).isEqualTo(it.målgruppe)
        }

        assertThat(resultat).hasSize(expected.size)
    }

    private fun mapAktiviteter(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            aktivitet(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering =
                    faktaOgVurderingAktivitetTilsynBarn(
                        type = parseValgfriEnum<AktivitetType>(DomenenøkkelForeslåVedtaksperioder.TYPE, rad)!!,
                    ),
            )
        }

    private fun mapMålgruppe(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            målgruppe(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = parseEnum(DomenenøkkelForeslåVedtaksperioder.TYPE, rad),
                    ),
            )
        }

    private fun mapVilkår(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            vilkår(
                behandlingId = BehandlingId.random(),
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                resultat = parseEnum(DomenenøkkelForeslåVedtaksperioder.RESULTAT, rad),
                type = VilkårType.PASS_BARN,
            )
        }

    private fun mapVedtaksperioder(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            val id =
                parseValgfriInt(DomenenøkkelFelles.ID, rad)?.let {
                    if (it == -1) {
                        val id = UUID.randomUUID()
                        idSomSkalIgnoreres.add(id)
                        id
                    } else {
                        testIdTilUUID[it]
                    }
                }
            Vedtaksperiode(
                id = id ?: UUID.randomUUID(),
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                målgruppe = parseEnum(DomenenøkkelForeslåVedtaksperioder.MÅLGRUPPE, rad),
                aktivitet = parseEnum(DomenenøkkelForeslåVedtaksperioder.AKTIVITET, rad),
            )
        }
}
