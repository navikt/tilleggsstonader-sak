package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtelseServiceTest {
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val ytelseClient = mockk<YtelseClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val unleashService = mockk<UnleashService>()
    private val service =
        YtelseService(
            fagsakPersonService = fagsakPersonService,
            ytelseClient = ytelseClient,
            behandlingService = behandlingService,
            unleashService = unleashService,
        )

    @Test
    fun `skal bare be integrasjoner om aktivitetspenger når togglen er på`() {
        val fagsakPersonId = FagsakPersonId.random()
        val ident = "12345678901"
        val request = slot<YtelsePerioderRequest>()
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns ident
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns true
        every { ytelseClient.hentYtelser(capture(request)) } returns YtelsePerioderUtil.tomYtelsePerioderDto()

        service.hentYtelser(fagsakPersonId)

        assertThat(request.captured.typer).contains(TypeYtelsePeriode.AKTIVITETSPENGER)
    }

    @Test
    fun `skal ikke be integrasjoner om aktivitetspenger når togglen er av`() {
        val fagsakPersonId = FagsakPersonId.random()
        val ident = "12345678901"
        val request = slot<YtelsePerioderRequest>()
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns ident
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns false
        every { ytelseClient.hentYtelser(capture(request)) } returns YtelsePerioderUtil.tomYtelsePerioderDto()

        service.hentYtelser(fagsakPersonId)

        assertThat(request.captured.typer).doesNotContain(TypeYtelsePeriode.AKTIVITETSPENGER)
        verify(exactly = 1) { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) }
    }
}
