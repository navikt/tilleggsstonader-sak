package no.nav.tilleggsstonader.sak.tilgang

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
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
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class TilgangServiceTest {
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val kode6Gruppe = "kode6"
    private val kode7Gruppe = "kode7"
    private val rolleConfig = RolleConfig("", "", "", kode6 = kode6Gruppe, kode7 = kode7Gruppe, "", "")
    private val tilgangService =
        TilgangService(
            tilgangskontrollService = tilgangskontrollService,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            fagsakPersonService = fagsakPersonService,
            rolleConfig = rolleConfig,
            cacheManager = cacheManager,
            auditLogger = mockk(relaxed = true),
            mockk(),
        )
    private val mocketPersonIdent = "12345"

    private val fagsak = fagsak(fagsakpersoner(setOf(mocketPersonIdent)))
    private val behandling: Behandling = behandling(fagsak)
    private val olaIdent = "4567"

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("A")
        every { fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId) } returns fagsak.hentAktivIdent()
        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling(fagsak, behandling)
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class ValiderTilgangTilStønadstype {
        @Test
        internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til person eller dets barn`() {
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(false)

            val assertThatThrownBy =
                assertThatThrownBy {
                    tilgangService.validerTilgangTilStønadstype(mocketPersonIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)
                }
            assertThatThrownBy.isInstanceOf(ManglerTilgang::class.java)
        }

        @Test
        internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(true)

            tilgangService.validerTilgangTilStønadstype(mocketPersonIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)
        }

        @Test
        internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til behandling`() {
            val tilgangsfeilNavAnsatt = Tilgang(false, "Nav-ansatt")
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns tilgangsfeilNavAnsatt

            val feil =
                catchThrowableOfType<ManglerTilgang> {
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
            every {
                tilgangskontrollService.sjekkTilgangTilStønadstype(any(), stønadstype = Stønadstype.BARNETILSYN, any())
            } returns Tilgang(true)

            assertDoesNotThrow {
                tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
            }
        }

        @Test
        internal fun `validerTilgangTilPersonMedBarn - cachear ikke svaret då pdl allikevel er cachet`() {
            every {
                tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any())
            } returns Tilgang(true)

            mockBrukerContext("A")
            tilgangService.validerTilgangTilStønadstype(olaIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)
            tilgangService.validerTilgangTilStønadstype(olaIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)
            verify(exactly = 2) {
                tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any())
            }
        }

        @Test
        internal fun `validerTilgangTilPersonMedBarn - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(true)

            mockBrukerContext("A")
            tilgangService.validerTilgangTilStønadstype(olaIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)
            mockBrukerContext("B")
            tilgangService.validerTilgangTilStønadstype(olaIdent, Stønadstype.BARNETILSYN, AuditLoggerEvent.ACCESS)

            verify(exactly = 2) {
                tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any())
            }
        }
    }

    @Nested
    inner class ValiderTilgangTilBehandling {
        @Test
        internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(true)

            mockBrukerContext("A")

            tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
            tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

            verify(exactly = 1) { behandlingService.hentSaksbehandling(behandling.id) }
            verify(exactly = 2) { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) }
        }

        @Test
        internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
            every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(true)

            mockBrukerContext("A")
            tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)
            mockBrukerContext("B")
            tilgangService.validerTilgangTilBehandling(behandling.id, AuditLoggerEvent.ACCESS)

            verify(exactly = 1) { behandlingService.hentSaksbehandling(behandling.id) }
            verify(exactly = 2) { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) }
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
        every { tilgangskontrollService.sjekkTilgangTilStønadstype(any(), any(), any()) } returns Tilgang(true)
        every { fagsakService.hentFagsakPåEksternId(any()) } returns fagsak

        tilgangService.validerTilgangTilEksternFagsak(fagsak.eksternId.id, AuditLoggerEvent.ACCESS)
    }

    @Nested
    inner class Roller {
        @Test
        fun `egne ansatt - har rolle hvis man har egne-ansatt-rolle`() {
            testWithBrukerContext(groups = listOf(rolleConfig.egenAnsatt)) {
                assertThat(tilgangService.harEgenAnsattRolle()).isTrue()
                assertThat(tilgangService.harStrengtFortroligRolle()).isFalse()
            }
        }

        @Test
        fun `strengt fortrolig - har rolle hvis man har kode6-rolle`() {
            testWithBrukerContext(groups = listOf(rolleConfig.kode6)) {
                assertThat(tilgangService.harStrengtFortroligRolle()).isTrue()
                assertThat(tilgangService.harEgenAnsattRolle()).isFalse()
            }
        }

        @Test
        fun `roller - har ikke tilgang hvis man ikke har noen roller`() {
            testWithBrukerContext(groups = listOf()) {
                assertThat(tilgangService.harEgenAnsattRolle()).isFalse()
                assertThat(tilgangService.harStrengtFortroligRolle()).isFalse()
            }
        }
    }

    private fun filtrer(personer: List<PdlSøker>): List<PdlSøker> =
        tilgangService.filtrerUtFortroligDataForRolle(personer) { it.adressebeskyttelse.gjeldende() }

    private fun adresseBeskyttelse(gradering: AdressebeskyttelseGradering) = listOf(Adressebeskyttelse(gradering, metadataGjeldende))
}
