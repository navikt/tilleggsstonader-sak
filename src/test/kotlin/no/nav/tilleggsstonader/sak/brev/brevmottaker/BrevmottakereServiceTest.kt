package no.nav.tilleggsstonader.sak.brev.brevmottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.MottakerRolle
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import java.util.*

class BrevmottakereServiceTest {

    private val brevmottakereRepository = mockk<BrevmottakerVedtaksbrevRepository>()
    private val behandlingService = mockk<BehandlingService>()

    private val brevmottakereService: BrevmottakereService =
        BrevmottakereService(brevmottakereRepository, behandlingService)

    val brevmottakerPersonDtoVanligBruker =
        BrevmottakerPersonDto(UUID.randomUUID(), "12312312323", "Test Testersen", MottakerRolle.BRUKER)

    val brevmottakerOrganisasjonDto =
        BrevmottakerOrganisasjonDto(UUID.randomUUID(), "45645646456", "Forretninger AS", "Navn navnesen")

    val vanligeBrevmottakereDto =
        BrevmottakereDto(listOf(brevmottakerPersonDtoVanligBruker), listOf(brevmottakerOrganisasjonDto))

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom behandligsstatus er FATTER_VEDTAK`() {
        val behandling = behandling(status = BehandlingStatus.FATTER_VEDTAK)

        every { behandlingService.hentBehandling(any()) } returns behandling

        assertThatThrownBy {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), vanligeBrevmottakereDto)
        }.hasMessageContaining("Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering.")
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom behandligsstatus er IVERKSETTER_VEDTAK`() {
        val behandling = behandling(status = BehandlingStatus.IVERKSETTER_VEDTAK)

        every { behandlingService.hentBehandling(any()) } returns behandling

        assertThatThrownBy {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), vanligeBrevmottakereDto)
        }.hasMessageContaining("Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering.")
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom behandligsstatus er FERDIGSTILT`() {
        val behandling = behandling(status = BehandlingStatus.FERDIGSTILT)

        every { behandlingService.hentBehandling(any()) } returns behandling

        assertThatThrownBy {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), vanligeBrevmottakereDto)
        }.hasMessageContaining("Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering.")
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom behandligsstatus er SATT_PÅ_VENT`() {
        val behandling = behandling(status = BehandlingStatus.SATT_PÅ_VENT)

        every { behandlingService.hentBehandling(any()) } returns behandling

        assertThatThrownBy {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), vanligeBrevmottakereDto)
        }.hasMessageContaining("Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering.")
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom det ikke finnes noen mottakere`() {
        val brevmottakereDto =
            BrevmottakereDto(emptyList(), emptyList())

        every { behandlingService.hentBehandling(any()) } returns behandling()

        val feil = catchThrowableOfType<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), brevmottakereDto)
        }
        assertThat(feil.message).contains("Vedtaksbrevet må ha minst 1 mottaker")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom det er fler enn to mottakere`() {
        val brevmottakerPersonDto2 =
            BrevmottakerPersonDto(UUID.randomUUID(), "98769876987", "Donald Duck", MottakerRolle.BRUKER)

        val brevmottakereDto =
            BrevmottakereDto(
                listOf(brevmottakerPersonDtoVanligBruker, brevmottakerPersonDto2),
                listOf(brevmottakerOrganisasjonDto),
            )

        every { behandlingService.hentBehandling(any()) } returns behandling()

        val feil = catchThrowableOfType<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), brevmottakereDto)
        }
        assertThat(feil.message).contains("Vedtaksbrevet kan ikke ha mer enn 2 mottakere")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom man legger inn 2 personer som brevmottakere med samme personIdent`() {
        val personIdent = "12312312323"
        val brevmottakerPersonDtoMedSammePersonIdent1 =
            BrevmottakerPersonDto(UUID.randomUUID(), personIdent, "Test Testersen", MottakerRolle.BRUKER)

        val brevmottakerPersonDtoMedSammePersonIdent2 =
            BrevmottakerPersonDto(UUID.randomUUID(), personIdent, "Donald Duck", MottakerRolle.BRUKER)
        val brevmottakereDto =
            BrevmottakereDto(
                listOf(
                    brevmottakerPersonDtoMedSammePersonIdent1,
                    brevmottakerPersonDtoMedSammePersonIdent2,
                ),
                emptyList(),
            )

        every { behandlingService.hentBehandling(any()) } returns behandling()

        val feil = catchThrowableOfType<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), brevmottakereDto)
        }
        assertThat(feil.message).contains("En person kan bare legges til en gang som brevmottaker")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `lagreBrevmottakere skal kaste feilmelding dersom man legger inn 2 organisasjoner som brevmottakere med samme organisasjonsnummer`() {
        val organisasjonsnummer = "45645646456"

        val brevmottakerOrganisasjonDtoMedSammeOrgnr1 =
            BrevmottakerOrganisasjonDto(UUID.randomUUID(), organisasjonsnummer, "Forretninger AS", "Navn navnesen")

        val brevmottakerOrganisasjonDtoMedSammeOrgnr2 =
            BrevmottakerOrganisasjonDto(UUID.randomUUID(), organisasjonsnummer, "Forretninger AS", "Navn navnesen")

        val brevmottakereDto =
            BrevmottakereDto(
                emptyList(),
                listOf(brevmottakerOrganisasjonDtoMedSammeOrgnr1, brevmottakerOrganisasjonDtoMedSammeOrgnr2),
            )

        every { behandlingService.hentBehandling(any()) } returns behandling()

        val feil = catchThrowableOfType<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(BehandlingId.random(), brevmottakereDto)
        }
        assertThat(feil.message).contains("En organisasjon kan bare legges til en gang som brevmottaker")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `hentEllerOpprettBrevmottakere skal returnere BrevmottakereDto dersom disse finnes i repository`() {
        val id = UUID.randomUUID()
        val behandlingID = BehandlingId.random()
        val ident = "123123123"
        val mottakernavn = "Test Testersen"

        val brevmottakerTestObjekt = BrevmottakerVedtaksbrev(
            id = id,
            behandlingId = behandlingID,
            mottaker = Mottaker(
                mottakerRolle = no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerRolle.BRUKER,
                mottakerType = MottakerType.PERSON,
                ident = ident,
                mottakerNavn = mottakernavn,
                organisasjonsNavn = null,
            ),

            journalpostId = null,
            bestillingId = null,
            sporbar = Sporbar(),
        )

        val brevmottakereDtoFasit = BrevmottakereDto(
            personer = listOf(
                BrevmottakerPersonDto(
                    id = id,
                    personIdent = ident,
                    navn = mottakernavn,
                    mottakerRolle = MottakerRolle.BRUKER,
                ),
            ),
            organisasjoner = emptyList(),
        )

        every { brevmottakereRepository.existsByBehandlingId(behandlingID) } returns true
        every { brevmottakereRepository.findByBehandlingId(behandlingID) } returns listOf(brevmottakerTestObjekt)

        val brevmottakere: BrevmottakereDto = brevmottakereService.hentEllerOpprettBrevmottakere(behandlingID)

        assertThat(brevmottakere).isEqualTo(brevmottakereDtoFasit)
    }
}
