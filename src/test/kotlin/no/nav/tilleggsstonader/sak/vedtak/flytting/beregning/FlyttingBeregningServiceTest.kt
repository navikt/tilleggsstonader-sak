package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.mapTilAndeler
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FlyttingBeregningServiceTest {
    private val flyttingVilkårService = mockk<FlyttingVilkårService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService =
        FlyttingBeregningService(
            flyttingVilkårService = flyttingVilkårService,
            satsPrivatBilProvider = SatsPrivatBilProvider(),
            vedtakService = vedtakService,
        )
    private val behandling = saksbehandling()

    private fun FlyttingBeregningService.beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) = beregn(behandling, vedtaksperioder, Beregningsplan(Beregningsomfang.ALLE_PERIODER))

    @Test
    fun `beholder beregnet beløp uansett betalingsdokumentasjon og viderefører dokumentasjonen`() {
        listOf(false, true).forEach { erBetalingDokumentert ->
            val vilkår = flyttebyrå(10_000, 12_000, erBetalingDokumentert = erBetalingDokumentert)
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(vilkår)

            val resultat =
                beregningService
                    .beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
                    .resultater
                    .single()

            assertThat(resultat.beløp).isEqualTo(10000.toBigDecimal())
            assertThat((resultat.grunnlag as BeregningsgrunnlagFlyttebyrå).erBetalingDokumentert)
                .isEqualTo(erBetalingDokumentert)
            assertThat(vilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        }
    }

    @Test
    fun `velger laveste pris fra flyttebyrå uavhengig av tilbudsrekkefølge`() {
        listOf(
            10_000 to 12_000,
            12_000 to 10_000,
            10_000 to 10_000,
        ).forEach { (tilbud1, tilbud2) ->
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
                listOf(flyttebyrå(tilbud1, tilbud2))

            val resultat = beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))

            assertThat(resultat.resultater.single().beløp).isEqualTo(10000.toBigDecimal())
        }
    }

    @Test
    fun `beregner egen kjøring med enveisavstand, tillegg og avrunding`() {
        val vilkår = egenKjøring(fakta = FaktaFlytteSelv(250, 1500, 300, 400, 100, "Adresse 1"))
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(vilkår)

        val resultat = beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))

        assertThat(resultat.resultater.single().beløp).isEqualTo(3035.toBigDecimal())
    }

    @Test
    fun `runder kilometerbeløp HALF_UP og manglende tillegg regnes som null`() {
        val beregnedeBeløp =
            listOf(24 to "71", 26 to "76", 25 to "74").map { (avstand, forventet) ->
                every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
                    listOf(egenKjøring(fakta = FaktaFlytteSelv(avstand, null, null, null, null, "Adresse 1")))
                val resultat =
                    beregningService
                        .beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
                        .resultater
                        .single()
                assertThat(resultat.beløp).isEqualByComparingTo(forventet)
                resultat.beløp
            }

        assertThat(beregnedeBeløp).containsExactly(
            BigDecimal("71"),
            BigDecimal("76"),
            BigDecimal("74"),
        )
    }

    @Test
    fun `avviser egen kjøring når vilkårsperioden strekker seg over flere kalenderår`() {
        val vilkår =
            egenKjøring(
                fom = 31 desember 2025,
                tom = 1 januar 2026,
                fakta = FaktaFlytteSelv(100, null, null, null, null, "Adresse 1"),
            )
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(vilkår)

        assertThatThrownBy {
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(31 desember 2025, 1 januar 2026)),
            )
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Kan ikke finne relevant kilometersats for $vilkår")
    }

    @Test
    fun `bruker videreført ubekreftet sats når vilkåret starter etter siste bekreftede sats`() {
        val vilkår =
            egenKjøring(
                fom = 1 januar 2027,
                tom = 31 januar 2027,
                fakta = FaktaFlytteSelv(100, null, null, null, null, "Adresse 1"),
            )
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(vilkår)

        val resultat =
            beregningService
                .beregn(
                    behandling,
                    listOf(vedtaksperiode(1 januar 2027, 31 januar 2027)),
                ).resultater
                .single()

        assertThat(resultat.beløp).isEqualByComparingTo("294")
        assertThat((resultat.grunnlag as BeregningsgrunnlagEgenKjøring).satsBekreftet)
            .isFalse()
    }

    @Test
    fun `lager ett resultat per vilkår selv når samme vilkår dekkes av flere perioder`() {
        val vilkårJanuar = flyttebyrå(10_000, 12_000, 1 januar 2026, 31 januar 2026)
        val vilkårFebruar = flyttebyrå(12_000, 10_000, 1 februar 2026, 28 februar 2026)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                vilkårFebruar,
                vilkårJanuar,
            )

        val resultat =
            beregningService.beregn(
                behandling,
                listOf(
                    vedtaksperiode(1 januar 2026, 15 januar 2026),
                    vedtaksperiode(16 januar 2026, 31 januar 2026),
                    vedtaksperiode(1 februar 2026, 28 februar 2026),
                ),
            )

        assertThat(resultat.resultater.map { it.fom }).containsExactly(1 januar 2026, 1 februar 2026)
        assertThat(resultat.resultater.map { it.beløp }).containsExactly(BigDecimal("10000"), BigDecimal("10000"))
    }

    @Test
    fun `avviser manglende pris og manglende dekning av hele vilkåret`() {
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(flyttebyrå(10_000, null))
        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
        }.isInstanceOf(Feil::class.java)

        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                flyttebyrå(
                    10_000,
                    12_000,
                ),
            )
        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 30 januar 2026)))
        }.isInstanceOf(Feil::class.java)
    }

    @Test
    fun `ignorerer slettet vilkår og avviser tomt beregningsgrunnlag`() {
        val slettet =
            flyttebyrå(10_000, 12_000).copy(
                resultat = Vilkårsresultat.SLETTET,
                status = VilkårStatus.SLETTET,
                slettetKommentar = "Slettet i test",
            )
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(slettet)

        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
        }.isInstanceOf(ApiFeil::class.java)
    }

    @Test
    fun `uendret revurdering gjenbruker tidligere beløp sats og målgruppe`() {
        val original = egenKjøring(fakta = FaktaFlytteSelv(100, null, null, null, null, "Adresse 1"))
        val tidligere = tidligereResultat(listOf(original))
        val lagret =
            tidligere.resultater.single().copy(
                beløp = 123.toBigDecimal(),
                grunnlag =
                    (tidligere.resultater.single().grunnlag as BeregningsgrunnlagEgenKjøring).copy(
                        sats =
                            BigDecimal(
                                "1.23",
                            ),
                    ),
            )
        val revurdering = forrigeVedtak(listOf(original), BeregningsresultatFlytting(listOf(lagret)))
        val kopi = original.copy(id = VilkårId.random(), behandlingId = revurdering.id, status = VilkårStatus.UENDRET)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns listOf(kopi)

        val resultat =
            beregningService.beregn(
                revurdering,
                listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT),
            )

        assertThat(resultat.resultater).containsExactly(lagret.copy(fraTidligereVedtak = true))
        assertThat(resultat.mapTilAndeler(Stønadstype.FLYTTING_TSO).single().beløp).isEqualTo(123)
        assertThat(lagret.fraTidligereVedtak).isFalse()
        val reberegnet =
            beregningService
                .beregn(
                    revurdering,
                    listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                    Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ).resultater
                .single()
        assertThat(reberegnet.beløp).isEqualTo(294.toBigDecimal())
        assertThat(reberegnet.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `ny flytting beregnes mens gammel før grensen gjenbrukes i komplett snapshot`() {
        val original = flyttebyrå(5000, 6000)
        val tidligere = tidligereResultat(listOf(original))
        val revurdering = forrigeVedtak(listOf(original), tidligere)
        val kopi = original.copy(id = VilkårId.random(), behandlingId = revurdering.id, status = VilkårStatus.UENDRET)
        val ny = flyttebyrå(3000, 4000, 1 februar 2026, 28 februar 2026).copy(behandlingId = revurdering.id)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns listOf(ny, kopi)

        val resultat =
            beregningService.beregn(
                revurdering,
                listOf(vedtaksperiode(1 januar 2026, 28 februar 2026)),
                Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 februar 2026),
            )

        assertThat(resultat.resultater.map { it.fraTidligereVedtak }).contains(true, false)
        assertThat(resultat.resultater.map { it.beløp }).contains(5000.toBigDecimal(), 3000.toBigDecimal())
        assertThat(resultat.mapTilAndeler(Stønadstype.FLYTTING_TSR).map { it.beløp }).contains(5000, 3000)
    }

    @Test
    fun `endret pris dokumentasjon dato forkorting og flyttemåte reberegnes også før grensen`() {
        val original = flyttebyrå(5000, 6000)
        val tidligere = tidligereResultat(listOf(original))
        val revurdering = forrigeVedtak(listOf(original), tidligere)
        val byrå = original.fakta as FaktaFlyttebyrå
        val endringer =
            listOf(
                original.copy(fakta = byrå.copy(tilbud1 = byrå.tilbud1.copy(pris = 3000))),
                original.copy(fakta = byrå.copy(erBetalingDokumentert = false)),
                original.copy(fom = 2 januar 2026),
                original.copy(tom = 15 januar 2026),
                original.copy(fom = 2 januar 2026, tom = 2 januar 2026),
                original.copy(fakta = FaktaFlytteSelv(100, null, null, null, null, "Adresse 1")),
            )
        endringer.forEach { endret ->
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns
                listOf(
                    endret.copy(
                        id = VilkårId.random(),
                        behandlingId = revurdering.id,
                        status = VilkårStatus.ENDRET,
                    ),
                )
            val resultat =
                beregningService
                    .beregn(
                        revurdering,
                        listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                        Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 februar 2026),
                    ).resultater
                    .single()
            assertThat(resultat.fraTidligereVedtak).isFalse()
            assertThat(resultat.fom).isEqualTo(endret.fom)
            assertThat(resultat.tom).isEqualTo(endret.tom)
        }
        assertThat(tidligere.resultater.single().beløp).isEqualTo(5000.toBigDecimal())
    }

    @Test
    fun `dokumentasjon begge veier endrer andeler men beholder beløp og oppfylt resultat`() {
        listOf(false, true).forEach { dokumentert ->
            val original = flyttebyrå(5000, 6000, erBetalingDokumentert = dokumentert)
            val tidligere = tidligereResultat(listOf(original))
            val revurdering = forrigeVedtak(listOf(original), tidligere)
            val byrå = original.fakta as FaktaFlyttebyrå
            val endret =
                original.copy(fakta = byrå.copy(erBetalingDokumentert = !dokumentert), status = VilkårStatus.ENDRET)
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns listOf(endret)
            val resultat =
                beregningService.beregn(
                    revurdering,
                    listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                    Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 januar 2026),
                )
            assertThat(resultat.resultater.single().beløp).isEqualTo(5000.toBigDecimal())
            assertThat(endret.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
            listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR).forEach { stønadstype ->
                assertThat(resultat.mapTilAndeler(stønadstype)).hasSize(if (dokumentert) 0 else 1)
            }
        }
    }

    @Test
    fun `slettede flyttinger gjenbrukes aldri selv før grensen`() {
        val original = flyttebyrå(5000, 6000)
        val beholdt = flyttebyrå(3000, 4000, 1 februar 2026, 28 februar 2026)
        val tidligere = tidligereResultat(listOf(original, beholdt))
        val revurdering = forrigeVedtak(listOf(original, beholdt), tidligere)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns
            listOf(
                original.copy(status = VilkårStatus.SLETTET, resultat = Vilkårsresultat.SLETTET),
                beholdt.copy(status = VilkårStatus.UENDRET),
            )
        val resultat =
            beregningService.beregn(
                revurdering,
                listOf(vedtaksperiode(1 januar 2026, 28 februar 2026)),
                Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 januar 2026),
            )
        assertThat(resultat.mapTilAndeler(Stønadstype.FLYTTING_TSO).single().beløp).isEqualTo(3000)
    }

    @Test
    fun `avviser manglende tidligere resultat og unsupported omfang`() {
        val original = flyttebyrå(5000, 6000)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns listOf(original)
        listOf(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT, Beregningsomfang.KUN_NYE_KJORELISTE_UKER).forEach { omfang ->
            assertThatThrownBy {
                beregningService.beregn(
                    behandling,
                    listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                    Beregningsplan(omfang),
                )
            }.isInstanceOf(Feil::class.java)
        }
        val revurdering = forrigeVedtak(listOf(original), BeregningsresultatFlytting(emptyList()))
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns listOf(original)
        assertThatThrownBy {
            beregningService.beregn(
                revurdering,
                listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)),
                Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 februar 2026),
            )
        }.isInstanceOf(Feil::class.java).hasMessageContaining("Fant ikke tidligere beregningsresultat")
    }

    private fun tidligereResultat(vilkår: List<VilkårFlytting>): BeregningsresultatFlytting {
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns vilkår
        return beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 28 februar 2026)))
    }

    private fun forrigeVedtak(
        vilkår: List<VilkårFlytting>,
        resultat: BeregningsresultatFlytting,
    ): Saksbehandling {
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns vilkår
        every { vedtakService.hentVedtakEllerFeil(behandling.id) } returns
            GeneriskVedtak(
                behandlingId = behandling.id,
                data =
                    InnvilgelseFlytting(
                        vedtaksperioder = listOf(vedtaksperiode(1 januar 2026, 28 februar 2026)),
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                        beregningsresultat = resultat,
                    ),
                gitVersjon = null,
                tidligsteEndring = null,
            )
        return saksbehandling(forrigeIverksatteBehandlingId = behandling.id)
    }

    private fun flyttebyrå(
        tilbud1: Int?,
        tilbud2: Int?,
        fom: LocalDate = 1 januar 2026,
        tom: LocalDate = 31 januar 2026,
        erBetalingDokumentert: Boolean = true,
    ) = VilkårFlytting(
        behandlingId = behandling.id,
        delvilkårsett = emptyList(),
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        fom = fom,
        tom = tom,
        fakta =
            FaktaFlyttebyrå(
                tilbud1 = FlyttebyråTilbud("A", tilbud1),
                tilbud2 = FlyttebyråTilbud("B", tilbud2),
                erBetalingDokumentert = erBetalingDokumentert,
                adresse = "Adresse 1",
            ),
    )

    private fun egenKjøring(
        fom: LocalDate = 1 januar 2026,
        tom: LocalDate = 31 januar 2026,
        fakta: FaktaFlytteSelv,
    ) = VilkårFlytting(
        behandlingId = behandling.id,
        delvilkårsett = emptyList(),
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        fom = fom,
        tom = tom,
        fakta = fakta,
    )
}
