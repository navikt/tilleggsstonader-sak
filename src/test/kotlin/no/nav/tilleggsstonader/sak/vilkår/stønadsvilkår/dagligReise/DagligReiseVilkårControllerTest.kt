package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreVilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class DagligReiseVilkårControllerTest : CleanDatabaseIntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO)
    val behandling = behandling(fagsak = fagsak, steg = StegType.VILKÅR)

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    val svarPrivatBil =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                SvarOgBegrunnelseDto(
                    svar = SvarId.NEI,
                    begrunnelse = "begrunnelse",
                ),
            RegelId.KAN_KJØRE_MED_EGEN_BIL to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        opprettOgTilordneOppgaveForBehandling(behandling.id)
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for daglig reise - offentlig transport`() {
        val nyttVilkår =
            LagreVilkårDagligReiseDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                adresse = "Tiltaksveien 1",
                reiseId = dummyReiseId,
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )

        val resultat = kall.vilkårDagligReise.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkår, resultat)

        val oppdatertVilkår =
            nyttVilkår.copy(
                tom = LocalDate.of(2025, 2, 28),
                fakta =
                    faktaOffentligTransport(
                        reisedagerPerUke = 4,
                    ),
            )

        val resultatOppdatert = kall.vilkårDagligReise.oppdaterVilkår(oppdatertVilkår, resultat.id, behandling.id)

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)

        val resultatSlettet =
            kall.vilkårDagligReise.slettVilkår(
                behandling.id,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårDagligReise.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for daglig reise - privat bil`() {
        val fom = 1 januar 2026
        val tom = 31 januar 2026

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.VILKÅR,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
            }

        val aktivitet =
            kall.vilkårperiode
                .hentForBehandling(behandlingContext.behandlingId)
                .vilkårperioder.aktiviteter
                .single()

        val nyttVilkår =
            LagreVilkårDagligReiseDto(
                fom = fom,
                tom = tom,
                adresse = "Tiltaksveien 1",
                reiseId = dummyReiseId,
                svar = svarPrivatBil,
                fakta = faktaPrivatBil(aktivitet = aktivitet),
            )

        val opprettetVilkår = kall.vilkårDagligReise.opprettVilkår(behandlingContext.behandlingId, nyttVilkår)

        assertThat(opprettetVilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(opprettetVilkår.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkår, opprettetVilkår)

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta =
                    faktaPrivatBil(
                        reiseavstandEnVei = BigDecimal("10"),
                        aktivitet = aktivitet,
                    ),
            )

        val resultatOppdatert =
            kall.vilkårDagligReise.oppdaterVilkår(oppdatertVilkår, opprettetVilkår.id, behandlingContext.behandlingId)

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)

        val resultatSlettet =
            kall.vilkårDagligReise.slettVilkår(
                behandlingId = behandlingContext.behandlingId,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårDagligReise.hentVilkår(behandlingContext.behandlingId)
        assertThat(hentedeVilkår).isEmpty()
    }

    @Test
    fun `skal kunne hente vilkår med bompenger over 500 fra databasen men ikke lagre nye verdier over 500`() {
        val fom = 1 januar 2026
        val tom = 31 januar 2026

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.VILKÅR,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
            }

        val aktivitet =
            kall.vilkårperiode
                .hentForBehandling(behandlingContext.behandlingId)
                .vilkårperioder.aktiviteter
                .single()

        val nyttVilkår =
            LagreVilkårDagligReiseDto(
                fom = fom,
                tom = tom,
                adresse = "Tiltaksveien 1",
                reiseId = dummyReiseId,
                svar = svarPrivatBil,
                fakta = faktaPrivatBil(aktivitet = aktivitet),
            )

        val opprettetVilkår = kall.vilkårDagligReise.opprettVilkår(behandlingContext.behandlingId, nyttVilkår)

        jdbcTemplate.update(
            """
            UPDATE vilkar
            SET fakta = jsonb_set(
                CAST(fakta AS jsonb),
                '{faktaDelperioder,0,bompengerPerDag}',
                to_jsonb(CAST(:bompengerPerDag AS numeric)),
                true
            )
            WHERE id = :vilkarId
            """.trimIndent(),
            mapOf(
                "bompengerPerDag" to BigDecimal("5023"),
                "vilkarId" to opprettetVilkår.id.id,
            ),
        )

        val vilkårFraDb = kall.vilkårDagligReise.hentVilkår(behandlingContext.behandlingId).single()
        val fakta = vilkårFraDb.fakta as FaktaDagligReisePrivatBilDto
        assertThat(fakta.faktaDelperioder.single().bompengerPerDag).isEqualTo(BigDecimal("5023"))

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta =
                    faktaPrivatBil(
                        aktivitet = aktivitet,
                        bompengerPerDag = BigDecimal("501"),
                    ),
            )

        kall.vilkårDagligReise.apiRespons
            .oppdaterVilkår(oppdatertVilkår, opprettetVilkår.id, behandlingContext.behandlingId)
            .expectProblemDetail(
                forventetStatus = HttpStatus.BAD_REQUEST,
                forventetDetail = "Skal du innvilge med bompenger høyere enn 500kr må du ta kontakt med Tilleggsstønader-temet",
            )
    }

    @Test
    fun `skal kunne lagre ned et vilkår med fakta UBESTEMT med adresse og reiseId om vilkår ikke er oppfylt`() {
        val svarAvstandIkkeOppfylt =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.NEI, "Antall km"),
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.NEI, "Begrunnelse"),
            )

        val nyttVilkår =
            LagreVilkårDagligReiseDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                adresse = "Tiltaksveien 1",
                reiseId = dummyReiseId,
                svar = svarAvstandIkkeOppfylt,
                fakta = FaktaDagligReiseUbestemtDto,
            )

        val resultat = kall.vilkårDagligReise.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.reiseId).isEqualTo(dummyReiseId)
        assertThat(resultat.adresse).isEqualTo("Tiltaksveien 1")
        assertThat(resultat.fakta).isNotNull
    }

    @Test
    fun `skal hente alle regler som tilhører daglig reise`() {
        val resultat = kall.vilkårDagligReise.regler()

        FileUtil.assertFileJsonIsEqual("vilkår/regelstruktur/DAGLIG_REISE.json", resultat)
    }

    private fun faktaOffentligTransport(
        reisedagerPerUke: Int = 5,
        prisEnkelbillett: Int? = 40,
        prisSyvdagersbillett: Int? = null,
        prisTrettidagersbillett: Int? = 800,
    ) = FaktaDagligReiseOffentligTransportDto(
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
    )

    private fun faktaPrivatBil(
        reiseavstandEnVei: BigDecimal = BigDecimal("10"),
        fom: LocalDate = 1 januar 2026,
        tom: LocalDate = 31 januar 2026,
        bompengerPerDag: BigDecimal? = null,
        fergekostnadPerDag: BigDecimal? = null,
        aktivitet: VilkårperiodeDto,
    ) = FaktaDagligReisePrivatBilDto(
        reiseavstandEnVei = reiseavstandEnVei,
        faktaDelperioder =
            listOf(
                FaktaDelperiodePrivatBilDto(
                    fom = fom,
                    tom = tom,
                    reisedagerPerUke = 5,
                    bompengerPerDag = bompengerPerDag,
                    fergekostnadPerDag = fergekostnadPerDag,
                ),
            ),
        aktivitetId = aktivitet.globalId,
        adresse = "Tiltaksveien 1",
        aktivitetType = aktivitet.type.toString(),
    )

    private fun assertLagretVilkår(
        lagreVilkårRequest: LagreVilkårDagligReiseDto,
        resultat: VilkårDagligReiseDto,
    ) {
        assertThat(resultat.fom).isEqualTo(lagreVilkårRequest.fom)
        assertThat(resultat.tom).isEqualTo(lagreVilkårRequest.tom)
        assertThat(resultat.fakta).isEqualTo(lagreVilkårRequest.fakta)
        assertThat(resultat.delvilkårsett).hasSize(1)

        assertAlleSvarHarFåttVurdering(delvilkår = resultat.delvilkårsett, svar = lagreVilkårRequest.svar)
    }

    private fun assertAlleSvarHarFåttVurdering(
        delvilkår: List<DelvilkårDto>,
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
    ) {
        val brukteRegelIder = delvilkår.flatMap { it.vurderinger.map { vurdering -> vurdering.regelId } }.toSet()

        assertThat(brukteRegelIder).hasSize(svar.size)
    }
}
