package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PeriodeGrunnlagAktivitetServiceTest {

    val aktivitetClient = mockk<AktivitetClient>()
    val fagsakPersonService = mockk<FagsakPersonService>()

    val service = AktivitetService(fagsakPersonService, aktivitetClient)

    val personId = FagsakPersonId.random()

    @BeforeEach
    fun setUp() {
        every { fagsakPersonService.hentAktivIdent(personId) } returns "ident"
    }

    @Nested
    inner class HentAktiviteter {
        @Test
        fun `henting av aktiviteter skal filtrere vekk typer som ikke er av typen tiltak`() {
            every { aktivitetClient.hentAktiviteter(any(), any(), any()) } returns listOf(
                aktivitetArenaDto(type = TypeAktivitet.AMO.name, erStønadsberettiget = false),
                aktivitetArenaDto(type = TypeAktivitet.AILOK.name, erStønadsberettiget = false),
            )

            val aktivteter = service.hentAktiviteter(personId)
            assertThat(aktivteter).hasSize(1)
            assertThat(aktivteter.map { it.type }).containsExactly(TypeAktivitet.AMO.name)
        }

        @Test
        fun `skal ignorere typer som ikke er mappet`() {
            every { aktivitetClient.hentAktiviteter(any(), any(), any()) } returns listOf(
                aktivitetArenaDto(type = "FINNER_IKKE", erStønadsberettiget = false),
                aktivitetArenaDto(type = TypeAktivitet.ARBRRHBAG.name, erStønadsberettiget = false),
            )

            val aktivteter = service.hentAktiviteter(personId)
            assertThat(aktivteter).hasSize(1)
            assertThat(aktivteter.map { it.type }).containsExactly(TypeAktivitet.ARBRRHBAG.name)
        }

        /**
         * Eks hvis vi ikke mappet en type ennå så kan vi også gå etter at erStønadsberettiget er satt til true
         * Det finnes dog tiltak av typen erStønadsberettiget=false som også skal være med
         */
        @Test
        fun `i tilfelle erStønadsberettiget er true så skal de aktivitetene være med, de settes kun til true for tiltak`() {
            every { aktivitetClient.hentAktiviteter(any(), any(), any()) } returns listOf(
                aktivitetArenaDto(type = "FINNER_IKKE", erStønadsberettiget = true),
            )

            val aktivteter = service.hentAktiviteter(personId)
            assertThat(aktivteter).hasSize(1)
            assertThat(aktivteter.map { it.type }).containsExactly("FINNER_IKKE")
        }
    }
}
