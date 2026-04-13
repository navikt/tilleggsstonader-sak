package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingType
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.henlagtBehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

internal class OpprettBehandlingUtilTest {
    private val fagsak = fagsak()

    // TODO: Sjekk disse testene opp mot BehandlingServiceTest (mye overlapp)
    @Nested
    inner class UtledBehandlingType {
        @Test
        fun `hvis man kun har henlagte så skal neste type være førstegangsbehandling`() {
            assertThat(utledBehandlingType(listOf(henlagtBehandling()), BehandlingÅrsak.SØKNAD)).isEqualTo(
                BehandlingType.FØRSTEGANGSBEHANDLING,
            )

            val henlangteBehandlinger =
                listOf(
                    henlagtBehandling(),
                    henlagtBehandling(),
                )
            assertThat(
                utledBehandlingType(
                    henlangteBehandlinger,
                    BehandlingÅrsak.SØKNAD,
                ),
            ).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        }

        @Test
        fun `hvis man har en ferdigstilt behandling som ikke er henlagt så blir neste behandling revurdering`() {
            assertThat(
                utledBehandlingType(
                    tidligereBehandlinger =
                        listOf(
                            behandling(
                                resultat = BehandlingResultat.AVSLÅTT,
                                status = BehandlingStatus.FERDIGSTILT,
                            ),
                        ),
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
            assertThat(
                utledBehandlingType(
                    tidligereBehandlinger =
                        listOf(
                            behandling(
                                resultat = BehandlingResultat.INNVILGET,
                                status = BehandlingStatus.FERDIGSTILT,
                            ),
                        ),
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
            assertThat(
                utledBehandlingType(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.OPPHØRT,
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
        }

        @Test
        fun `hvis man har en innvilget og senere en henlagt behandling er det fortsatt revurdering`() {
            assertThat(
                utledBehandlingType(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.INNVILGET,
                            vedtakstidspunkt = LocalDateTime.now().minusDays(1),
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                        henlagtBehandling(),
                    ),
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
        }
    }

    @Nested
    inner class Førstegangsbehandling {
        @Test
        fun `mulig å lage behandling når det ikke finnes behandling fra før`() {
            validerKanOppretteNyBehandling(
                stønadstype = Stønadstype.BARNETILSYN,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                listOf(),
                null,
            )
        }

        @Test
        fun `det skal være mulig å opprette hvis eksisterende behandling er henlagt førstegangsbehandling`() {
            validerKanOppretteNyBehandling(
                stønadstype = Stønadstype.BARNETILSYN,
                BehandlingType.FØRSTEGANGSBEHANDLING,
                listOf(henlagtBehandling()),
                null,
            )
        }

        @Test
        fun `det skal ikke være mulig å opprette hvis eksisterende behandling er en revurdering`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                    type = BehandlingType.REVURDERING,
                )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.BARNETILSYN,
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    listOf(behandling),
                    behandling,
                )
            }.hasMessage("Kan ikke opprette en førstegangsbehandling når forrige behandling ikke er en førstegangsbehandling")
        }

        @Test
        fun `det skal ikke være mulig å opprette hvis eksisterende behandling er avslått førstegangsbehandling`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.AVSLÅTT,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.BARNETILSYN,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    tidligereBehandlinger = listOf(behandling),
                    sisteIverksatteBehandlinger = null,
                )
            }.hasMessage("Kan ikke opprette en førstegangsbehandling når siste behandling ikke er henlagt")
        }

        @Test
        fun `det skal være mulig å opprette en revurdering når det finnes en førstegangsbehandling på vent`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.IKKE_SATT,
                    status = BehandlingStatus.SATT_PÅ_VENT,
                )

            assertThatNoException().isThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.BARNETILSYN,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    tidligereBehandlinger = listOf(behandling),
                    sisteIverksatteBehandlinger = null,
                )
            }
        }
    }

    @Nested
    inner class Revurdering {
        @Test
        fun `det skal være mulig å opprette en revurdering hvis eksisterende behandling er avslått førstegangsbehandling`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.AVSLÅTT,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            validerKanOppretteNyBehandling(
                stønadstype = Stønadstype.BARNETILSYN,
                behandlingType = BehandlingType.REVURDERING,
                tidligereBehandlinger = listOf(behandling),
                sisteIverksatteBehandlinger = null,
            )
        }

        @Test
        fun `det skal ikke være mulig å opprette en revurdering om eksisterende behandling er henlagt`() {
            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.BARNETILSYN,
                    behandlingType = BehandlingType.REVURDERING,
                    tidligereBehandlinger = listOf(henlagtBehandling(fagsak = fagsak)),
                    sisteIverksatteBehandlinger = null,
                )
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }

        @Test
        fun `skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.BARNETILSYN,
                    behandlingType = BehandlingType.REVURDERING,
                    tidligereBehandlinger = listOf(),
                    sisteIverksatteBehandlinger = null,
                )
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }
    }

    @Nested
    inner class Kjøreliste {
        @Test
        fun `det skal være mulig å opprette en kjørelistebehandling om det finnes en iverksatt behandling`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                )

            assertThatNoException().isThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    behandlingType = BehandlingType.KJØRELISTE,
                    tidligereBehandlinger = listOf(behandling),
                    sisteIverksatteBehandlinger = behandling,
                )
            }

            assertThatNoException().isThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    behandlingType = BehandlingType.KJØRELISTE,
                    tidligereBehandlinger = listOf(behandling),
                    sisteIverksatteBehandlinger = behandling,
                )
            }
        }

        @Test
        fun `det skal ikke være mulig å opprette en kjørelistebehandling om det ikke finnes en iverksatt behandling`() {
            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                    behandlingType = BehandlingType.KJØRELISTE,
                    tidligereBehandlinger = listOf(),
                    sisteIverksatteBehandlinger = null,
                )
            }.hasMessage("Det finnes ikke en tidligere iverksatt behandling på fagsaken")
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `det skal ikke være mulig å opprette en kjørelistebehandling på noe annet enn daglig reise`(stønadstype: Stønadstype) {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                )

            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    stønadstype = stønadstype,
                    behandlingType = BehandlingType.KJØRELISTE,
                    tidligereBehandlinger = listOf(behandling),
                    sisteIverksatteBehandlinger = behandling,
                )
            }.hasMessage("Det er ikke lov å opprette en kjørelistebehandling på stønadstype $stønadstype")
        }
    }
}
