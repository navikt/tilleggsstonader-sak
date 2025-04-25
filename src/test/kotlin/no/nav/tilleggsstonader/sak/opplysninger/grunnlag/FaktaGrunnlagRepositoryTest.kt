package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlagTestUtil
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FaktaGrunnlagRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var faktaGrunnlagRepository: FaktaGrunnlagRepository

    @Test
    fun `skal kunne lagre og hente opp data`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val faktaGrunnlagBarnAnnenForelder =
            GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id)
        faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelder)

        val fakta =
            faktaGrunnlagRepository.findByBehandlingIdAndType(
                behandlingId = behandling.id,
                type = TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON,
            )
        Assertions.assertThat(fakta).hasSize(1)
        Assertions.assertThat(fakta[0].data).isEqualTo(faktaGrunnlagBarnAnnenForelder.data)
        Assertions.assertThat(fakta[0].typeId).isEqualTo("barn1")
        Assertions.assertThat(fakta[0].type).isEqualTo(TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON)
    }
}
