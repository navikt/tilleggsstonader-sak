package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.fødsel
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class GrunnlagsdataServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val barnService = mockk<BarnService>()
    val personService = mockk<PersonService>()
    val grunnlagsdataRepository = mockk<GrunnlagsdataRepository>()

    val service = GrunnlagsdataService(
        behandlingService = behandlingService,
        barnService = barnService,
        personService = personService,
        grunnlagsdataRepository = grunnlagsdataRepository,
    )

    val behandling = saksbehandling()
    val identBarn1 = "identBarn1"

    val pdlBarn = mapOf(
        identBarn1 to pdlBarn(fødsel = fødsel()),
    )
    val søkerMedBarn = SøkerMedBarn(
        behandling.ident,
        pdlSøker(),
        barn = pdlBarn,
    )

    @BeforeEach
    fun setUp() {
        every { grunnlagsdataRepository.findByIdOrNull(any()) } returns null
        every { grunnlagsdataRepository.insert(any()) } answers { firstArg() }
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { barnService.finnBarnPåBehandling(behandling.id) } returns emptyList()
        every { personService.hentPersonMedBarn(behandling.ident) } returns søkerMedBarn
    }

    @Nested
    inner class HentEllerOpprett {

        @Test
        fun `skal opprette grunnlag hvis det ikke finnes fra før`() {
            val grunnlagsdata = service.hentGrunnlagsdata(behandling.id)

            assertThat(grunnlagsdata.grunnlag.barn).isEmpty()
            assertThat(grunnlagsdata.grunnlag.navn.fornavn).isEqualTo("Fornavn")
            verify(exactly = 1) { grunnlagsdataRepository.insert(any()) }
        }

        @Test
        fun `skal hente direkte fra basen hvis dataen finnes fra før`() {
            every { grunnlagsdataRepository.findByIdOrNull(behandling.id) } returns grunnlagsdataDomain()

            service.hentGrunnlagsdata(behandling.id)
            verify(exactly = 0) { grunnlagsdataRepository.insert(any()) }
        }
    }

    @Nested
    inner class BarnMapping {

        @Test
        fun `skal hente grunnlag til behandlingBarn`() {
            every { barnService.finnBarnPåBehandling(behandling.id) } returns listOf(behandlingBarn(personIdent = identBarn1))

            service.hentGrunnlagsdata(behandling.id)

            val grunnlagsdata = service.hentGrunnlagsdata(behandling.id)

            assertThat(grunnlagsdata.grunnlag.barn).hasSize(1)
        }

        @Test
        fun `skal kaste feil hvis man ikke finner barn i PDL - då er det noe som er feil med barn på behandlingen`() {
            every { barnService.finnBarnPåBehandling(behandling.id) } returns listOf(behandlingBarn())

            assertThatThrownBy {
                service.hentGrunnlagsdata(behandling.id)
            }.hasMessageContaining("Finner ikke grunnlag for barn")
        }
    }
}
