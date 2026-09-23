package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.OpphørReiseTilSamlingRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingOffentligTransportDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

/**
 * Tester opphør av reise til samling.
 */
class OpphørReiseTilSamlingIntegrationTest(
    @Autowired private val vedtakRepository: VedtakRepository,
) : IntegrationTest() {
    private fun hentOpphør(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<OpphørReiseTilSamling>()

    @Test
    fun `skal kunne opphøre reise til samling`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                // To atskilte reiser (januar og mars). Opphørsdato midt i februar,
                // mellom reisene, korter ned vedtaksperiodene ved å fjerne hele
                // mars-perioden uten å treffe midt i en oppfylt reise.
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 mars 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 mars 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                    opprett {
                        offentligTransportReiseTilSamling(
                            1 mars 2025,
                            31 mars 2025,
                            reiseId = ReiseId.random(),
                        )
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vedtak {
                    opphør(opphørsdato = 15 februar 2025)
                }
            }

        val vedtak = hentOpphør(revurderingId)

        assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtak.opphørsdato).isEqualTo(15 februar 2025)
        assertThat(vedtak.data.vedtaksperioder.maxOf { it.tom }).isEqualTo(31 januar 2025)
    }

    @Test
    fun `skal kunne opphøre hele saken når opphørsdato er før første reise`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                // Aktivitet og målgruppe strekker seg gjennom hele januar-mars, men den eneste
                // reisen ligger i mars. Opphørsdato settes midt i januar, altså god tid før den
                // første (og eneste) reisen. Da skal opphøret fjerne hele vedtaksperioden, og
                // vedtaket blir et opphør av hele saken uten noen gjenværende vedtaksperioder.
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 mars 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 mars 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 mars 2025, 31 mars 2025)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vedtak {
                    opphør(opphørsdato = 1 mars 2025)
                }
            }

        val vedtak = hentOpphør(revurderingId)

        assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(vedtak.opphørsdato).isEqualTo(1 mars 2025)
        assertThat(vedtak.data.vedtaksperioder).isEmpty()
    }

    @Test
    fun `skal ikke kunne opphøre midt i en oppfylt samlingsperiode`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(1 januar 2025, 31 mars 2025)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {}

        // Reisen strekker seg fra januar til mars, og opphørsdato midt i mars
        // treffer midt i den oppfylte samlingsperioden.
        kall.vedtak.apiRespons
            .lagreOpphør(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
                behandlingId = revurderingId,
                opphørDto =
                    OpphørReiseTilSamlingRequest(
                        årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "begrunnelse",
                        opphørsdato = 15 februar 2025,
                    ),
            ).expectProblemDetail(
                forventetStatus = HttpStatus.BAD_REQUEST,
                forventetDetail =
                    "Vi har foreløpig ikke støtte for å beregne når samlingsperioder strekker seg utenfor vedtaksperiodene.",
            )
    }

    @Test
    fun `skal ikke kunne opphøre midt i et endret vilkår`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(1 januar 2025, 31 mars 2025)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        // Endrer beløpet på reisen (uten å endre perioden), slik at vilkåret får status ENDRET
        // med samme tom (31. mars) som strekker seg langt forbi den tiltenkte opphørsdatoen.
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                vilkår {
                    endreReiseTilSamling {
                        copy(
                            fakta =
                                (fakta as FaktaReiseTilSamlingOffentligTransportDto)
                                    .copy(utgifterOffentligTransport = 500.toBigDecimal()),
                        )
                    }
                }
            }

        kall.vedtak.apiRespons
            .lagreOpphør(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
                behandlingId = revurderingId,
                opphørDto =
                    OpphørReiseTilSamlingRequest(
                        årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "begrunnelse",
                        opphørsdato = 15 februar 2025,
                    ),
            ).expectProblemDetail(
                forventetStatus = HttpStatus.BAD_REQUEST,
                forventetDetail =
                    "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter opphørsdato",
            )
    }
}
