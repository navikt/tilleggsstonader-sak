package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FaktaGrunnlagServiceTest : IntegrationTest() {
    @Autowired
    lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired
    lateinit var faktaGrunnlagRepository: FaktaGrunnlagRepository

    @Test
    fun `skal hente opp riktig type av faktagrunnlag`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id))

        val grunnlag = faktaGrunnlagService.hentGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>(behandling.id)

        assertThat(grunnlag).hasSize(1)
        assertThat(grunnlag[0].data).isInstanceOf(FaktaGrunnlagBarnAndreForeldreSaksinformasjon::class.java)
    }
}
