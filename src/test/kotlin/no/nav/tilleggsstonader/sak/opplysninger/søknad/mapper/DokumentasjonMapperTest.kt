package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.util.JournalpostUtil.lagDokument
import no.nav.tilleggsstonader.sak.util.JournalpostUtil.lagJournalpost
import no.nav.tilleggsstonader.sak.util.SøknadUtil.lagDokumentasjon
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokument as DokumentDomain

class DokumentasjonMapperTest {

    val vedlegg1 = Dokument(UUID.fromString("6782bb66-4d5e-491e-a8a5-dff8a35b0982"), "tittel")
    val vedlegg2 = Dokument(UUID.fromString("dab1e324-0a27-4fd1-ad67-c4e962b2b7c2"), "tittel")

    val dokumentasjon = listOf(
        lagDokumentasjon(vedlegg = listOf(vedlegg1)),
        lagDokumentasjon(vedlegg = listOf(vedlegg2), harSendtInn = true, barnId = "barnId"),
    )

    val skjema = søknadskjemaBarnetilsyn(dokumentasjon = dokumentasjon).skjema

    val journalpost = lagJournalpost(
        dokumenter = listOf(
            lagDokument("vedlegg1", vedlegg1.id.toString()),
            lagDokument("vedlegg2", vedlegg2.id.toString()),
        ),
    )

    @Test
    fun `skal mappe vedlegg fra søknaden til vedlegg fra journalposten`() {
        val data = DokumentasjonMapper.mapDokumentasjon(skjema, journalpost)
        assertThat(data).containsExactlyInAnyOrder(
            Dokumentasjon(
                type = Vedleggstype.UTGIFTER_PASS_SFO_AKS_BARNEHAGE,
                dokumenter = listOf(DokumentDomain("vedlegg1")),
                identBarn = null,
            ),
            Dokumentasjon(
                type = Vedleggstype.UTGIFTER_PASS_SFO_AKS_BARNEHAGE,
                dokumenter = listOf(DokumentDomain("vedlegg2")),
                identBarn = "barnId",
            ),
        )
    }

    @Test
    fun `skal feile hvis man ikke finner vedlegg i journalpost`() {
        assertThatThrownBy {
            DokumentasjonMapper.mapDokumentasjon(skjema, lagJournalpost())
        }.hasMessageContaining("Finner ikke vedlegg i journalpost")
    }
}
