package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningRevurderingServiceTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `ved forlengelse av en reise skal vi bare reberegne perioder som påvirkes av revurderingen`() {
        val førsteJanuar = 1 januar 2025
        val førsteJanuarPlussEnTrettidagersperiode = førsteJanuar.plusDays(30)
        val førsteJanuarPlussToTrettidagersperioder = førsteJanuar.plusDays(60)

        val behandlingId =
            gjennomførEnFørstegangsbehandling(
                reiseFom = førsteJanuar,
                reiseTom = førsteJanuarPlussToTrettidagersperioder,
            )

        endreAlleBeløpTilNoeHeltTulleteStort()

        val førsteJanuarPlussTreTrettidagersperioder = førsteJanuar.plusDays(89)

        // Gjennomfører beregning
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vilkår {
                    oppdaterDagligReise { vilkår ->
                        with(vilkår.single()) {
                            id to tilLagreDagligReiseDto().copy(tom = førsteJanuarPlussTreTrettidagersperioder)
                        }
                    }
                }
            }

        with(hentBeregnedeReiser(revurderingId).single().perioder) {
            assertThat(size).isEqualTo(3)

            // Forventer at første andel, som er langt unna tidligste endring-datoen, ikke blir reberegnet
            assertThat(first().fom).isEqualTo(1 januar 2025)
            assertThat(first().beløp).isEqualTo(999999999)

            // Forventer at andre andel, ikke har endret grunnlaget sitt, ikke blir reberegnet
            assertThat(get(1).fom).isEqualTo(`førsteJanuarPlussEnTrettidagersperiode`)
            assertThat(get(1).beløp).isEqualTo(999999999)

            // Forventer at tredje andel, som er helt ny i revurderingen, blir reberegnet
            assertThat(last().fom).isEqualTo(førsteJanuarPlussToTrettidagersperioder)
            assertThat(last().beløp).isEqualTo(800)
        }
    }

    @Test
    fun `ved forlengelse av en reise skal vi reberegne perioder nær tidligste endring-datoen gitt at de har endret grunnlaget sitt`() {
        val reiseFom = 1 januar 2025
        val reiseOpprinneligTom = 16 februar 2025
        val reiseForlengetTom = 30 mars 2025

        val behandlingId = gjennomførEnFørstegangsbehandling(reiseFom, reiseOpprinneligTom)

        endreAlleBeløpTilNoeHeltTulleteStort()

        // Gjennomfører beregning
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vilkår {
                    oppdaterDagligReise { vilkår ->
                        with(vilkår.single()) {
                            id to tilLagreDagligReiseDto().copy(tom = reiseForlengetTom)
                        }
                    }
                }
            }

        with(hentBeregnedeReiser(revurderingId).single().perioder) {
            assertThat(size).isEqualTo(3)

            // Forventer at første andel, som er langt unna tidligste endring-datoen, ikke blir reberegnet
            assertThat(first().fom).isEqualTo(1 januar 2025)
            assertThat(first().beløp).isEqualTo(999999999)

            // Forventer at andre andel, som har endret tom-dato, blir reberegnet
            assertThat(get(1).fom).isEqualTo(31 januar 2025)
            assertThat(get(1).beløp).isEqualTo(800)

            // Forventer at tredje andel, som er helt ny i revurderingen, blir reberegnet
            assertThat(last().fom).isEqualTo(2 mars 2025)
            assertThat(last().beløp).isEqualTo(800)
        }
    }

    @Test
    fun `hvis en ny helt reise legges til i revurdering, skal andre reiser i førstegangsvedtaket ikke reberegnes`() {
        // Reise i førstegangsvedtaket
        val førsteJanuar = 1 januar 2025
        val truefemteJanuar = 25 januar 2025

        val behandlingId = gjennomførEnFørstegangsbehandling(førsteJanuar, truefemteJanuar)

        endreAlleBeløpTilNoeHeltTulleteStort()

        // Ny reise i revurderingen
        val tjuefjerdeDesember = 24 desember 2024
        val femteJanuar = 5 januar 2025

        // Gjennomfører beregning
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vilkår {
                    opprett {
                        offentligTransport(tjuefjerdeDesember, femteJanuar)
                    }
                }
            }

        val beregnedeReiser = hentBeregnedeReiser(revurderingId)

        // Forventer at første reise (den nye i revurderingen) består av én andel som har blitt reberegnet
        with(beregnedeReiser.first().perioder.single()) {
            assertThat(fom).isEqualTo(tjuefjerdeDesember)
            assertThat(tom).isEqualTo(femteJanuar)
            assertThat(beløp).isEqualTo(720)
        }

        // Forventer at andre andel (den fra førstegnagsvedtaket) ikke har blitt reberegnet
        with(beregnedeReiser.last().perioder.single()) {
            assertThat(fom).isEqualTo(førsteJanuar)
            assertThat(tom).isEqualTo(truefemteJanuar)
            assertThat(beløp).isEqualTo(999999999)
        }
    }

    private fun hentBeregnedeReiser(revurderingId: BehandlingId): List<BeregningsresultatForReiseDto> =
        kall.vedtak
            .hentVedtak(
                Stønadstype.DAGLIG_REISE_TSO,
                revurderingId,
            ).expectOkWithBody<InnvilgelseDagligReiseResponse>()
            .beregningsresultat.offentligTransport!!
            .reiser

    private fun gjennomførEnFørstegangsbehandling(
        reiseFom: LocalDate,
        reiseTom: LocalDate,
    ): BehandlingId =
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            aktivitet {
                opprett {
                    add(::lagreAktivitet)
                }
            }
            målgruppe {
                opprett {
                    add(::lagreMålgruppe)
                }
            }
            vilkår {
                opprett {
                    offentligTransport(reiseFom, reiseTom)
                }
            }
        }

    private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
        lagreVilkårperiodeAktivitet(behandlingId, fom = 1 januar 2024, tom = 30 mars 2026)

    private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
        lagreVilkårperiodeMålgruppe(behandlingId, fom = 1 januar 2024, tom = 30 mars 2026)

    private fun endreAlleBeløpTilNoeHeltTulleteStort() {
        jdbcTemplate.update(
            """
            UPDATE vedtak
            SET data = replace(data::text, '"beløp": 800', '"beløp": 999999999')::jsonb            
            """.trimIndent(),
            emptyMap<String, Any>(),
        )
    }
}
