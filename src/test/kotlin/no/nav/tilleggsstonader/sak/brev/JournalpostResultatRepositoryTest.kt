package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import java.time.temporal.ChronoUnit

internal class JournalpostResultatRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var journalpostResultatRepository: JournalpostResultatRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `lagre og hent journalpostResultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val journalpostResultat = JournalpostResultat(
            behandlingId = behandling.id,
            mottakerId = fagsak.hentAktivIdent(),
            journalpostId = "123",
            bestillingId = null,
        )

        journalpostResultatRepository.insert(journalpostResultat)

        val journalpostResultatFraDb = journalpostResultatRepository.findByIdOrNull(journalpostResultat.id)

        assertThat(journalpostResultatFraDb).isNotNull
        assertThat(journalpostResultatFraDb).usingRecursiveComparison().ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(journalpostResultat)
        assertThat(journalpostResultatFraDb!!.sporbar.opprettetTid).isCloseTo(
            journalpostResultat.sporbar.opprettetTid,
            Assertions.within(1, ChronoUnit.SECONDS),
        )
        assertThat(journalpostResultatFraDb.sporbar.endret.endretTid).isCloseTo(
            journalpostResultat.sporbar.endret.endretTid,
            Assertions.within(1, ChronoUnit.SECONDS),
        )
    }

    @Test
    internal fun `skal ikke kunne lagre to journalpostResultat på samme behandling med samme mottaker`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val journalpostResultat1 = JournalpostResultat(
            behandlingId = behandling.id,
            mottakerId = fagsak.hentAktivIdent(),
            journalpostId = "123",
            bestillingId = null,
        )

        val journalpostResultat2 = JournalpostResultat(
            behandlingId = behandling.id,
            mottakerId = fagsak.hentAktivIdent(),
            journalpostId = "123",
            bestillingId = null,
        )

        journalpostResultatRepository.insert(journalpostResultat1)
        assertThatThrownBy {
            journalpostResultatRepository.insert(journalpostResultat2)
        }.hasCauseInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    internal fun `skal kunne lagre to journalpostResultat på samme behandling med forskjellige mottakere`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val journalpostResultat = JournalpostResultat(
            behandlingId = behandling.id,
            mottakerId = fagsak.hentAktivIdent(),
            journalpostId = "123",
            bestillingId = null,
        )

        val annenMottakerId = "annenMottakerIdent"
        val journalpostResultatAnnenMottaker = JournalpostResultat(
            behandlingId = behandling.id,
            mottakerId = annenMottakerId,
            journalpostId = "123",
            bestillingId = null,
        )

        journalpostResultatRepository.insert(journalpostResultat)
        journalpostResultatRepository.insert(journalpostResultatAnnenMottaker)

        val hentetResultat = journalpostResultatRepository.findByBehandlingIdAndMottakerId(behandling.id, fagsak.hentAktivIdent())
        val hentetResultatAnnenMottaker =
            journalpostResultatRepository.findByBehandlingIdAndMottakerId(behandling.id, annenMottakerId)

        assertThat(hentetResultat).isNotNull
        assertThat(hentetResultat).usingRecursiveComparison().ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(journalpostResultat)

        assertThat(hentetResultatAnnenMottaker).isNotNull
        assertThat(hentetResultatAnnenMottaker).usingRecursiveComparison()
            .ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(journalpostResultatAnnenMottaker)
    }
}
