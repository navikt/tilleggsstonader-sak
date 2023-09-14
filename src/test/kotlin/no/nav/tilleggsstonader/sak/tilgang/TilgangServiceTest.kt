package no.nav.tilleggsstonader.sak.tilgang

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.fagsak.dto.tilDto
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ManglerTilgang
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class TilgangServiceTest {

    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", kode6 = kode6Gruppe, kode7 = kode7Gruppe, "")
    private val tilgangService =
        TilgangService(
            tilgangskontrollService = tilgangskontrollService,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            fagsakPersonService = fagsakPersonService,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = mockk(relaxed = true),
        )
    private val mocketPersonIdent = "12345"

    private val fagsak = fagsak(fagsakpersoner(setOf(mocketPersonIdent)))
    private val behandling: Behandling = behandling(fagsak)
    private val olaIdent = "4567"
    private val kariIdent = "98765"

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("A")
        every { fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId) } returns fagsak.hentAktivIdent()
        every { behandlingService.hentAktivIdent(behandling.id) } returns fagsak.hentAktivIdent()
        every { fagsakService.hentAktivIdent(fagsak.id) } returns fagsak.hentAktivIdent()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(false)

        val assertThatThrownBy = assertThatThrownBy {
            tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent, AuditLoggerEvent.ACCESS)
        }
        assertThatThrownBy.isInstanceOf(ManglerTilgang::class.java)
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til behandling`() {
        val tilgangsfeilNavAnsatt = Tilgang(false, "NAV-ansatt")
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns tilgangsfeilNavAnsatt

        val feil = catchThrowableOfType<ManglerTilgang> {
            tilgangService.validerTilgangTilBehandling(
                behandling.id,
                AuditLoggerEvent.ACCESS,
            )
        }

        assertThat(feil.frontendFeilmelding).contains(tilgangsfeilNavAnsatt.begrunnelse)
        assertThat(feil.frontendFeilmelding).contains(tilgangsfeilNavAnsatt.begrunnelse)
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent, AuditLoggerEvent.ACCESS)
        verify(exactly = 1) {
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent, AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 1) {
            behandlingService.hentAktivIdent(behandling.id)
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any())
        }
    }

    @Test
    internal fun `validerTilgangTilFagsakPerson - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)

        mockBrukerContext("A")
        tilgangService.validerTilgangTilFagsakPerson(fagsak.fagsakPersonId, AuditLoggerEvent.ACCESS)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilFagsakPerson(fagsak.fagsakPersonId, AuditLoggerEvent.ACCESS)

        verify(exactly = 2) {
            tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any())
        }
    }

    @Test
    internal fun `filtrerUtFortroligDataForRolle - skal filtrere ut de roller som man har tilgang til`() {
        val uten = pdlSøker(emptyList())
        val ugradert = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.UGRADERT))
        val fortrolig = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.FORTROLIG))
        val strengtFortrolig = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG))
        val strengtFortroligUtland = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND))
        val personer = listOf(uten, ugradert, fortrolig, strengtFortrolig, strengtFortroligUtland)

        testWithBrukerContext(groups = listOf()) { assertThat(filtrer(personer)).containsExactly(uten, ugradert) }
        testWithBrukerContext(groups = listOf(rolleConfig.kode7)) {
            assertThat(filtrer(personer)).containsExactly(uten, ugradert, fortrolig)
        }
    }

    @Test
    internal fun `filtrerUtFortroligDataForRolle - kode 6 skal kun returnere kode 6`() {
        val ugradert = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.UGRADERT))
        val fortrolig = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.FORTROLIG))
        val strengtFortrolig = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG))
        val strengtFortroligUtland = pdlSøker(adresseBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND))
        val personer = listOf(ugradert, fortrolig, strengtFortrolig, strengtFortroligUtland)

        testWithBrukerContext(groups = listOf(rolleConfig.kode6)) {
            assertThat(filtrer(personer)).containsExactly(strengtFortrolig, strengtFortroligUtland)
        }
    }

    @Test
    internal fun `filtrerUtFortroligDataForRolle - skal ikke filtrere bort de uten adressebeskyttelse`() {
        val uten = pdlSøker(emptyList())
        testWithBrukerContext(groups = listOf()) {
            assertThat(filtrer(listOf(uten))).containsExactly(uten)
        }
    }

    @Test
    internal fun `validerTilgangTilEksternFagsak `() {
        every { tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(any(), any()) } returns Tilgang(true)
        every { fagsakService.hentFagsakDtoPåEksternId(any()) } returns fagsak.tilDto(true)

        tilgangService.validerTilgangTilEksternFagsak(fagsak.eksternId.id, AuditLoggerEvent.ACCESS)
    }

    private fun filtrer(personer: List<PdlSøker>): List<PdlSøker> =
        tilgangService.filtrerUtFortroligDataForRolle(personer) { it.adressebeskyttelse.gjeldende() }

    private fun adresseBeskyttelse(gradering: AdressebeskyttelseGradering) =
        listOf(Adressebeskyttelse(gradering, metadataGjeldende))
}
