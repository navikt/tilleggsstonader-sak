package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingType
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.utledBehandlingTypeV2
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class OpprettBehandlingUtilTest {
    private val fagsak = fagsak()

    @Nested
    inner class UtledBehandlingType {
        @Test
        fun `hvis man kun har henlagte så skal neste type være førstegangsbehandling`() {
            assertThat(utledBehandlingType(listOf(behandling(resultat = BehandlingResultat.HENLAGT))))
                .isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)

            val behandlinger =
                listOf(
                    behandling(resultat = BehandlingResultat.HENLAGT),
                    behandling(resultat = BehandlingResultat.HENLAGT),
                )
            assertThat(utledBehandlingType(behandlinger)).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        }

        // TODO: Slett når snike i køen er implementert
        @Test
        fun `hvis man har en behandling som ikke er henlagt så blir neste behandling revurdering`() {
            assertThat(utledBehandlingType(listOf(behandling(resultat = BehandlingResultat.IKKE_SATT))))
                .isEqualTo(BehandlingType.REVURDERING)
            assertThat(utledBehandlingType(listOf(behandling(resultat = BehandlingResultat.AVSLÅTT))))
                .isEqualTo(BehandlingType.REVURDERING)
            assertThat(utledBehandlingType(listOf(behandling(resultat = BehandlingResultat.INNVILGET))))
                .isEqualTo(BehandlingType.REVURDERING)
        }

        @Test
        fun `hvis man har en ferdigstilt behandling som ikke er henlagt så blir neste behandling revurdering`() {
            assertThat(
                utledBehandlingTypeV2(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.AVSLÅTT,
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
            assertThat(
                utledBehandlingTypeV2(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.INNVILGET,
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
            assertThat(
                utledBehandlingTypeV2(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.OPPHØRT,
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
        }

        @Test
        fun `hvis man har en innvilget og sen en henlagt er det fortsatt revurdering`() {
            assertThat(
                utledBehandlingType(
                    listOf(
                        behandling(
                            resultat = BehandlingResultat.INNVILGET,
                            vedtakstidspunkt = LocalDateTime.now().minusDays(1),
                            status = BehandlingStatus.FERDIGSTILT,
                        ),
                        behandling(resultat = BehandlingResultat.HENLAGT),
                    ),
                ),
            ).isEqualTo(BehandlingType.REVURDERING)
        }
    }

    @Nested
    inner class Førstegangsbehandling {
        @Test
        fun `mulig å lage behandling når det ikke finnes behandling fra før`() {
            validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf())
        }

        @Test
        fun `det skal være mulig å opprette hvis eksisterende behandling er henlagt førstegangsbehandling`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.HENLAGT,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
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
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
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
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
            }.hasMessage("Kan ikke opprette en førstegangsbehandling når siste behandling ikke er henlagt")
        }

        @Test
        fun `det skal ikke være mulig å opprette en førstegangsbehandling når det finnes en behandling på vent`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.IKKE_SATT,
                    status = BehandlingStatus.SATT_PÅ_VENT,
                )

            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
            }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        fun `det skal ikke være mulig å opprette en revurdering når det finnes en førstegangsbehandling på vent`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.IKKE_SATT,
                    status = BehandlingStatus.SATT_PÅ_VENT,
                )

            assertThatThrownBy {
                validerKanOppretteNyBehandling(
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    tidligereBehandlinger = listOf(behandling),
                    kanHaFlereBehandlingPåSammeFagsak = false,
                )
            }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        fun `det skal være mulig å opprette en revurdering når det finnes en førstegangsbehandling på vent`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.IKKE_SATT,
                    status = BehandlingStatus.SATT_PÅ_VENT,
                )

            // Sjekker at denne ikke kaster feil
            validerKanOppretteNyBehandling(
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                tidligereBehandlinger = listOf(behandling),
                kanHaFlereBehandlingPåSammeFagsak = true,
            )
        }
    }

    @Nested
    inner class Revurdering {
        @Test
        fun `det skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
            val behandlinger =
                listOf(
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.INNVILGET,
                    ),
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                    ),
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.INNVILGET,
                    ),
                )
            assertThatThrownBy { validerKanOppretteNyBehandling(BehandlingType.REVURDERING, behandlinger) }
                .hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        internal fun `skal kunne opprette en ny behandling hvis en tidligere behandling er satt på vent`() {
            val behandlinger =
                listOf(
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.FERDIGSTILT,
                    ),
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.SATT_PÅ_VENT,
                        type = BehandlingType.REVURDERING,
                    ),
                )
            assertThatThrownBy { validerKanOppretteNyBehandling(BehandlingType.REVURDERING, behandlinger) }
                .hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        fun `det skal være mulig å opprette en revurdering hvis eksisterende behandling er avslått førstegangsbehandling`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.AVSLÅTT,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(behandling))
        }

        @Test
        fun `det skal ikke være mulig å opprette en revurdering om eksisterende behandling er henlagt`() {
            val behandling =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.HENLAGT,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(behandling))
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }

        @Test
        fun `skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf())
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }
    }
}
