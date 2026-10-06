package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
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

    @Test
    fun `skal hente aktivitetspenger til grunnlag når togglen er på`() {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN))
        val fom = 1 januar 2026
        val tom = 31 januar 2026
        val request = slot<YtelsePerioderRequest>()
        val perioder = listOf(YtelsePeriode.Aktivitetspenger(fom = fom, tom = tom))
        val response = YtelsePerioderUtil.tomYtelsePerioderDto().copy(perioder = perioder)
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns true
        every { ytelseClient.hentYtelser(capture(request)) } returns response

        val resultat = service.hentYtelseForGrunnlag(behandling.id, fom, tom)

        assertThat(request.captured).isEqualTo(
            YtelsePerioderRequest(
                ident = behandling.ident,
                fom = fom,
                tom = tom,
                typer =
                    listOf(
                        TypeYtelsePeriode.AAP,
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                        TypeYtelsePeriode.AKTIVITETSPENGER,
                    ),
            ),
        )
        assertThat(resultat).isEqualTo(response)
    }

    @Test
    fun `skal beholde eksisterende grunnlagskilder når aktivitetspengertogglen er av`() {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN))
        val fom = 1 januar 2026
        val tom = 31 januar 2026
        val request = slot<YtelsePerioderRequest>()
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns false
        every { ytelseClient.hentYtelser(capture(request)) } returns YtelsePerioderUtil.tomYtelsePerioderDto()

        service.hentYtelseForGrunnlag(behandling.id, fom, tom)

        assertThat(request.captured).isEqualTo(
            YtelsePerioderRequest(
                ident = behandling.ident,
                fom = fom,
                tom = tom,
                typer =
                    listOf(
                        TypeYtelsePeriode.AAP,
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                    ),
            ),
        )
        assertThat(request.captured.typer).doesNotContain(TypeYtelsePeriode.AKTIVITETSPENGER)
    }
}
