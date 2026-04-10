package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBilSatsForDelperiode
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatEkstrakostnader
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class AvklartKjørelisteValideringTest {
    val mandag = 5 januar 2026
    val tirsdag = 6 januar 2026
    val lørdag = 10 januar 2026
    val nesteMandag = 12 januar 2026

    val reiseId = ReiseId.random()

    @Nested
    inner class ValiderAtAlleDagerIKjørelistaErInnenForRammevedtaket {
        @Test
        fun `happy case - innsendte dager er innenfor rammevedtak`() {
            val ramme = rammeForReise(fom = mandag, tom = lørdag)
            val kjøreliste =
                KjørelisteUtil.kjøreliste(
                    reiseId = reiseId,
                    periode = Datoperiode(mandag, tirsdag),
                    kjørteDager = listOf(KjørelisteUtil.KjørtDag(mandag, null)),
                )

            assertDoesNotThrow {
                validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(ramme, kjøreliste)
            }
        }

        @Test
        fun `skal kaste feil dersom innsendte dager er før rammevedtaket starter`() {
            val ramme = rammeForReise(fom = tirsdag, tom = lørdag)
            val kjøreliste =
                KjørelisteUtil.kjøreliste(
                    reiseId = reiseId,
                    periode = Datoperiode(mandag, tirsdag),
                    kjørteDager = listOf(KjørelisteUtil.KjørtDag(mandag, null)),
                )

            assertThatThrownBy {
                validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(ramme, kjøreliste)
            }.isInstanceOf(Feil::class.java).hasMessageContaining("Kjøreliste er ikke innenfor rammevedtaket")
        }

        @Test
        fun `skal kaste feil dersom innsendte dager er etter rammevedtaket slutter`() {
            val ramme = rammeForReise(fom = mandag, tom = tirsdag)
            val kjøreliste =
                KjørelisteUtil.kjøreliste(
                    reiseId = reiseId,
                    periode = Datoperiode(mandag, lørdag),
                    kjørteDager = listOf(KjørelisteUtil.KjørtDag(mandag, null)),
                )

            assertThatThrownBy {
                validerAtAlleDagerIKjørelistaErInnenForRammevedtaket(ramme, kjøreliste)
            }.isInstanceOf(Feil::class.java).hasMessageContaining("Kjøreliste er ikke innenfor rammevedtaket")
        }
    }

    @Nested
    inner class ValiderOppdatertAvklartKjørtUke {
        @Test
        fun `skal ikke kaste feil dersom oppdaterte dager er innenfor rammevedtak og samsvarer med det bruker har sendt inn`() {
            val oppdaterteDager =
                listOf(
                    avklartKjørtDag(mandag, GodkjentGjennomførtKjøring.JA),
                    avklartKjørtDag(tirsdag, GodkjentGjennomførtKjøring.JA),
                )

            val kjørelisteDager =
                listOf(
                    kjørelisteDag(mandag, harKjørt = true),
                    kjørelisteDag(tirsdag, harKjørt = true),
                )

            val ramme = rammeForReise(fom = mandag, tom = tirsdag, reisedagerPerUke = 2)

            assertDoesNotThrow {
                validerOppdatertAvklartKjørtUke(
                    oppdaterteDager = oppdaterteDager,
                    ukeSomSkalOppdateres = mandag.tilUkeIÅr(),
                    rammevedtak = ramme,
                    innsendteKjørelisteDager = kjørelisteDager,
                )
            }
        }

        @Test
        fun `skal kaste feil dersom antall godkjente dager overskrider rammevedtaket`() {
            val oppdaterteDager =
                listOf(
                    avklartKjørtDag(mandag, GodkjentGjennomførtKjøring.JA),
                    avklartKjørtDag(tirsdag, GodkjentGjennomførtKjøring.JA),
                )
            val kjørelisteDager =
                listOf(
                    kjørelisteDag(mandag, harKjørt = true),
                    kjørelisteDag(tirsdag, harKjørt = true),
                )
            val ramme = rammeForReise(fom = mandag, tom = tirsdag, reisedagerPerUke = 1)

            validerKasterFeilVedOppdatering(
                oppdaterteDager = oppdaterteDager,
                rammevedtak = ramme,
                kjøreliste = kjørelisteDager,
                forventetFeilmelding = "Antall godkjente reisedager kan ikke være høyere enn antall dager godkjent i rammevedtak",
            )
        }

        @Test
        fun `skal kaste feil dersom antall godkjente dager overskrider rammevedtaket ved flere delperioder`() {
            val mandagUke1 = 5 januar 2026
            val søndagUke1 = 11 januar 2026
            val mandagUke2 = 12 januar 2026
            val søndagUke2 = 18 januar 2026

            val oppdaterteDager =
                listOf(
                    avklartKjørtDag(mandagUke2, GodkjentGjennomførtKjøring.JA),
                    avklartKjørtDag(mandagUke2.plusDays(1), GodkjentGjennomførtKjøring.JA),
                )
            val kjørelisteDager =
                listOf(
                    kjørelisteDag(mandagUke2, harKjørt = true),
                    kjørelisteDag(mandagUke2.plusDays(1), harKjørt = true),
                )

            val ramme =
                rammeForReise(
                    fom = mandagUke1,
                    tom = søndagUke2,
                    delperioder =
                        listOf(
                            RammeForReiseMedPrivatBilDelperiode(
                                fom = mandagUke1,
                                tom = søndagUke1,
                                ekstrakostnader = RammeForReiseMedPrivatEkstrakostnader(null, null),
                                reisedagerPerUke = 2,
                                satser = listOf(rammeForReiseMedPrivatBilSatsForDelperiode(fom = mandagUke1, tom = søndagUke1)),
                            ),
                            RammeForReiseMedPrivatBilDelperiode(
                                fom = mandagUke2,
                                tom = søndagUke2,
                                ekstrakostnader = RammeForReiseMedPrivatEkstrakostnader(null, null),
                                reisedagerPerUke = 1,
                                satser = listOf(rammeForReiseMedPrivatBilSatsForDelperiode(fom = mandagUke2, tom = søndagUke2)),
                            ),
                        ),
                )

            validerKasterFeilVedOppdatering(
                oppdaterteDager = oppdaterteDager,
                rammevedtak = ramme,
                kjøreliste = kjørelisteDager,
                ukeSomSkalOppdateres = mandagUke2.tilUkeIÅr(),
                forventetFeilmelding = "Antall godkjente reisedager kan ikke være høyere enn antall dager godkjent i rammevedtak",
            )
        }

        @Test
        fun `skal kaste feil dersom en dag tilhører en annen uke`() {
            val oppdaterteDager = listOf(avklartKjørtDag(dato = nesteMandag, GodkjentGjennomførtKjøring.NEI))
            val ramme = rammeForReise(reisedagerPerUke = 5)

            validerKasterFeilVedOppdatering(
                oppdaterteDager = oppdaterteDager,
                rammevedtak = ramme,
                forventetFeilmelding = "Alle dager må være innenfor uken som skal oppdateres",
            )
        }

        @Nested
        inner class GyldigeVerdier {
            @Test
            fun `skal kaste feil dersom godkjentGjennomførtKjøring er IKKE_VURDERT`() {
                val oppdatertAvklartDag =
                    avklartKjørtDag(dato = mandag, godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertAvklartDag),
                    forventetFeilmelding = "Alle dager som oppdateres må være huket av som enten godkjent eller ikke godkjent",
                )
            }

            @Test
            fun `skal kaste feil dersom parkeringsutgift er satt men kjøring ikke er godkjent`() {
                val oppdatertAvklartDag =
                    avklartKjørtDag(
                        dato = mandag,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                        parkeringsutgift = 50,
                    )

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertAvklartDag),
                    forventetFeilmelding = "Parkeringsutgift kan kun settes dersom kjøring for dag er godkjent",
                )
            }
        }

        @Nested
        inner class Begrunnelse {
            @Test
            fun `skal ikke kaste feil dersom begrunnelse er oppgitt`() {
                val oppdatertDag =
                    avklartKjørtDag(
                        dato = mandag,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                        parkeringsutgift = 200,
                        begrunnelse = "Saksbehandler har vurdert",
                    )

                val kjørelisteDag = kjørelisteDag(dato = mandag, harKjørt = false)

                assertDoesNotThrow {
                    validerOppdatertAvklartKjørtUke(
                        oppdaterteDager = listOf(oppdatertDag),
                        ukeSomSkalOppdateres = mandag.tilUkeIÅr(),
                        rammevedtak = rammeForReise(),
                        innsendteKjørelisteDager = listOf(kjørelisteDag),
                    )
                }
            }

            @Test
            fun `skal kaste feil dersom parkeringsutgift er over 100 og begrunnelse mangler`() {
                val oppdatertDag =
                    avklartKjørtDag(
                        dato = mandag,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                        parkeringsutgift = 150,
                        begrunnelse = null,
                    )

                val kjørelisteDag = kjørelisteDag(dato = mandag, harKjørt = true, parkeringsutgift = 150)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertDag),
                    kjøreliste = listOf(kjørelisteDag),
                    forventetFeilmelding = "Må oppgi begrunnelse for parkeringsutgift over 100",
                )
            }

            @Test
            fun `skal kaste feil dersom dag ikke finnes i kjørelisten og begrunnelse mangler`() {
                val oppdatertDag =
                    avklartKjørtDag(dato = mandag, godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertDag),
                    kjøreliste = emptyList(),
                    forventetFeilmelding =
                        "Må oppgi begrunnelse for å endre dag ${mandag.norskFormat()}" +
                            " når det ikke finnes en opprinnelig kjøreliste for dagen",
                )
            }

            @Test
            fun `skal kaste feil dersom kjøring godkjennes men bruker ikke har kjørt`() {
                val oppdatertDag =
                    avklartKjørtDag(dato = mandag, godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA)
                val kjørelisteDag = kjørelisteDag(harKjørt = false)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertDag),
                    kjøreliste = listOf(kjørelisteDag),
                    forventetFeilmelding = "Må oppgi begrunnelse for å godkjenne kjøring når bruker ikke har oppgitt å ha kjørt",
                )
            }

            @Test
            fun `skal kaste feil dersom kjøring ikke godkjennes men bruker har kjørt`() {
                val oppdatertDag =
                    avklartKjørtDag(dato = mandag, godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI)
                val kjørelisteDag = kjørelisteDag(harKjørt = true)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertDag),
                    kjøreliste = listOf(kjørelisteDag),
                    forventetFeilmelding = "Må oppgi begrunnelse for å ikke godkjenne kjøring når bruker har oppgitt å ha kjørt",
                )
            }

            @Test
            fun `skal kaste feil dersom parkeringsutgift er endret og begrunnelse mangler`() {
                val oppdatertDag =
                    avklartKjørtDag(
                        dato = mandag,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                        parkeringsutgift = 80,
                    )

                val kjørelisteDag = kjørelisteDag(harKjørt = true, parkeringsutgift = 50)

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(oppdatertDag),
                    kjøreliste = listOf(kjørelisteDag),
                    forventetFeilmelding = "Må oppgi begrunnelse for å endring av parkeringsutgift",
                )
            }

            @Test
            fun `skal kaste feil dersom parkeringsutgift endres fra verdi til null og begrunnelse mangler`() {
                val dag =
                    avklartKjørtDag(
                        dato = mandag,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                        parkeringsutgift = null,
                    )
                val kjørelisteDag = kjørelisteDag(harKjørt = true, parkeringsutgift = 50) // fjernet parkering

                validerKasterFeilVedOppdatering(
                    oppdaterteDager = listOf(dag),
                    kjøreliste = listOf(kjørelisteDag),
                    forventetFeilmelding = "Må oppgi begrunnelse for å endring av parkeringsutgift",
                )
            }
        }
    }

    private fun validerKasterFeilVedOppdatering(
        oppdaterteDager: List<AvklartKjørtDag>,
        rammevedtak: RammeForReiseMedPrivatBil = rammeForReise(),
        kjøreliste: List<KjørelisteDag> = listOf(kjørelisteDag()),
        ukeSomSkalOppdateres: UkeIÅr = mandag.tilUkeIÅr(),
        forventetFeilmelding: String,
    ) {
        assertThatThrownBy {
            validerOppdatertAvklartKjørtUke(
                oppdaterteDager = oppdaterteDager,
                ukeSomSkalOppdateres = ukeSomSkalOppdateres,
                rammevedtak = rammevedtak,
                innsendteKjørelisteDager = kjøreliste,
            )
        }.isInstanceOf(ApiFeil::class.java).hasMessageContaining(forventetFeilmelding)
    }

    private fun avklartKjørtDag(
        dato: LocalDate = mandag,
        godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
        parkeringsutgift: Int? = null,
        begrunnelse: String? = null,
    ) = AvklartKjørtDag(
        dato = dato,
        godkjentGjennomførtKjøring = godkjentGjennomførtKjøring,
        automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
        avvik = emptyList(),
        parkeringsutgift = parkeringsutgift,
        begrunnelse = begrunnelse,
    )

    private fun kjørelisteDag(
        dato: LocalDate = mandag,
        harKjørt: Boolean = true,
        parkeringsutgift: Int? = null,
    ) = KjørelisteDag(dato = dato, harKjørt = harKjørt, parkeringsutgift = parkeringsutgift)

    private fun rammeForReise(
        fom: LocalDate = mandag,
        tom: LocalDate = mandag,
        reisedagerPerUke: Int = 5,
    ) = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = fom, tom = tom, reisedagerPerUke = reisedagerPerUke)

    private fun rammeForReise(
        fom: LocalDate = mandag,
        tom: LocalDate = mandag,
        delperioder: List<RammeForReiseMedPrivatBilDelperiode>,
    ) = rammeForReiseMedPrivatBil(reiseId = reiseId, fom = fom, tom = tom, delperioder = delperioder)
}
