package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class BoutgifterOpphørIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal lagre og hente opphør`() {
        val fom = 1 januar 2025
        val tom = 28 februar 2025

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                defaultBoutgifterTestdata(
                    fom = fom,
                    tom = tom,
                )
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                vedtak {
                    opphør(
                        årsaker = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "Statsbudsjettet er tomt",
                        opphørsdato = tom.minusDays(10),
                    )
                }
            }

        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.BOUTGIFTER, revurderingId)
                .expectOkWithBody<OpphørBoutgifterResponse>()

        assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtak.årsakerOpphør).containsExactly(ÅrsakOpphør.ANNET)
        assertThat(vedtak.begrunnelse).isEqualTo("Statsbudsjettet er tomt")
    }

    @Test
    fun `skal ikke kunne lagre opphør ved endringer i vedtak før opphørsdato`() {
        val fom = 1 januar 2025
        val tom = 31 mars 2025

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakBoutgifter(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        løpendeutgifterEnBolig(fom.plusMonths(1).tilFørsteDagIMåneden(), tom)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val behandlingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                vilkår {
                    oppdater { vilkårsvurdering ->
                        with(vilkårsvurdering.vilkårsett.single()) {
                            SvarPåVilkårDto(
                                id = id,
                                behandlingId = behandlingId,
                                delvilkårsett = delvilkårsett,
                                fom = fom.plusMonths(1).tilFørsteDagIMåneden(),
                                tom = 28 februar 2025,
                                utgift = utgift,
                                erFremtidigUtgift = erFremtidigUtgift,
                                offentligTransport = null,
                            )
                        }
                    }
                }
            }

        kall.vedtak.apiRespons
            .lagreOpphør(
                stønadstype = Stønadstype.BOUTGIFTER,
                behandlingId = behandlingId,
                opphørDto =
                    opphørDto(
                        opphørsdato = 2 mars 2025,
                    ),
            ).expectProblemDetail(
                forventetStatus = HttpStatus.BAD_REQUEST,
                forventetDetail =
                    "Opphør er et ugyldig vedtaksresultat fordi " +
                        "opphørsdato (02.03.2025) er etter tidligste endring (01.03.2025)",
            )
    }
}
