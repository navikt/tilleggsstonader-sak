package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.dto.KodeverkDto
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate

class VilkårperiodeControllerTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        kall.vilkårperiode.opprett(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )

        val hentedeVilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(hentedeVilkårperioder.målgrupper).hasSize(1)
        assertThat(hentedeVilkårperioder.aktiviteter).isEmpty()

        val målgruppe = hentedeVilkårperioder.målgrupper[0]
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)
    }

    @Test
    fun `skal kunne oppdatere eksisterende aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        val originalLagreRequest =
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            )

        val response = kall.vilkårperiode.opprett(originalLagreRequest)

        val nyTom = LocalDate.now()

        kall.vilkårperiode.oppdater(
            lagreVilkårperiode = originalLagreRequest.copy(behandlingId = behandling.id, tom = nyTom),
            vilkårperiodeId = response.periode!!.id,
        )

        val lagredeVilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(lagredeVilkårperioder.målgrupper.single().tom).isEqualTo(nyTom)
    }

    @Test
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)
        val behandlingForAnnenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("123488888012")))).let {
                testoppsettService.lagre(behandling(it))
            }

        val response =
            kall.vilkårperiode.opprett(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

        opprettOgTilordneOppgaveForBehandling(behandlingForAnnenFagsak.id)
        kall.vilkårperiode.apiRespons
            .slett(
                vilkårperiodeId = response.periode!!.id,
                SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
            ).expectProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "BehandlingId er ikke lik")
    }

    @Test
    fun `skal kunne lagre aktivitet med type aktivitet når stønadstype daglig reise tsr`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(),
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        val originalLagreRequest =
            LagreVilkårperiode(
                type = AktivitetType.TILTAK,
                tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsrDto(
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    ),
                behandlingId = behandling.id,
            )

        kall.vilkårperiode.opprett(originalLagreRequest)
    }

    @Test
    fun `skal kunne lagre aktivitet uten aktivitetsdager når stønadstype daglig reise tsr`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(),
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        val originalLagreRequest =
            LagreVilkårperiode(
                type = AktivitetType.TILTAK,
                tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsrDto(
                        svarHarUtgifter = SvarJaNei.JA,
                    ),
                behandlingId = behandling.id,
            )

        kall.vilkårperiode.opprett(originalLagreRequest)
    }

    @Test
    fun `skal kaste feil hvis aktivitet er tiltak og ingen type aktivitet`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(),
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        val originalLagreRequest =
            LagreVilkårperiode(
                type = AktivitetType.TILTAK,
                tiltaksvariant = null,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar =
                    FaktaOgSvarAktivitetDagligReiseTsrDto(
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    ),
                behandlingId = behandling.id,
            )

        kall.vilkårperiode.apiRespons
            .opprett(originalLagreRequest)
            .expectProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Mangler data: tiltaksvariant må være satt for aktivitet TILTAK",
            )
    }

    @Nested
    inner class OppdateringAvGrunnlag {
        @Test
        fun `må ha saksbehandlerrolle for å kunne oppdatere grunnlag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            medBrukercontext(roller = listOf(rolleConfig.veilederRolle)) {
                opprettOgTilordneOppgaveForBehandling(behandling.id)
                kall.vilkårperiode.apiRespons
                    .oppdaterGrunnlag(behandling.id)
                    .expectProblemDetail(
                        HttpStatus.FORBIDDEN,
                        "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
                    )
            }
        }
    }

    @Nested
    inner class HentTiltaksvarianter {
        @Test
        fun `skal hente ulike tiltaksvarianter for alle stønadstyper som har tiltaksvarianter`() {
            val stønadstyperMedTiltaksvarianter =
                listOf(
                    Stønadstype.DAGLIG_REISE_TSR,
                    Stønadstype.REISE_TIL_SAMLING_TSR,
                    Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
                )

            val tiltaksvarianterPerStønadstype =
                stønadstyperMedTiltaksvarianter.associateWith { kall.vilkårperiode.hentTiltaksvarianter(it) }

            tiltaksvarianterPerStønadstype.values.forEach { tiltaksvarianter ->
                assertThat(tiltaksvarianter).isNotEmpty()
            }

            assertThat(tiltaksvarianterPerStønadstype.values.distinct()).hasSize(stønadstyperMedTiltaksvarianter.size)
        }

        @Test
        fun `skal returnere tom liste for stønadstype som ikke har tiltaksvarianter`() {
            val tiltaksvarianter = kall.vilkårperiode.hentTiltaksvarianter(Stønadstype.BARNETILSYN)

            assertThat(tiltaksvarianter).isEmpty()
        }

        @Test
        fun `skal bruke daglig reise tsr som default stønadstype når man ikke sender inn stønadstype`() {
            val tiltaksvarianterUtenParam =
                kall.vilkårperiode.apiRespons
                    .hentTiltaksvarianter()
                    .expectOkWithBody<List<KodeverkDto>>()
            val tiltaksvarianterDagligReiseTsr = kall.vilkårperiode.hentTiltaksvarianter(Stønadstype.DAGLIG_REISE_TSR)

            assertThat(tiltaksvarianterUtenParam).isEqualTo(tiltaksvarianterDagligReiseTsr)
        }
    }
}
