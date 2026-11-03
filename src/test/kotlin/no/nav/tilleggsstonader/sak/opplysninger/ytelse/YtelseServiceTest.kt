package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto.KildeResultatYtelse
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class YtelseServiceTest {
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val ytelseClient = mockk<YtelseClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val service =
        YtelseService(
            fagsakPersonService = fagsakPersonService,
            ytelseClient = ytelseClient,
            behandlingService = behandlingService,
        )

    @ParameterizedTest
    @MethodSource("stønadstypeOgForventedeTyper")
    fun `skal hente og returnere aktivitetspenger til grunnlag`(
        stønadstype: Stønadstype,
        forventedeTyper: List<TypeYtelsePeriode>,
    ) {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = stønadstype))
        val fom = 1 januar 2026
        val tom = 31 januar 2026
        val request = slot<YtelsePerioderRequest>()
        val response =
            YtelsePerioderUtil
                .tomYtelsePerioderDto()
                .copy(perioder = listOf(YtelsePeriode.Aktivitetspenger(fom = fom, tom = tom)))
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { ytelseClient.hentYtelser(capture(request)) } returns response

        val resultat = service.hentYtelseForGrunnlag(behandling.id, fom, tom)

        assertThat(request.captured).isEqualTo(
            YtelsePerioderRequest(
                ident = behandling.ident,
                fom = fom,
                tom = tom,
                typer = forventedeTyper,
            ),
        )
        assertThat(resultat.perioder).containsExactly(YtelsePeriode.Aktivitetspenger(fom = fom, tom = tom))
        assertThat(resultat).isEqualTo(response)
    }

    @Nested
    inner class HarAktivitetspengerIDagEllerFremITid {
        private val ident = "12345678901"
        private val aktivitetspenger = YtelsePeriode.Aktivitetspenger(fom = LocalDate.now(), tom = LocalDate.now())
        private val kildeFeilet =
            KildeResultatYtelse(type = TypeYtelsePeriode.AKTIVITETSPENGER, resultat = ResultatKilde.FEILET)

        @Test
        fun `skal returnere true når bruker har aktivitetspenger`() {
            every { ytelseClient.hentYtelser(any()) } returns
                YtelsePerioderUtil.tomYtelsePerioderDto().copy(perioder = listOf(aktivitetspenger))

            assertThat(service.harAktivitetspengerIDagEllerFremITid(ident)).isTrue()
            assertThat(service.harAktivitetspengerIDagEllerFremITidEllerFeil(ident)).isTrue()
        }

        @Test
        fun `skal returnere false når ung-sak bekrefter at bruker ikke har aktivitetspenger`() {
            every { ytelseClient.hentYtelser(any()) } returns YtelsePerioderUtil.tomYtelsePerioderDto()

            assertThat(service.harAktivitetspengerIDagEllerFremITid(ident)).isFalse()
            assertThat(service.harAktivitetspengerIDagEllerFremITidEllerFeil(ident)).isFalse()
        }

        @Test
        fun `tolerant variant skal returnere false når kallet feiler`() {
            every { ytelseClient.hentYtelser(any()) } throws RuntimeException("ung-sak er nede")

            assertThat(service.harAktivitetspengerIDagEllerFremITid(ident)).isFalse()
        }

        @Test
        fun `tolerant variant skal returnere false når kilden har status FEILET`() {
            every { ytelseClient.hentYtelser(any()) } returns
                YtelsePerioderUtil.tomYtelsePerioderDto().copy(kildeResultat = listOf(kildeFeilet))

            assertThat(service.harAktivitetspengerIDagEllerFremITid(ident)).isFalse()
        }

        @Test
        fun `streng variant skal feile når kallet kaster exception`() {
            every { ytelseClient.hentYtelser(any()) } throws RuntimeException("ung-sak er nede")

            assertThrows<Feil> { service.harAktivitetspengerIDagEllerFremITidEllerFeil(ident) }
        }

        @Test
        fun `streng variant skal feile når kilden har status FEILET`() {
            every { ytelseClient.hentYtelser(any()) } returns
                YtelsePerioderUtil.tomYtelsePerioderDto().copy(kildeResultat = listOf(kildeFeilet))

            assertThrows<Feil> { service.harAktivitetspengerIDagEllerFremITidEllerFeil(ident) }
        }
    }

    companion object {
        @JvmStatic
        fun stønadstypeOgForventedeTyper() =
            listOf(
                Arguments.of(
                    Stønadstype.BARNETILSYN,
                    listOf(
                        TypeYtelsePeriode.AAP,
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                        TypeYtelsePeriode.AKTIVITETSPENGER,
                    ),
                ),
                Arguments.of(
                    Stønadstype.DAGLIG_REISE_TSO,
                    listOf(
                        TypeYtelsePeriode.AAP,
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                        TypeYtelsePeriode.TILTAKSPENGER_TPSAK,
                        TypeYtelsePeriode.TILTAKSPENGER_ARENA,
                        TypeYtelsePeriode.DAGPENGER,
                        TypeYtelsePeriode.AKTIVITETSPENGER,
                    ),
                ),
                Arguments.of(
                    Stønadstype.DAGLIG_REISE_TSR,
                    listOf(
                        TypeYtelsePeriode.AAP,
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        TypeYtelsePeriode.OMSTILLINGSSTØNAD,
                        TypeYtelsePeriode.TILTAKSPENGER_TPSAK,
                        TypeYtelsePeriode.TILTAKSPENGER_ARENA,
                        TypeYtelsePeriode.DAGPENGER,
                        TypeYtelsePeriode.AKTIVITETSPENGER,
                    ),
                ),
            )
    }
}
