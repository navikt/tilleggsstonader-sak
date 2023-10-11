package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSøknadBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.søknadBarnetilsyn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VilkårGrunnlagServiceTest {

    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val søknadService = mockk<SøknadService>()
    val service = VilkårGrunnlagService(
        grunnlagsdataService,
        søknadService,
    )

    val behandlingId = UUID.randomUUID()

    @Test
    fun `skal mappe søknad og grunnlag`() {
        every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata()
        every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn()

        val data = service.hentGrunnlag(behandlingId)
        assertFileIsEqual("vilkår/vilkårGrunnlagDto.json", data)
    }

    @Nested
    inner class GrunnlagBarnTest {

        @Test
        fun `skal mappe søknadsgrunnlag for de barn som fantes i søknaden`() {
            every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata(
                grunnlagsdata = lagGrunnlagsdata(
                    barn = listOf(lagGrunnlagsdataBarn("1"), lagGrunnlagsdataBarn("2")),
                ),
            )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                barn = setOf(lagSøknadBarn(ident = "1")),
            )

            val data = service.hentGrunnlag(behandlingId)

            assertThat(data.barn).hasSize(2)
            data.barn[0].let {
                assertThat(it.ident).isEqualTo("1")
                assertThat(it.søknadgrunnlag).isNotNull
            }
            data.barn[1].let {
                assertThat(it.ident).isEqualTo("2")
                assertThat(it.søknadgrunnlag).isNull()
            }
        }

        @Test
        fun `skal kaste feil hvis ikke alle barnen fra søknaden har grunnlagsdata`() {
            every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata(
                grunnlagsdata = lagGrunnlagsdata(
                    barn = listOf(lagGrunnlagsdataBarn("1")),
                ),
            )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                barn = setOf(lagSøknadBarn(ident = "1"), lagSøknadBarn(ident = "2"), lagSøknadBarn(ident = "3")),
            )
            assertThatThrownBy {
                service.hentGrunnlag(behandlingId)
            }.hasMessage("Mangler grunnlagsdata for barn i søknad (2,3)")
        }
    }
}
