package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate

class StønadsperiodeControllerTest : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente stønadsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val nyeStønadsperioder = listOf(nyStønadsperiode())
        val opprettStønadsperiodeResponse = lagreStønadsperioder(behandling, nyeStønadsperioder)

        val hentedeStønadsperioder = hentStønadsperioder(behandling)

        assertThat(opprettStønadsperiodeResponse).hasSize(1)
        assertThat(opprettStønadsperiodeResponse[0].id).isNotNull()
        assertThat(hentedeStønadsperioder).containsExactlyElementsOf(opprettStønadsperiodeResponse)
    }

    private fun hentStønadsperioder(behandling: Behandling) =
        restTemplate.exchange<List<StønadsperiodeDto>>(
            localhost("api/stonadsperiode/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun lagreStønadsperioder(
        behandling: Behandling,
        nyeStønadsperioder: List<StønadsperiodeDto>,
    ) = restTemplate.exchange<List<StønadsperiodeDto>>(
        localhost("api/stonadsperiode/${behandling.id}"),
        HttpMethod.POST,
        HttpEntity(nyeStønadsperioder, headers),
    ).body!!

    private fun nyStønadsperiode() = StønadsperiodeDto(
        id = null,
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        målgruppe = MålgruppeType.AAP,
        aktivitet = AktivitetType.TILTAK,
    )
}
