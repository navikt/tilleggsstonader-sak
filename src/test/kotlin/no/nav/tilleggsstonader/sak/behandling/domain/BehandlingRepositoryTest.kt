package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.assertions.hasCauseMessageContaining
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.AVSLÅTT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.IKKE_SATT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.OPPRETTET
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Endret
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BehandlingOppsettUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.nyeOpplysningerMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var eksternBehandlingIdRepository: EksternBehandlingIdRepository

    private val ident = "123"

    @Test
    fun `skal ikke være mulig å legge inn en behandling med referanse til en behandling som ikke eksisterer`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            testoppsettService.lagre(behandling(fagsak, forrigeIverksatteBehandlingId = BehandlingId.random()))
        }.isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    fun findByFagsakId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(FagsakId.random())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    @Test
    fun findByFagsakAndStatus() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(FagsakId.random(), OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, FERDIGSTILT)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, OPPRETTET)).containsOnly(behandling)
    }

    @Nested
    inner class FinnSaksbehandling {
        val fagsak =
            testoppsettService
                .lagreFagsak(
                    fagsak(
                        setOf(
                            PersonIdent(ident = "1"),
                            PersonIdent(
                                ident = "2",
                                sporbar = Sporbar(endret = Endret(endretTid = osloNow().plusDays(2))),
                            ),
                            PersonIdent(ident = "3"),
                        ),
                    ),
                )
        val behandling =
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    status = OPPRETTET,
                    resultat = INNVILGET,
                    revurderFra = LocalDate.of(2023, 1, 1),
                    type = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.SØKNAD,
                    henlagtÅrsak = HenlagtÅrsak.FEILREGISTRERT,
                    henlagtBegrunnelse = "Registrert feil",
                    vedtakstidspunkt = SporbarUtils.now(),
                    kravMottatt = LocalDate.now(),
                    nyeOpplysningerMetadata = nyeOpplysningerMetadata(),
                ),
            )

        @Test
        fun `finnSaksbehandling returnerer korrekt konstruert saksbehandling`() {
            behandlingRepository
                .finnSaksbehandling(behandling.id)
                .assertFelterErLike(behandling, fagsak)
        }

        @Test
        fun `finnSaksbehandling med eksternBehandlingId skal mappe ok`() {
            val eksternBehandlingId = eksternBehandlingIdRepository.findByBehandlingId(behandling.id).id
            behandlingRepository
                .finnSaksbehandling(eksternBehandlingId)
                .assertFelterErLike(behandling, fagsak)
        }
    }

    private fun Saksbehandling.assertFelterErLike(
        behandling: Behandling,
        fagsak: Fagsak,
    ) {
        assertThat(id).isEqualTo(behandling.id)
        assertThat(eksternId).isGreaterThan(0)
        assertThat(forrigeIverksatteBehandlingId).isEqualTo(behandling.forrigeIverksatteBehandlingId)
        assertThat(type).isEqualTo(behandling.type)
        assertThat(status).isEqualTo(behandling.status)
        assertThat(steg).isEqualTo(behandling.steg)
        assertThat(årsak).isEqualTo(behandling.årsak)
        assertThat(kravMottatt).isEqualTo(behandling.kravMottatt)
        assertThat(resultat).isEqualTo(behandling.resultat)
        assertThat(henlagtÅrsak).isEqualTo(behandling.henlagtÅrsak)
        assertThat(henlagtBegrunnelse).isEqualTo(behandling.henlagtBegrunnelse)
        assertThat(ident).isEqualTo("2")
        assertThat(fagsakId).isEqualTo(fagsak.id)
        assertThat(fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(eksternFagsakId).isEqualTo(fagsak.eksternId.id)
        assertThat(stønadstype).isEqualTo(fagsak.stønadstype)
        assertThat(opprettetAv).isEqualTo(behandling.sporbar.opprettetAv)
        assertThat(opprettetTid).isEqualTo(behandling.sporbar.opprettetTid)
        assertThat(endretAv).isEqualTo(behandling.sporbar.endret.endretAv)
        assertThat(endretTid).isEqualTo(behandling.sporbar.endret.endretTid)
        assertThat(vedtakstidspunkt).isEqualTo(behandling.vedtakstidspunkt)
        assertThat(revurderFra).isEqualTo(behandling.revurderFra)
        assertThat(nyeOpplysningerMetadata).isEqualTo(behandling.nyeOpplysningerMetadata)
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
        val fagsak =
            testoppsettService.lagreFagsak(
                fagsak(
                    setOf(
                        PersonIdent(ident = "1"),
                        PersonIdent(
                            ident = "2",
                            sporbar = Sporbar(endret = Endret(endretTid = osloNow().plusDays(2))),
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
                opprettetTid = osloNow().minusDays(2),
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
        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(BehandlingId.random(), BehandlingId.random()))
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
            assertThat(behandlingRepository.existsByFagsakId(FagsakId.random())).isFalse
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
            assertThat(behandlingRepository.existsByFagsakId(FagsakId.random())).isFalse
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
            assertThat(behandlingRepository.existsByFagsakId(FagsakId.random())).isFalse
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
                val cause =
                    assertThatThrownBy {
                        testoppsettService.lagre(behandling(fagsak, status = status))
                    }.cause()
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

            val cause =
                assertThatThrownBy {
                    behandlingRepository.update(påVent.copy(status = UTREDES))
                }.cause()
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

    @Nested
    inner class FinnGjeldendeIverksatteBehandlinger {
        @Test
        fun `ingen behandlinger - finner ingen gjeldende behandlinger`() {
            assertThat(behandlingRepository.finnGjeldendeIverksatteBehandlinger(Stønadstype.BARNETILSYN))
                .isEmpty()
        }

        @Test
        fun `behandling men ikke ferdigstilt - finner ingen gjeldende behandlinger`() {
            testoppsettService.opprettBehandlingMedFagsak(behandling())
            assertThat(behandlingRepository.finnGjeldendeIverksatteBehandlinger(Stønadstype.BARNETILSYN))
                .isEmpty()
        }

        @Test
        fun `behandling ferdigstilt med resultat avslått - finner ingen gjeldende behandlinger`() {
            val behandling = behandling(status = FERDIGSTILT, resultat = AVSLÅTT)
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            assertThat(behandlingRepository.finnGjeldendeIverksatteBehandlinger(Stønadstype.BARNETILSYN))
                .isEmpty()
        }

        @Test
        fun `skal hente siste behandlingen som ble iverksatt for en iverksatte behandling`() {
            val behandling = behandling(status = FERDIGSTILT, resultat = INNVILGET)
            testoppsettService.opprettBehandlingMedFagsak(behandling)
            assertThat(behandlingRepository.finnGjeldendeIverksatteBehandlinger(Stønadstype.BARNETILSYN))
                .hasSize(1)
        }

        @Test
        fun `skal hente behandlingen med siste vedtakstidspunktet`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val behandling =
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    resultat = INNVILGET,
                    vedtakstidspunkt = LocalDateTime.now().minusDays(2),
                )
            val behandling2 =
                behandling(fagsak, status = FERDIGSTILT, resultat = INNVILGET, vedtakstidspunkt = LocalDateTime.now())
            val behandling3 =
                behandling(
                    fagsak,
                    status = FERDIGSTILT,
                    resultat = INNVILGET,
                    vedtakstidspunkt = LocalDateTime.now().minusDays(1),
                )

            testoppsettService.lagre(listOf(behandling, behandling2, behandling3))

            assertThat(behandlingRepository.finnGjeldendeIverksatteBehandlinger(Stønadstype.BARNETILSYN).map { it.id })
                .containsExactly(behandling2.id)
        }
    }
}
