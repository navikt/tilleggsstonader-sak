package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakPerson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException

class BarnRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var barnRepository: BarnRepository

    @Test
    fun `skal kunne opprette barn og hente på behandlingId`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingBarn = barnRepository.insert(behandlingBarn(behandlingId = behandling.id))

        val behandlingBarnFraBehandlingId = barnRepository.findByBehandlingId(behandling.id)

        assertThat(behandlingBarn).isEqualTo(behandlingBarnFraBehandlingId.single())
    }

    @Test
    fun `kan ikke ha 2 barn med samme ident på en behandling`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        barnRepository.insert(behandlingBarn(behandlingId = behandling.id))

        assertThatThrownBy {
            barnRepository.insert(behandlingBarn(behandlingId = behandling.id))
        }.isInstanceOf(DbActionExecutionException::class.java)
            .rootCause()
            .hasMessageContaining(
                "duplicate key value violates unique constraint \"behandling_barn_behandling_id_person_ident_idx\"",
            )
    }

    @Nested
    inner class FinnIdenterForFagsakPerson {
        val identSøker = "1"
        val identBarn = "1"
        val fagsakPerson = fagsakPerson(setOf(PersonIdent(identSøker)))

        val fagsakLæremidler = fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = Stønadstype.LÆREMIDLER)
        val fagsakTilsynBarn = fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = Stønadstype.BARNETILSYN)

        val behandlingTilsynBarn = behandling(fagsakTilsynBarn)
        val barnTilsynBarn = behandlingBarn(behandlingId = behandlingTilsynBarn.id, personIdent = identBarn)

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettPerson(fagsakPerson)
        }

        @Test
        fun `finner ikke ident hvis fagsakPersonId ikke finnes`() {
            assertThat(barnRepository.finnIdenterTilFagsakPersonId(FagsakPersonId.random())).isEmpty()
        }

        @Test
        fun `skal finne ident til barn kobler til fagsakPersonId`() {
            testoppsettService.lagreFagsak(fagsakLæremidler)
            testoppsettService.lagreFagsak(fagsakTilsynBarn)
            testoppsettService.lagre(behandlingTilsynBarn, opprettGrunnlagsdata = false)
            barnRepository.insert(barnTilsynBarn)

            assertThat(barnRepository.finnIdenterTilFagsakPersonId(fagsakPerson.id))
                .containsExactly(identBarn)
        }
    }
}
