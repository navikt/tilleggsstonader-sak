package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeValideringUtilsTest {
    @Nested
    inner class ValiderIngenEndringerFørRevurderFra {
        val vedtaksperiodeJanFeb =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            )

        val vedtaksperiodeMars =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 3, 1),
                tom = LocalDate.of(2025, 3, 31),
            )

        val vedtaksperiodeApril =
            lagVedtaksperiode(
                fom = LocalDate.of(2025, 4, 1),
                tom = LocalDate.of(2025, 4, 30),
            )

        val vedtaksperioderJanFeb = listOf(vedtaksperiodeJanFeb)
        val vedtaksperioderJanMars = listOf(vedtaksperiodeJanFeb, vedtaksperiodeMars)
        val førsteMars: LocalDate = LocalDate.of(2025, 3, 1)
        val femtendeMars: LocalDate = LocalDate.of(2025, 3, 15)
        val førsteApril: LocalDate = LocalDate.of(2025, 4, 1)

        @Test
        fun `kaster ikke feil ved ingen revurder fra og ingen gamle perioder (førstegangsbehandling)`() {
            assertDoesNotThrow {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioder = vedtaksperioderJanMars,
                    vedtaksperioderForrigeBehandling = emptyList(),
                    revurderFra = null,
                )
            }
        }

        @Nested
        inner class NyPeriode {
            @Test
            fun `kaster ikke feil ved ny periode som starter etter revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanMars,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved ny periode med fom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved ny periode med fom og tom før revuder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }
        }

        @Test
        fun `kaster feil ved ny periode som er lik eksisterende periode lagt til før revuder fra`() {
            val nyeVedtaksperioder =
                listOf(
                    vedtaksperiodeJanFeb,
                    vedtaksperiodeJanFeb.copy(id = UUID.randomUUID()),
                )

            val feil =
                assertThrows<ApiFeil> {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = nyeVedtaksperioder,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanFeb,
                        revurderFra = førsteMars,
                    )
                }
            assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
        }

        @Test
        fun `kaster feil ved nye perioder før revurder fra etter opphør med ingen eksisterende vedtaksperioder`() {
            val feil =
                assertThrows<ApiFeil> {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanMars,
                        vedtaksperioderForrigeBehandling = emptyList(),
                        revurderFra = førsteMars,
                    )
                }
            assertThat(feil).hasMessage("Det er ikke tillat å legge til nye perioder før revurder fra dato")
        }

        @Nested
        inner class EndretPeriode {
            @Test
            fun `kaster ikke feil ved fom før revurder fra og tom etter revurder fra, der tom flyttet fremover i tid`() {
                val nyeVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeMars.copy(tom = LocalDate.of(2025, 4, 10)),
                    )

                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = nyeVedtaksperioder,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                        revurderFra = femtendeMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved tom flyttet til før revurder fra`() {
                val nyeVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeMars.copy(tom = LocalDate.of(2025, 3, 10)),
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = nyeVedtaksperioder,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom flyttet til før revurder fra`() {
                val gamleVedtaksperioder =
                    listOf(
                        vedtaksperiodeJanFeb,
                        vedtaksperiodeApril,
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanMars,
                            vedtaksperioderForrigeBehandling = gamleVedtaksperioder,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der tom flyttes fremover i tid, men fortsatt før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 3, 31))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der tom flyttes fremover i tid forbi revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 5, 31))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved fom og tom før revurder fra der fom flyttes fremover i tid, men fortsatt før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(fom = LocalDate.of(2025, 1, 3))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster ikke feil når forlenger fom som er dagen før revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 3, 31))),
                        vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil når avkorter fom som er dagen før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(fom = LocalDate.of(2025, 2, 27))),
                            vedtaksperioderForrigeBehandling = listOf(vedtaksperiodeJanFeb),
                            revurderFra = førsteMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster ikke feil når avkorter tom til dagen før revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = listOf(vedtaksperiodeJanFeb.copy(tom = LocalDate.of(2025, 2, 28))),
                        vedtaksperioderForrigeBehandling =
                            listOf(
                                vedtaksperiodeJanFeb.copy(
                                    tom =
                                        LocalDate.of(
                                            2025,
                                            3,
                                            31,
                                        ),
                                ),
                            ),
                        revurderFra = førsteMars,
                    )
                }
            }
        }

        @Nested
        inner class SlettetPeriode {
            @Test
            fun `kaster ikke feil ved slettet perioder etter revurder fra`() {
                assertDoesNotThrow {
                    validerIngenEndringerFørRevurderFra(
                        vedtaksperioder = vedtaksperioderJanFeb,
                        vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                        revurderFra = førsteMars,
                    )
                }
            }

            @Test
            fun `kaster feil ved slettet periode med fom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanFeb,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = femtendeMars,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }

            @Test
            fun `kaster feil ved slettet periode med fom og tom før revurder fra`() {
                val feil =
                    assertThrows<ApiFeil> {
                        validerIngenEndringerFørRevurderFra(
                            vedtaksperioder = vedtaksperioderJanFeb,
                            vedtaksperioderForrigeBehandling = vedtaksperioderJanMars,
                            revurderFra = førsteApril,
                        )
                    }
                assertThat(feil).hasMessage("Det er ikke tillat å legg til, endre eller slette perioder fra før revurder fra dato")
            }
        }
    }

    private fun lagVedtaksperiode(
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = UUID.randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
