package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.util.UUID

class VilkårperiodeControllerTest : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )

        val hentedeVilkårperioder = hentVilkårperioder(behandling)

        assertThat(hentedeVilkårperioder.målgrupper).hasSize(1)
        assertThat(hentedeVilkårperioder.aktiviteter).isEmpty()

        val målgruppe = hentedeVilkårperioder.målgrupper[0]
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)
    }

    @Test
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingForAnnenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
            testoppsettService.lagre(behandling(it))
        }

        val response = opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )
        val exception = catchProblemDetailException {
            slettVilkårperiode(
                vilkårperiodeId = response.periode.id,
                SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
            )
        }
        assertThat(exception.detail.detail).contains("BehandlingId er ikke lik")
    }

    @Test
    fun `skal validere stønadsperioder ved opprettelse av vilkårperiode- ingen stønadsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val response = opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )

        assertThat(response.stønadsperiodeStatus).isEqualTo(Stønadsperiodestatus.Ok)
        assertThat(response.stønadsperiodeFeil).isNull()
    }

    @Test
    fun `skal validere stønadsperioder ved oppdatering av vilkårperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val fom1 = LocalDate.of(2024, 1, 1)
        val tom1 = LocalDate.of(2024, 2, 1)

        val fom2 = LocalDate.of(2024, 2, 1)
        val tom2 = LocalDate.of(2024, 3, 1)

        opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = fom1,
                tom = tom1,
                delvilkår = delvilkårMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )
        val oppprettetTiltakPeriode = opprettVilkårperiode(
            LagreVilkårperiode(
                type = AktivitetType.TILTAK,
                fom = fom1,
                tom = tom1,
                delvilkår = delvilkårAktivitetDto(),
                behandlingId = behandling.id,
            ),
        )

        lagreStønadsperioder(behandling, listOf(nyStønadsperiode(fom1, tom1)))

        val oppdatertResponse = oppdaterVilkårperiode(
            vilkårperiodeId = oppprettetTiltakPeriode.periode.id,
            LagreVilkårperiode(
                type = AktivitetType.TILTAK,
                fom = fom2,
                tom = tom2,
                delvilkår = delvilkårAktivitetDto(),
                behandlingId = behandling.id,
            ),
        )

        assertThat(oppdatertResponse.stønadsperiodeStatus).isEqualTo(Stønadsperiodestatus.Feil)
    }

    private fun hentVilkårperioder(behandling: Behandling) =
        restTemplate.exchange<Vilkårperioder>(
            localhost("api/vilkarperiode/behandling/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun opprettVilkårperiode(
        lagreVilkårperiode: LagreVilkårperiode,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode"),
        HttpMethod.POST,
        HttpEntity(lagreVilkårperiode, headers),
    ).body!!

    private fun oppdaterVilkårperiode(
        vilkårperiodeId: UUID,
        lagreVilkårperiode: LagreVilkårperiode,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/$vilkårperiodeId"),
        HttpMethod.POST,
        HttpEntity(lagreVilkårperiode, headers),
    ).body!!

    private fun lagreStønadsperioder(
        behandling: Behandling,
        nyeStønadsperioder: List<StønadsperiodeDto>,
    ) = restTemplate.exchange<List<StønadsperiodeDto>>(
        localhost("api/stonadsperiode/${behandling.id}"),
        HttpMethod.POST,
        HttpEntity(nyeStønadsperioder, headers),
    ).body!!

    private fun nyStønadsperiode(fom: LocalDate = LocalDate.now(), tom: LocalDate = LocalDate.now()) =
        StønadsperiodeDto(
            id = null,
            fom = fom,
            tom = tom,
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        )

    private fun slettVilkårperiode(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/$vilkårperiodeId"),
        HttpMethod.DELETE,
        HttpEntity(slettVikårperiode, headers),
    ).body!!
}
