package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningStegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import org.junit.jupiter.api.Test

class InnvilgeReiseTilSamlingIntegrationTest : IntegrationTest() {
    @Test
    fun `kan beregne reise til samling offentlig transport`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fomNay = 1 januar 2025
        val tomNay = 31 januar 2025

        val behandlingContextNay =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(fomNay, tomNay)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fomNay, tomNay)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(fomNay, tomNay)
                    }
                }
            }
// Innvilgelse Reise til samling er ikke implementert ennå, so forventer vi isNotFound
        gjennomførBeregningStegKall(behandlingContextNay.behandlingId, Stønadstype.REISE_TIL_SAMLING_TSO)
            .expectStatus()
            .isNotFound
    }
}
