package no.nav.tilleggsstonader.sak

import io.mockk.every
import io.mockk.mockk
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.kafka.support.Acknowledgment
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@RestController
@Unprotected
class SendInnKjørelisteTestController(
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val journalpostClient: JournalpostClient,
    private val journalhendelseKafkaListener: JournalhendelseKafkaListener,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping(
        value = ["/api/test/kjoreliste/send"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sendInnKjørelistePåFagsak(
        @RequestBody sendInnKjøreliste: SendInnKjøreliste,
    ) {
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = sendInnKjøreliste.reiseId.toString(),
                periode = Datoperiode(sendInnKjøreliste.fom, sendInnKjøreliste.tom),
                dagerKjørt = sendInnKjøreliste.kjørteDager.map { KjørtDag(it.dato, it.parkeringsutgift) },
            )

        val fagsak = fagsakService.hentFagsakPåEksternId(sendInnKjøreliste.fagsakEksternId)
        val ident = fagsakPersonService.hentIdenter(fagsak.fagsakPersonId).first().ident

        val kjørelisteDookumentInfo =
            dokumentInfo(
                brevkode = DokumentBrevkode.DAGLIG_REISE_KJØRELISTE.verdi,
                dokumentvarianter = listOf(dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)),
            )

        val journalpostId = Random.nextLong()

        val journalpost =
            journalpost(
                journalpostId = journalpostId.toString(),
                journalposttype = Journalposttype.I,
                dokumenter = listOf(kjørelisteDookumentInfo),
                kanal = "NAV_NO",
                bruker = Bruker(ident, BrukerIdType.FNR),
                journalstatus = Journalstatus.MOTTATT,
            )

        every {
            journalpostClient.hentDokument(
                journalpostId.toString(),
                kjørelisteDookumentInfo.dokumentInfoId,
                Dokumentvariantformat.ORIGINAL,
            )
        } returns jsonMapper.writeValueAsBytes(InnsendtSkjema("", LocalDateTime.now(), Språkkode.NB, kjøreliste))

        opprettJournalpost(journalpost)

        val journalfoeringHendelseRecord =
            JournalfoeringHendelseRecord().apply {
                this.journalpostId = journalpostId
                this.mottaksKanal = "NAV_NO"
                this.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
                this.temaNytt = fagsak.stønadstype.tilTema().name
                this.hendelsesId = UUID.randomUUID().toString()
            }

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", journalfoeringHendelseRecord),
            mockk<Acknowledgment>(relaxed = true),
        )

        logger.info("Sendt inn kjøreliste med journalpostId: $journalpostId. Kjøreliste-request: $sendInnKjøreliste")
    }
}

data class SendInnKjøreliste(
    val fagsakEksternId: Long,
    val reiseId: ReiseId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val kjørteDager: List<DagKjørt>,
) : Periode<LocalDate> {
    init {
        require(kjørteDager.all { inneholder(it.dato) }) {
            "Alle kjørte dager må være innenfor periode $fom - $tom. Kjørte dager: $kjørteDager"
        }
    }
}

data class DagKjørt(
    val dato: LocalDate,
    val parkeringsutgift: Int?,
)
