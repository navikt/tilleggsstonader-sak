package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.mottakerPerson
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

internal class BrevmottakerVedtaksbrevRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var brevmottakerRepository: BrevmottakerRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test internal fun `lagre og hent brevmottaker`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val brevmottaker = BrevmottakerVedtaksbrev(
            behandlingId = behandling.id,
            mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
            journalpostId = "123",
            bestillingId = null,
        )

        brevmottakerRepository.insert(brevmottaker)

        val journalpostResultatFraDb = brevmottakerRepository.findByIdOrNull(brevmottaker.id)

        assertThat(journalpostResultatFraDb).isNotNull
        assertThat(journalpostResultatFraDb).usingRecursiveComparison().ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottaker)
        assertThat(journalpostResultatFraDb!!.sporbar.opprettetTid).isCloseTo(
            brevmottaker.sporbar.opprettetTid,
            Assertions.within(1, ChronoUnit.SECONDS),
        )
        assertThat(journalpostResultatFraDb.sporbar.endret.endretTid).isCloseTo(
            brevmottaker.sporbar.endret.endretTid,
            Assertions.within(1, ChronoUnit.SECONDS),
        )
    }

    @Test
    internal fun `skal ikke kunne lagre to journalpostResultat på samme behandling med samme mottaker`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val brevmottaker1 = BrevmottakerVedtaksbrev(
            behandlingId = behandling.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
            journalpostId = "123",
            bestillingId = null,
        )

        val brevmottaker2 = BrevmottakerVedtaksbrev(
            behandlingId = behandling.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
            journalpostId = "123",
            bestillingId = null,
        )

        brevmottakerRepository.insert(brevmottaker1)
        assertThatThrownBy {
            brevmottakerRepository.insert(brevmottaker2)
        }.hasCauseInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    internal fun `skal kunne lagre to journalpostResultat på samme behandling med forskjellige mottakere`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val brevmottaker = BrevmottakerVedtaksbrev(
            behandlingId = behandling.id,
            mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
            journalpostId = "123",
            bestillingId = null,
        )

        val brevmottakerAnnenMottaker = BrevmottakerVedtaksbrev(
            behandlingId = behandling.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
            journalpostId = "123",
            bestillingId = null,
        )

        brevmottakerRepository.insert(brevmottaker)
        brevmottakerRepository.insert(brevmottakerAnnenMottaker)

        val hentetResultat = brevmottakerRepository.findByIdOrNull(brevmottaker.id)
        val hentetResultatAnnenMottaker = brevmottakerRepository.findByIdOrNull(brevmottakerAnnenMottaker.id)

        assertThat(hentetResultat).isNotNull
        assertThat(hentetResultat).usingRecursiveComparison().ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottaker)

        assertThat(hentetResultat).isNotNull
        assertThat(hentetResultatAnnenMottaker).usingRecursiveComparison()
            .ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottakerAnnenMottaker)
    }
}
