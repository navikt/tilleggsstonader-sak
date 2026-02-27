package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningRevurderingTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `forlengelse av reise der perioden allerede har blitt utbetalt skal validere feil`() {
        val dagensDato = LocalDate.now()
        val fom = dagensDato.minusDays(46)
        val tom = dagensDato.plusDays(42)

        val reiser =
            lagreDagligReiseDto(
                fom = fom,
                tom = dagensDato.plusDays(7),
                fakta =
                    FaktaDagligReiseOffentligTransportDto(
                        reisedagerPerUke = 2,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 800,
                    ),
            )

        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
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
                        add { _, _ -> reiser }
                    }
                }
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                vilkår {
                    oppdaterDagligReise { vilkårDagligReise ->
                        // Utvider tom og antall reisedager
                        vilkårDagligReise.single().id to
                            reiser.copy(
                                tom = tom,
                                fakta = (reiser.fakta as FaktaDagligReiseOffentligTransportDto).copy(reisedagerPerUke = 5),
                            )
                    }
                }
            }

        gjennomførBeregningSteg(
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ).expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath(
                "$.detail",
            ).isEqualTo(
                """
                I den revurderte beregningen vil en allerede utbetalt periode med enkeltbilletter bli endret 
                til en periode med månedskort, som kan være til ugunst for søker. For å hindre dette kan du legge 
                inn en ny reise i stedet for å forlenge den eksisterende.
                """.trimIndent(),
            )
    }

    @Test
    fun `skal være lov å redigere billettpriser`() {
        val dagensDato = LocalDate.now()
        val fom = dagensDato.minusDays(46)
        val tom = dagensDato.plusDays(42)

        val reiser =
            lagreDagligReiseDto(
                fom = fom,
                tom = dagensDato.plusDays(7),
                fakta =
                    FaktaDagligReiseOffentligTransportDto(
                        reisedagerPerUke = 5,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 1000,
                    ),
            )

        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
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
                        add { _, _ -> reiser }
                    }
                }
            }

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingId,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                vilkår {
                    oppdaterDagligReise { vilkårDagligReise ->
                        // Utvider tom og antall reisedager
                        vilkårDagligReise.single().id to
                            reiser.copy(
                                fakta = (reiser.fakta as FaktaDagligReiseOffentligTransportDto).copy(prisTrettidagersbillett = 800),
                            )
                    }
                }
            }

        gjennomførBeregningSteg(
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ).expectStatus().isOk
    }
}

private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeAktivitet(behandlingId, fom = LocalDate.now().minusDays(46), tom = LocalDate.now().plusDays(42))

private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
    lagreVilkårperiodeMålgruppe(behandlingId, fom = LocalDate.now().minusDays(46), tom = LocalDate.now().plusDays(42))
