package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.util.UUID

class MottaSøknadTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var journalpostClient: JournalpostClient

    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    @Autowired
    lateinit var hendelseRepository: HendelseRepository

    val journalpostId = 123321L
    val ident = "12345678901"

    @Test
    fun `mottar boutgifter-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.BOUTGIFTER, søknad = søknadBoutgifter(ident = ident))
        kjørTasksKlareForProsessering()

        val fagsakerPåBruker = fagsakRepository.findBySøkerIdent(setOf(ident))
        assertThat(fagsakerPåBruker).hasSize(1)
        val fagsak = fagsakerPåBruker.single()
        assertThat(fagsak.stønadstype).isEqualTo(Stønadstype.BOUTGIFTER)

        val behandlinger = behandlingRepository.findByFagsakId(fagsak.id)
        assertThat(behandlinger).hasSize(1)
        val behandling = behandlinger.single()
        assertThat(behandling.årsak).isEqualTo(BehandlingÅrsak.SØKNAD)
    }

    private fun mockJournalpost(
        brevkode: DokumentBrevkode,
        søknad: Any,
    ) {
        val søknaddookumentInfo =
            dokumentInfo(
                brevkode = brevkode.verdi,
                dokumentvarianter = listOf(dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)),
            )
        val journalpost =
            journalpost(
                journalpostId = journalpostId.toString(),
                journalposttype = Journalposttype.I,
                dokumenter = listOf(søknaddookumentInfo),
                kanal = "NAV_NO",
                bruker = Bruker(ident, BrukerIdType.FNR),
                journalstatus = Journalstatus.MOTTATT,
            )

        every { journalpostClient.hentJournalpost(journalpostId.toString()) } returns journalpost

        every {
            journalpostClient.hentDokument(journalpostId.toString(), søknaddookumentInfo.dokumentInfoId, Dokumentvariantformat.ORIGINAL)
        } returns
            objectMapper.writeValueAsBytes(søknad)
    }
}
