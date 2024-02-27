package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.assertions.hasCauseMessageContaining
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.IKKE_SATT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Endret
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BehandlingOppsettUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.time.LocalDateTime
import java.util.UUID

class BehandlingRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var eksternBehandlingIdRepository: EksternBehandlingIdRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    /*@Autowired
    private lateinit var oppgaveRepository: OppgaveRepository
     */

    private val ident = "123"

    @Test
    fun `skal ikke være mulig å legge inn en behandling med referanse til en behandling som ikke eksisterer`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            testoppsettService.lagre(behandling(fagsak, forrigeBehandlingId = UUID.randomUUID()))
        }
            .isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    fun findByFagsakId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    /*
    Har ikke oppgave ennå
    @Test
    fun `hentUferdigeBehandlingerFørDato skal bare hente behandlinger før en gitt dato`() {
        val enMånedSiden = LocalDateTime.now().minusMonths(1)

        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = BARNETILSYN))
        val behandling = behandling(fagsak, opprettetTid = LocalDateTime.now().minusMonths(2))
        testoppsettService.lagre(behandling)
        val annenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")), stønadstype = BARNETILSYN))
        testoppsettService.lagre(behandling(annenFagsak, opprettetTid = LocalDateTime.now().minusWeeks(1)))
        val sporbar = Sporbar("saksbh", enMånedSiden.minusDays(1))
        val oppgave = Oppgave(sporbar = sporbar, behandlingId = behandling.id, gsakOppgaveId = 1, type = Oppgavetype.BehandleSak, erFerdigstilt = false)
        oppgaveRepository.insert(oppgave)
        assertThat(
            behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(
                BARNETILSYN,
                enMånedSiden,
            ),
        ).size()
            .isEqualTo(1)
    }
     */

    /*
    Har ikke oppgave ennå
    @Test
    fun `hentUferdigeBehandlingerFørDato skal ikke hente behandling dersom oppgave er endret etter frist`() {
        val enMånedSiden = LocalDateTime.now().minusMonths(1)

        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = BARNETILSYN))
        val behandling = behandling(fagsak, opprettetTid = LocalDateTime.now().minusMonths(2))
        testoppsettService.lagre(behandling)
        val annenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")), stønadstype = BARNETILSYN))
        testoppsettService.lagre(behandling(annenFagsak, opprettetTid = LocalDateTime.now().minusWeeks(1)))
        val sporbar = Sporbar("saksbh", enMånedSiden.plusDays(1))
        val oppgave = Oppgave(sporbar = sporbar, behandlingId = behandling.id, gsakOppgaveId = 1, type = Oppgavetype.BehandleSak, erFerdigstilt = false)
        oppgaveRepository.insert(oppgave)
        assertThat(
            behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(
                BARNETILSYN,
                enMånedSiden,
            ),
        ).size()
            .isEqualTo(0)
    }
     */

    @Test
    fun findByFagsakAndStatus() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(UUID.randomUUID(), OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, FERDIGSTILT)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, OPPRETTET)).containsOnly(behandling)
    }

    @Test
    fun `finnBehandlingServiceObject returnerer korrekt konstruert BehandlingServiceObject`() {
        val fagsak = testoppsettService
            .lagreFagsak(
                fagsak(
                    setOf(
                        PersonIdent(ident = "1"),
                        PersonIdent(
                            ident = "2",
                            sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2))),
                        ),
                        PersonIdent(ident = "3"),
                    ),
                ),
            )
        val behandling = testoppsettService.lagre(behandling(fagsak, status = OPPRETTET, resultat = INNVILGET))

        val behandlingServiceObject = behandlingRepository.finnSaksbehandling(behandling.id)

        assertThat(behandlingServiceObject.id).isEqualTo(behandling.id)
        assertThat(behandlingServiceObject.eksternId).isGreaterThan(0)
        assertThat(behandlingServiceObject.forrigeBehandlingId).isEqualTo(behandling.forrigeBehandlingId)
        assertThat(behandlingServiceObject.type).isEqualTo(behandling.type)
        assertThat(behandlingServiceObject.status).isEqualTo(behandling.status)
        assertThat(behandlingServiceObject.steg).isEqualTo(behandling.steg)
        assertThat(behandlingServiceObject.årsak).isEqualTo(behandling.årsak)
        assertThat(behandlingServiceObject.kravMottatt).isEqualTo(behandling.kravMottatt)
        assertThat(behandlingServiceObject.resultat).isEqualTo(behandling.resultat)
        assertThat(behandlingServiceObject.henlagtÅrsak).isEqualTo(behandling.henlagtÅrsak)
        assertThat(behandlingServiceObject.ident).isEqualTo("2")
        assertThat(behandlingServiceObject.fagsakId).isEqualTo(fagsak.id)
        assertThat(behandlingServiceObject.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(behandlingServiceObject.eksternFagsakId).isEqualTo(fagsak.eksternId.id)
        assertThat(behandlingServiceObject.stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(behandlingServiceObject.opprettetAv).isEqualTo(behandling.sporbar.opprettetAv)
        assertThat(behandlingServiceObject.opprettetTid).isEqualTo(behandling.sporbar.opprettetTid)
        assertThat(behandlingServiceObject.endretAv).isEqualTo(behandling.sporbar.endret.endretAv)
        assertThat(behandlingServiceObject.endretTid).isEqualTo(behandling.sporbar.endret.endretTid)
        assertThat(behandlingServiceObject.vedtakstidspunkt).isEqualTo(behandling.vedtakstidspunkt)
    }

    @Test
    fun finnMedEksternId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val findByBehandlingId = behandlingRepository.findByIdOrThrow(behandling.id)
        val saksbehandling = behandlingRepository.finnSaksbehandling(behandling.id)
        val findByEksternId = behandlingRepository.finnMedEksternId(saksbehandling.eksternId)

        assertThat(findByEksternId).isEqualTo(behandling)
        assertThat(findByEksternId).isEqualTo(findByBehandlingId)
    }

    @Test
    fun `finnFnrForBehandlingId(sql) skal finne gjeldende fnr for behandlingsid`() {
        val fagsak = testoppsettService.lagreFagsak(
            fagsak(
                setOf(
                    PersonIdent(ident = "1"),
                    PersonIdent(
                        ident = "2",
                        sporbar = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2))),
                    ),
                    PersonIdent(ident = "3"),
                ),
            ),
        )
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val fnr = behandlingRepository.finnAktivIdent(behandling.id)
        assertThat(fnr).isEqualTo("2")
    }

    @Test
    fun `finnMedEksternId skal gi null når det ikke finnes behandling for gitt id`() {
        val findByEksternId = behandlingRepository.finnMedEksternId(1000000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(ident))))
        testoppsettService.lagre(
            behandling(
                fagsak,
                status = UTREDES,
                opprettetTid = LocalDateTime.now().minusDays(2),
            ),
        )
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isNull()
    }

    @Test
    fun `finnSisteIverksatteBehandling skal finne id til siste ferdigstilte behandling`() {
        val førstegangsbehandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling
        val fagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))).copy(id = førstegangsbehandling.fagsakId))

        val behandlinger = BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte()
        testoppsettService.lagre(behandlinger)

        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)?.id)
            .isEqualTo(førstegangsbehandling.id)
    }

    @Test
    fun `finnEksterneIder - skal hente eksterne ider`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(behandling.id))
        val eksternBehandlingId = eksternBehandlingIdRepository.findByBehandlingId(behandling.id)

        assertThat(fagsak.eksternId.id).isNotEqualTo(0L)
        assertThat(eksternBehandlingId.id).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(behandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(eksternBehandlingId.id)
        assertThat(first.eksternFagsakId).isEqualTo(fagsak.eksternId.id)
    }

    @Test
    fun `finnEksterneIder - send inn én behandlingId som finnes, forvent én eksternId `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val annenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
        val annenBehandling = testoppsettService.lagre(behandling(annenFagsak))
        val annenEksternBehandlingId = eksternBehandlingIdRepository.findByBehandlingId(annenBehandling.id)

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(annenBehandling.id))

        assertThat(fagsak.eksternId.id).isNotEqualTo(0L)
        assertThat(annenEksternBehandlingId.id).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(annenBehandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(annenEksternBehandlingId.id)
        assertThat(first.eksternFagsakId).isEqualTo(annenFagsak.eksternId.id)
    }

    @Test
    fun `finnEksterneIder - send inn behandlingIder som ikke finnes, forvent ingen treff `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(behandling(fagsak))
        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(UUID.randomUUID(), UUID.randomUUID()))
        assertThat(eksterneIder.isEmpty())
    }

    @Test
    fun `finnEksterneIder - send inn tomt sett, forvent unntak `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(behandling(fagsak))
        org.junit.jupiter.api.assertThrows<Exception> {
            assertThat(behandlingRepository.finnEksterneIder(emptySet()))
        }
    }

    @Nested
    inner class ExistsByFagsak {

        @Test
        fun `inner ikke når det ikke finnes noen behandlinger`() {
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
        }

        @Test
        fun `finner ikke når det kun finnes av annen type`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
        }

        @Test
        fun `true når det av typen man spør etter`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    type = BehandlingType.REVURDERING,
                ),
            )
            assertThat(behandlingRepository.existsByFagsakId(UUID.randomUUID())).isFalse
        }
    }

    @Nested
    inner class Maks1UtredesPerFagsak {

        @Test
        fun `skal ikke kunne ha flere behandlinger på samma fagsak med annen status enn ferdigstilt`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, status = FERDIGSTILT))
            testoppsettService.lagre(behandling(fagsak, status = UTREDES))
            testoppsettService.lagre(behandling(fagsak, status = FERDIGSTILT))

            listOf(UTREDES, OPPRETTET, FATTER_VEDTAK, IVERKSETTER_VEDTAK).forEach { status ->
                val cause = assertThatThrownBy {
                    testoppsettService.lagre(behandling(fagsak, status = status))
                }.cause
                cause.isInstanceOf(DuplicateKeyException::class.java)
                cause.hasMessageContaining("duplicate key value violates unique constraint \"idx_behandlinger_i_arbeid\"")
            }
        }

        @Test
        fun `skal kunne ha en behandling som utredes når det finnes en behandling satt på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, status = FERDIGSTILT))
            testoppsettService.lagre(behandling(fagsak, status = SATT_PÅ_VENT))
            testoppsettService.lagre(behandling(fagsak, status = SATT_PÅ_VENT))

            testoppsettService.lagre(behandling(fagsak, status = UTREDES))
        }

        @Test
        fun `kan ikke endre en behandling fra satt på vent til utredes når det allerede finnes en behandling som ikke er ferdigstilt`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, status = FERDIGSTILT))
            val påVent = testoppsettService.lagre(behandling(fagsak, status = SATT_PÅ_VENT))
            testoppsettService.lagre(behandling(fagsak, status = IVERKSETTER_VEDTAK))

            val cause = assertThatThrownBy {
                behandlingRepository.update(påVent.copy(status = UTREDES))
            }.cause
            cause.isInstanceOf(DuplicateKeyException::class.java)
            cause.hasMessageContaining("duplicate key value violates unique constraint \"idx_behandlinger_i_arbeid\"")
        }

        @Test
        fun `skal kunne ha flere behandlinger på ulike fagsak med status utredes`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner("2")))
            testoppsettService.lagre(behandling(fagsak, status = UTREDES))
            testoppsettService.lagre(behandling(fagsak2, status = UTREDES))
        }
    }

    @Nested
    inner class ExistsByFagsakIdAndStatusIsNot {

        @Test
        fun `returnerer true hvis behandling med annen status finnes og false om behandling med annen status ikke finnes`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner("1")))
            testoppsettService.lagre(behandling(fagsak, status = UTREDES))

            val ikkeFerdigstiltFinnes = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsak.id, FERDIGSTILT)
            val ikkeUtredesFinnesIkke = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsak.id, UTREDES)

            assertThat(ikkeFerdigstiltFinnes).isTrue()
            assertThat(ikkeUtredesFinnesIkke).isFalse()
        }
    }

    /* har ikke andre fagsaker
    @Test
    internal fun `skal finne aktuell behandling for gjenbruk av inngangsvilkår`() {
        val fagsakPersonId = UUID.randomUUID()

        val fagsakOS = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        lagreBehandling(UUID.randomUUID(), OPPRETTET, IKKE_SATT, fagsakOS)

        val fagsakBT = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingBT = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakBT)

        val behandlingerForGjenbruk: List<Behandling> =
            behandlingRepository.finnBehandlingerForGjenbrukAvVilkår(fagsakBT.fagsakPersonId)

        assertThat(behandlingerForGjenbruk).containsExactly(førstegangsbehandlingOS, førstegangsbehandlingBT)
    }

    @Test
    internal fun `skal finne alle aktuelle behandlinger for gjenbruk av inngangsvilkår`() {
        val fagsakPersonId = UUID.randomUUID()

        val fagsakOS = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        val annengangsbehandlingOS = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakOS)
        lagreBehandling(UUID.randomUUID(), FERDIGSTILT, HENLAGT, fagsakOS)

        val fagsakBT = lagreFagsak(UUID.randomUUID(), BARNETILSYN, fagsakPersonId)
        val førstegangsbehandlingBT = lagreBehandling(UUID.randomUUID(), FERDIGSTILT, INNVILGET, fagsakBT)
        val revurderingUnderArbeidBT = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakBT)

        val fagsakSP = lagreFagsak(UUID.randomUUID(), SKOLEPENGER, fagsakPersonId)
        val revurderingUnderArbeidSP = lagreBehandling(UUID.randomUUID(), UTREDES, IKKE_SATT, fagsakSP)

        val behandlingerForGjenbruk: List<Behandling> =
            behandlingRepository.finnBehandlingerForGjenbrukAvVilkår(fagsakSP.fagsakPersonId)

        assertThat(behandlingerForGjenbruk).containsExactly(
            førstegangsbehandlingOS,
            annengangsbehandlingOS,
            førstegangsbehandlingBT,
            revurderingUnderArbeidBT,
            revurderingUnderArbeidSP,
        )
    }
     */

    @Nested
    inner class Vedtakstidspunkt {

        private val fagsak = fagsak()

        @BeforeEach
        internal fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
        }

        @Test
        internal fun `kan sette resultat med vedtakstidspunkt`() {
            testoppsettService.lagre(behandling(fagsak, resultat = INNVILGET))
        }

        @Test
        internal fun `kan ikke sette resultat uten vedtakstidspunkt`() {
            assertThatThrownBy {
                testoppsettService.lagre(behandling(fagsak, resultat = INNVILGET).copy(vedtakstidspunkt = null))
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }

        @Test
        internal fun `kan ikke sette vedtakstidspunkt uten resultat`() {
            assertThatThrownBy {
                testoppsettService.lagre(
                    behandling(
                        fagsak,
                        resultat = IKKE_SATT,
                    ).copy(vedtakstidspunkt = SporbarUtils.now()),
                )
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }

        @Test
        internal fun `kan ikke sette resultat IKKE_SATT med vedtakstidspunkt`() {
            assertThatThrownBy {
                testoppsettService.lagre(
                    behandling(
                        fagsak,
                        resultat = IKKE_SATT,
                    ).copy(vedtakstidspunkt = SporbarUtils.now()),
                )
            }.has(hasCauseMessageContaining("behandling_resultat_vedtakstidspunkt_check"))
        }
    }

    private fun lagreBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
        resultat: BehandlingResultat,
        fagsak: Fagsak,
    ): Behandling {
        return testoppsettService.lagre(
            behandling(
                id = behandlingId,
                status = status,
                resultat = resultat,
                fagsak = fagsak,
            ),
        )
    }

    private fun lagreFagsak(
        fagsakId: UUID,
        stønadstype: Stønadstype,
        fagsakPersonId: UUID,
    ): Fagsak {
        return testoppsettService.lagreFagsak(
            fagsak(
                id = fagsakId,
                stønadstype = stønadstype,
                fagsakPersonId = fagsakPersonId,
            ),
        )
    }
}
