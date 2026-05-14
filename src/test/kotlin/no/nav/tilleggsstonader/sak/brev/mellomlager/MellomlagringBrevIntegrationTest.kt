package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.integrasjonstest.BehandlingContext
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MellomlagringBrevIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var mellomlagerBrevRepository: MellomlagerBrevRepository

    @Autowired
    lateinit var mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository

    lateinit var behandlingContext: BehandlingContext
    lateinit var fagsak: Fagsak

    @BeforeAll
    fun opprettBehandling() {
        testBrukerkontekst =
            TestBrukerKontekst(
                defaultBruker = "julenissen",
                defaultRoller = listOf(rolleConfig.beslutterRolle),
            )
        behandlingContext = opprettOgTaBehandlingTilBrevsteg()
        fagsak = testoppsettService.hentFagsak(behandlingContext.fagsakId)
    }

    @AfterEach
    fun cleanUp() {
        mellomlagerBrevRepository.deleteAll()
    }

    @Test
    fun `skal mellomlagre og oppdatere vedtaksbrev for behandling`() {
        val førsteMellomlagring = MellomlagreBrevDto(brevverdier = "første-utkast", brevmal = "mal-1")
        val oppdatertMellomlagring = MellomlagreBrevDto(brevverdier = "oppdatert-utkast", brevmal = "mal-2")

        // Første mellomlagring
        kall.brev.mellomlagre(behandlingContext.behandlingId, førsteMellomlagring)

        val lagretBrev = mellomlagerBrevRepository.findByIdOrNull(behandlingContext.behandlingId)
        assertThat(lagretBrev).isNotNull
        assertThat(lagretBrev!!.brevverdier).isEqualTo(førsteMellomlagring.brevverdier)
        assertThat(lagretBrev.brevmal).isEqualTo(førsteMellomlagring.brevmal)

        // Oppdatert mellomlagring
        kall.brev.mellomlagre(behandlingContext.behandlingId, oppdatertMellomlagring)
        assertThat(mellomlagerBrevRepository.findAll().filter { it.behandlingId == behandlingContext.behandlingId }.toList()).hasSize(1)

        val oppdatertBrev = mellomlagerBrevRepository.findByIdOrNull(behandlingContext.behandlingId)
        assertThat(oppdatertBrev).isNotNull
        assertThat(oppdatertBrev!!.brevverdier).isEqualTo(oppdatertMellomlagring.brevverdier)
        assertThat(oppdatertBrev.brevmal).isEqualTo(oppdatertMellomlagring.brevmal)
    }

    @Test
    fun `skal mellomlagre og oppdatere frittstående brev for innlogget saksbehandler`() {
        val førsteMellomlagring = MellomlagreBrevDto(brevverdier = "første-frittstående-utkast", brevmal = "fritt-mal-1")
        val oppdatertMellomlagring = MellomlagreBrevDto(brevverdier = "oppdatert-frittstående-utkast", brevmal = "fritt-mal-2")

        // Første mellomlagring
        kall.brev.mellomlagreFrittstående(fagsak.id, førsteMellomlagring)

        val lagretFrittståendeBrev =
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(
                fagsak.id,
                testBrukerkontekst.defaultBruker,
            )

        assertThat(lagretFrittståendeBrev).isNotNull
        assertThat(lagretFrittståendeBrev!!.brevverdier).isEqualTo(førsteMellomlagring.brevverdier)
        assertThat(lagretFrittståendeBrev.brevmal).isEqualTo(førsteMellomlagring.brevmal)

        // Oppdatert mellomalgring
        kall.brev.mellomlagreFrittstående(fagsak.id, oppdatertMellomlagring)

        assertThat(mellomlagerFrittståendeBrevRepository.findAll().filter { it.fagsakId == fagsak.id }.toList()).hasSize(1)
        val oppdatertFrittståendeBrev =
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(
                fagsak.id,
                testBrukerkontekst.defaultBruker,
            )

        assertThat(oppdatertFrittståendeBrev).isNotNull
        assertThat(oppdatertFrittståendeBrev!!.brevverdier).isEqualTo(oppdatertMellomlagring.brevverdier)
        assertThat(oppdatertFrittståendeBrev.brevmal).isEqualTo(oppdatertMellomlagring.brevmal)
    }

    @Test
    fun `samtidige kall skal ikke kaste feil pga race-condition`() {
        val routingKall =
            (1..5).map {
                CompletableFuture.supplyAsync {
                    kall.brev.apiRespons.mellomlagre(
                        behandlingContext.behandlingId,
                        MellomlagreBrevDto(
                            brevverdier = "tralala",
                            brevmal = "trololol",
                        ),
                    )
                }
            }

        routingKall.forEach {
            it.get().expectStatus().isOk
        }
    }

    private fun opprettOgTaBehandlingTilBrevsteg() =
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            tilSteg = StegType.SEND_TIL_BESLUTTER,
        ) {
            defaultDagligReiseTsoTestdata()
        }
}
