package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AvklartKjørelisteUtilsTest {
    private val reiseIdA = ReiseId.random()
    private val reiseIdB = ReiseId.random()
    private val behandlingId = BehandlingId.random()

    // Mandag - fredag, uke 2 2026
    private val mandag = 5 januar 2026
    private val tirsdag = 6 januar 2026
    private val onsdag = 7 januar 2026
    private val torsdag = 8 januar 2026
    private val fredag = 9 januar 2026

    private val rammevedtak = rammeForReiseMedPrivatBil(reiseId = reiseIdA, fom = mandag, tom = fredag, reisedagerPerUke = 5)

    @Test
    fun `skal ikke gi avvik når det ikke finnes andre reisers uker`() {
        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = emptyList(),
            )

        assertThat(avvik).isEmpty()
    }

    @Test
    fun `skal gi avvik når annen reise har kjørt dager samme uke, selv om det er andre ukedager`() {
        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    dagerMedKjøring = setOf(onsdag, torsdag),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).containsExactly(TypeAvvikUke.INNSENDTE_DAGER_OVERLAPPER_MED_DAGER_DEKT_AV_ANNEN_REISE)
    }

    @Test
    fun `skal gi avvik selv om annen reise kun har kjørt én av dagene som overlapper med denne uken`() {
        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    dagerMedKjøring = setOf(fredag),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).containsExactly(TypeAvvikUke.INNSENDTE_DAGER_OVERLAPPER_MED_DAGER_DEKT_AV_ANNEN_REISE)
    }

    @Test
    fun `skal ikke gi avvik når annen reise ikke har kjørt noen dager samme uke`() {
        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    dagerMedKjøring = emptySet(),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).isEmpty()
    }

    @Test
    fun `skal ikke gi avvik når annen reises uke er slettet`() {
        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    dagerMedKjøring = setOf(onsdag, torsdag),
                    avklartKjørtUkeStatus = AvklartKjørtUkeStatus.SLETTET,
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).isEmpty()
    }

    @Test
    fun `skal ikke gi avvik når annen uke tilhører samme reise`() {
        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdA,
                    dagerMedKjøring = setOf(onsdag, torsdag),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).isEmpty()
    }

    @Test
    fun `skal ikke gi avvik når annen reises kjørte dager er en annen uke`() {
        val nesteMandag = mandag.plusWeeks(1)
        val nesteOnsdag = onsdag.plusWeeks(1)

        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    fom = nesteMandag,
                    dagerMedKjøring = setOf(nesteOnsdag),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtak,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik).isEmpty()
    }

    @Test
    fun `skal gi begge avvik når både antall dager overskrides og annen reise har kjørt samme uke`() {
        val rammevedtakMedFåDager = rammeForReiseMedPrivatBil(reiseId = reiseIdA, fom = mandag, tom = fredag, reisedagerPerUke = 1)

        val andreReisersUker =
            listOf(
                avklartKjørtUke(
                    reiseId = reiseIdB,
                    dagerMedKjøring = setOf(onsdag),
                ),
            )

        val avvik =
            utledAvvikForUke(
                rammevedtak = rammevedtakMedFåDager,
                reisedager = kjørelisteDager(mandag, tirsdag),
                avklarteUkerForAndreReiserISammeBehandling = andreReisersUker,
            )

        assertThat(avvik)
            .containsExactlyInAnyOrder(
                TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK,
                TypeAvvikUke.INNSENDTE_DAGER_OVERLAPPER_MED_DAGER_DEKT_AV_ANNEN_REISE,
            )
    }

    private fun kjørelisteDager(vararg datoerMedKjøring: LocalDate): List<KjørelisteDag> =
        (0..4).map { dagOffset ->
            val dato = mandag.plusDays(dagOffset.toLong())
            KjørelisteDag(
                dato = dato,
                harKjørt = dato in datoerMedKjøring,
                parkeringsutgift = null,
            )
        }

    private fun avklartKjørtUke(
        reiseId: ReiseId,
        dagerMedKjøring: Set<LocalDate>,
        fom: LocalDate = mandag,
        avklartKjørtUkeStatus: AvklartKjørtUkeStatus = AvklartKjørtUkeStatus.NY,
    ): AvklartKjørtUke {
        val tom = fom.plusDays(4)
        return AvklartKjørtUke(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            kjørelisteId = KjørelisteId.random(),
            reiseId = reiseId,
            fom = fom,
            tom = tom,
            uke = fom.tilUkeIÅr(),
            status = UkeStatus.OK_AUTOMATISK,
            avvik = emptySet(),
            avklartKjørtUkeStatus = avklartKjørtUkeStatus,
            dager =
                (0..4)
                    .map { dagOffset ->
                        val dato = fom.plusDays(dagOffset.toLong())
                        AvklartKjørtDag(
                            dato = dato,
                            godkjentGjennomførtKjøring =
                                if (dato in dagerMedKjøring) {
                                    GodkjentGjennomførtKjøring.JA
                                } else {
                                    GodkjentGjennomførtKjøring.NEI
                                },
                            automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                            avvik = emptyList(),
                            parkeringsutgift = null,
                            avklartKjørtDagStatus = AvklartKjørtDagStatus.NY,
                        )
                    }.toSet(),
        )
    }
}
