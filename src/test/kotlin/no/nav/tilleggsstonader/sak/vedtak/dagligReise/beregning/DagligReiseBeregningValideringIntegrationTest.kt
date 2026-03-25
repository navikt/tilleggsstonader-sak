package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.junit.jupiter.api.Test

class DagligReiseBeregningValideringIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `Kaster feil hvis validering ikke blir gjort`() {
        val fom = 1 januar 2026
        val tom = 1 juni 2026

        val fomReise1 = 1 januar 2026
        val tomReise1 = 19 februar 2026

        val fomReise2 = 2 mars 2026
        val tomReise2 = 20 mai 2026

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fomReise1, tomReise1)
                        offentligTransport(fomReise2, tomReise2)
                    }
                }
            }

        val vedtaksperioder = vedtaksperiode(fom, tom, aktivitet = AktivitetType.TILTAK, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)

        val vedtakRequest = InnvilgelseDagligReiseTsoRequest(listOf(vedtaksperioder.tilDto()))

        kall.vedtak.apiRespons
            .lagreEnhetsspesifiktVedtak(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                behandlingId = behandlingId.behandlingId,
                typeVedtakPath = "beregn",
                vedtakDto = vedtakRequest,
                enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD,
            ).expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo(
                "Kan ikke innvilge for valgte perioder fordi det ikke finnes vilkår for reise for alle vedtaksperioder.",
            )
    }
}
