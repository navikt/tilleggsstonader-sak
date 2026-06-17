package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.ekstern.stønad.HarBehandlingRequest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class HarBehandlingUnderArbeidControllerTest : CleanDatabaseIntegrationTest() {
    val ident = "12345678901"
    val identer = setOf(PersonIdent(ident))

    @BeforeEach
    fun setUp() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = identer, stønadstype = Stønadstype.BARNETILSYN))
        testoppsettService.lagre(
            behandling(
                fagsak = fagsak,
                årsak = BehandlingÅrsak.SØKNAD,
                status = BehandlingStatus.UTREDES,
            ),
            opprettGrunnlagsdata = false,
        )
    }

    @Nested
    inner class MedIdentStønadstype {
        @Test
        fun `returnerer true når søknad er under behandling`() {
            val svar = kall.harBehandling.harBehandling(IdentStønadstype(ident, Stønadstype.BARNETILSYN))
            assertThat(svar).isTrue()
        }

        @Test
        fun `returnerer false når det ikke finnes behandling for personen`() {
            val svar = kall.harBehandling.harBehandling(IdentStønadstype("99999999999", Stønadstype.BARNETILSYN))
            assertThat(svar).isFalse()
        }

        @Test
        fun `returnerer false når behandling er ferdigstilt`() {
            val fagsak =
                testoppsettService.lagreFagsak(
                    fagsak(
                        identer = setOf(PersonIdent("11111111111")),
                        stønadstype = Stønadstype.BARNETILSYN,
                    ),
                )
            testoppsettService.lagre(
                behandling(
                    fagsak = fagsak,
                    årsak = BehandlingÅrsak.SØKNAD,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
                opprettGrunnlagsdata = false,
            )

            val svar = kall.harBehandling.harBehandling(IdentStønadstype("11111111111", Stønadstype.BARNETILSYN))
            assertThat(svar).isFalse()
        }

        @Test
        fun `returnerer false for annen stønadstype`() {
            val svar = kall.harBehandling.harBehandling(IdentStønadstype(ident, Stønadstype.LÆREMIDLER))
            assertThat(svar).isFalse()
        }
    }

    @Nested
    inner class MedIdentSkjematype {
        @Test
        fun `returnerer true når søknad er under behandling`() {
            val svar = kall.harBehandling.harBehandling(IdentSkjematype(ident, Skjematype.SØKNAD_BARNETILSYN))
            assertThat(svar).isTrue()
        }

        @Test
        fun `returnerer false når det ikke finnes behandling for personen`() {
            val svar = kall.harBehandling.harBehandling(IdentSkjematype("99999999999", Skjematype.SØKNAD_BARNETILSYN))
            assertThat(svar).isFalse()
        }

        @Test
        fun `returnerer false for annen skjematype`() {
            val svar = kall.harBehandling.harBehandling(IdentSkjematype(ident, Skjematype.SØKNAD_LÆREMIDLER))
            assertThat(svar).isFalse()
        }

        @Test
        fun `returnerer true for daglig reise når en av stønadstype-variantene er under behandling`() {
            val fagsak =
                testoppsettService.lagreFagsak(
                    fagsak(
                        identer = setOf(PersonIdent("22222222222")),
                        stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    ),
                )
            testoppsettService.lagre(
                behandling(
                    fagsak = fagsak,
                    årsak = BehandlingÅrsak.SØKNAD,
                    status = BehandlingStatus.UTREDES,
                ),
                opprettGrunnlagsdata = false,
            )

            val svar = kall.harBehandling.harBehandling(IdentSkjematype("22222222222", Skjematype.SØKNAD_DAGLIG_REISE))
            assertThat(svar).isTrue()
        }
    }

    @Nested
    inner class Sikkerhet {
        @Test
        fun `returnerer 401 ved kall fra annen applikasjon enn soknad-api`() {
            kall.harBehandling.apiRespons
                .harBehandlingMedFeilKlient(HarBehandlingRequest(ident, stønadstype = Stønadstype.BARNETILSYN))
                .expectProblemDetail(HttpStatus.UNAUTHORIZED, "Kallet utføres ikke av en autorisert klient")
        }
    }
}
