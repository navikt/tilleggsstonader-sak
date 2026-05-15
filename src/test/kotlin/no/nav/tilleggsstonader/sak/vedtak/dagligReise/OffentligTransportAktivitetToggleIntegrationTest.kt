package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreDagligReiseDto
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OffentligTransportAktivitetToggleIntegrationTest : IntegrationTest() {
    val fom = 1 september 2025
    val tom = 30 september 2025
    val tomRevurdering = 31 oktober 2025

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var utbetalingStatusHåndterer: UtbetalingStatusHåndterer

    @Test
    fun `innvilge uten aktivitetId (toggle=AV), revurder og knytt til aktivitet (toggle=PÅ)`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        // 1. Førstegangsbehandling — toggle=AV (default), OT-vilkåret har ingen aktivitetId
        val fgbContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsr(fom, tom, TypeAktivitet.GRUPPEAMO)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeTiltakspenger(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fom, tom)
                    }
                }
            }

        val andelerFgb =
            tilkjentYtelseRepository.findByBehandlingId(fgbContext.behandlingId)!!.andelerTilkjentYtelse
        assertThat(andelerFgb).allMatch { it.type == TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO }

        // Marker andeler som OK slik at revurdering kan opprettes for TSR
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId = fgbContext.behandlingId.toString(),
            melding =
                UtbetalingStatusRecord(
                    status = UtbetalingStatus.OK,
                    detaljer = null,
                    error = null,
                ),
            utbetalingGjelderFagsystem = UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )

        // 2. Slå på toggle
        every { unleashService.isEnabled(Toggle.KAN_KNYTTE_OFFENTLIG_TRANSPORT_TIL_AKTIVITET) } returns true

        // 3. Revurdering — knytt OT-vilkåret til aktiviteten, typeAktivitet utledes fra aktiviteten
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = fgbContext.behandlingId,
            ) {
                aktivitet {
                    oppdater { aktiviteter, behandlingId ->
                        with(aktiviteter.single()) {
                            id to tilLagreVilkårperiodeAktivitet(behandlingId).copy(tom = tomRevurdering)
                        }
                    }
                }
                målgruppe {
                    oppdater { målgrupper, behandlingId ->
                        with(målgrupper.single()) {
                            id to tilLagreVilkårperiodeMålgruppe(behandlingId).copy(tom = tomRevurdering)
                        }
                    }
                }
                vilkår {
                    // Knytt OT-vilkåret til aktiviteten — aktivitetId settes, periode utvides
                    oppdaterDagligReise { vilkår, aktiviteter ->
                        with(vilkår.single()) {
                            id to
                                tilLagreDagligReiseDto().copy(
                                    tom = tomRevurdering,
                                    fakta =
                                        (fakta as FaktaDagligReiseOffentligTransportDto)
                                            .copy(aktivitetId = aktiviteter.single().globalId),
                                )
                        }
                    }
                }
            }

        // 4. Verifiser at typeAktivitet er utledet fra aktiviteten, ikke fra vedtaksperioden
        val andelerRevurdering =
            tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse
        assertThat(andelerRevurdering).isNotEmpty()
        assertThat(andelerRevurdering).allMatch { it.type == TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO }
    }
}
