package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlagTestUtil
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException

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

    @Test
    fun `Skal ikke være lov å lagre faktagrunnlag med samme behandling_id, type, og type_id når type_id ikke er null`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val faktaGrunnlagBarnAnnenForelder =
            GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id)
        val faktaGrunnlagBarnAnnenForelderDuplikat =
            GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id)

        faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelder)
        assertThrows<DbActionExecutionException> {
            faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelderDuplikat)
        }
    }

    @Test
    fun `Skal ikke være lov å lagre faktagrunnlag med samme behandling_id, type, og type_id når type_id er null`() {
        // Det opprettes et faktaGrunnlagPersonopplysninger når man oppretter behandlingen
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val faktaGrunnlagPersonopplysningerDuplikat =
            GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagPersonopplysninger(behandlingId = behandling.id)

        assertThrows<DbActionExecutionException> {
            faktaGrunnlagRepository.insert(faktaGrunnlagPersonopplysningerDuplikat)
        }
    }
}
