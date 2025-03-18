package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FaktaGrunnlagRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var faktaGrunnlagRepository: FaktaGrunnlagRepository

    @Test
    fun `skal kunne lagre og hente opp data`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val faktaGrunnlagBarnAnnenForelder = faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id)
        faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelder)

        val fakta =
            faktaGrunnlagRepository.findByBehandlingIdAndType(
                behandlingId = behandling.id,
                type = TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON,
            )
        assertThat(fakta).hasSize(1)
        assertThat(fakta[0].data).isEqualTo(faktaGrunnlagBarnAnnenForelder.data)
        assertThat(fakta[0].typeId).isEqualTo("barn1")
        assertThat(fakta[0].type).isEqualTo(TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON)
    }
}
