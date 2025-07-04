package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.assertions.hasCauseMessageContaining
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Endret
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakDomain
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.util.tilFagsakDomain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime

class FagsakRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var eksternFagsakIdRepository: EksternFagsakIdRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `harLøpendeUtbetaling returnerer true for fagsak med ferdigstilt behandling med aktiv utbetaling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling =
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        val andel =
            andelTilkjentYtelse(
                behandling.id,
                fom = LocalDate.now().datoEllerNesteMandagHvisLørdagEllerSøndag(),
                tom = LocalDate.now().datoEllerNesteMandagHvisLørdagEllerSøndag(),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andeler = arrayOf(andel)))

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isTrue()
    }

    @Test
    fun `harLøpendeUtbetaling returnerer true for fagsak med flere aktive ytelser`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling =
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        tilkjentYtelseRepository.insert(
            tilkjentYtelse(
                behandling.id,
                andelTilkjentYtelse(
                    kildeBehandlingId = behandling.id,
                    fom = LocalDate.now().datoEllerNesteMandagHvisLørdagEllerSøndag(),
                ),
            ),
        )
        tilkjentYtelseRepository.insert(
            tilkjentYtelse(
                behandling.id,
                andelTilkjentYtelse(
                    kildeBehandlingId = behandling.id,
                    fom = LocalDate.now().datoEllerNesteMandagHvisLørdagEllerSøndag(),
                ),
            ),
        )

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isTrue()
    }

    @Test
    fun `harLøpendeUtbetaling returnerer false for fagsak med ferdigstilt behandling med inaktiv utbetaling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling =
            testoppsettService.lagre(
                behandling(
                    fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id))

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isFalse()
    }

    @Test
    internal fun `skal ikke være mulig med flere stønader av samme typen for samme person`() {
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        Stønadstype.values().forEach {
            fagsakRepository.insert(fagsakDomain(personId = person.id, stønadstype = it))
        }
        Stønadstype.values().forEach {
            assertThatThrownBy {
                fagsakRepository.insert(
                    fagsakDomain(
                        personId = person.id,
                        stønadstype = it,
                    ),
                )
            }.hasRootCauseInstanceOf(PSQLException::class.java)
                .has(
                    hasCauseMessageContaining(
                        "ERROR: duplicate key value violates " +
                            "unique constraint \"unique_fagsak_person\"",
                    ),
                )
        }
    }

    @Test
    internal fun `2 ulike personer skal kunne ha samme type stønad`() {
        val person1 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        fagsakRepository.insert(fagsakDomain(personId = person1.id, stønadstype = Stønadstype.BARNETILSYN))
        fagsakRepository.insert(fagsakDomain(personId = person2.id, stønadstype = Stønadstype.BARNETILSYN))
    }

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert =
            testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.id).isEqualTo(fagsakPersistert.id)
    }

    @Test
    internal fun findBySøkerIdent() {
        testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakHentetFinnesIkke = fagsakRepository.findBySøkerIdent(setOf("0"), Stønadstype.BARNETILSYN)

        assertThat(fagsakHentetFinnesIkke).isNull()

        val fagsak =
            fagsakRepository.findBySøkerIdent(setOf("12345678901"), Stønadstype.BARNETILSYN)
                ?: error("Finner ikke fagsak")
        val person = fagsakPersonRepository.findByIdOrThrow(fagsak.fagsakPersonId)

        assertThat(person.identer.map { it.ident }).contains("12345678901")
        assertThat(person.identer.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun `skal returnere en liste med fagsaker hvis stønadstypen ikke satt`() {
        val ident = "12345678901"
        val person = testoppsettService.opprettPerson(ident)
        val fagsak1 =
            testoppsettService.lagreFagsak(
                fagsak(
                    person = person,
                    stønadstype = Stønadstype.BARNETILSYN,
                ),
            )
        val fagsak2 =
            testoppsettService.lagreFagsak(
                fagsak(
                    person = person,
                    stønadstype = Stønadstype.LÆREMIDLER,
                ),
            )
        val fagsaker = fagsakRepository.findBySøkerIdent(setOf(ident))

        assertThat(
            fagsaker.forEach { fagsak ->
                val fagsakperson = fagsakPersonRepository.findByIdOrThrow(fagsak.fagsakPersonId)
                assertThat(fagsakperson.identer.size).isEqualTo(1)
                assertThat(fagsakperson.identer.map { it.ident }).contains(ident)
            },
        )

        assertThat(fagsaker).containsExactlyInAnyOrder(fagsak1.tilFagsakDomain(), fagsak2.tilFagsakDomain())
    }

    @Test
    internal fun finnMedEksternId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val findByEksternId =
            fagsakRepository.finnMedEksternId(fagsak.eksternId.id)
                ?: error("Fagsak med ekstern id ${fagsak.eksternId} finnes ikke")

        assertThat(findByEksternId).isEqualTo(fagsak.tilFagsakDomain())
    }

    @Test
    internal fun `findByFagsakPersonIdAndStønadstype - skal finne fagsak`() {
        val person = testoppsettService.opprettPerson("1")
        val barnetilsyn = testoppsettService.lagreFagsak(fagsak(person = person))
        val læremidler = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.LÆREMIDLER))
        testoppsettService.lagreFagsak(fagsak())

        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, Stønadstype.BARNETILSYN)!!.id)
            .isEqualTo(barnetilsyn.id)
        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, Stønadstype.LÆREMIDLER)!!.id)
            .isEqualTo(læremidler.id)
        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, Stønadstype.BOUTGIFTER)).isNull()
    }

    @Test
    internal fun `finnMedEksternId skal gi null når det ikke finnes fagsak for gitt id`() {
        val findByEksternId = fagsakRepository.finnMedEksternId(100000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    internal fun `finnAktivIdent - skal finne aktiv ident`() {
        val fagsak = opprettFagsakMedFlereIdenter()
        testoppsettService.lagreFagsak(fagsak)
        assertThat(fagsakRepository.finnAktivIdent(fagsak.id)).isEqualTo("2")
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        var fagsak = opprettFagsakMedFlereIdenter()
        fagsak = testoppsettService.lagreFagsak(fagsak)
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val finnFagsakTilBehandling = fagsakRepository.finnFagsakTilBehandling(behandling.id)!!

        assertThat(finnFagsakTilBehandling.id).isEqualTo(fagsak.id)
        assertThat(eksternFagsakIdRepository.findByFagsakId(fagsak.id)).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun `skal sette eksternId til 200_000_000 som default`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThat(fagsak.eksternId.id).isGreaterThanOrEqualTo(200_000_000)
    }

    @Test
    internal fun `skal kunne søke opp fagsak basert på forskjellige personidenter - kun ett treff per fagsak`() {
        val fagsakMedFlereIdenter = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter("4", "5", "6"))

        assertThat(fagsakMedFlereIdenter.personIdenter).hasSize(3)
        assertThat(
            fagsakRepository.findBySøkerIdent(
                fagsakMedFlereIdenter.personIdenter.map { it.ident }.toSet(),
                Stønadstype.BARNETILSYN,
            ),
        ).isNotNull
        assertThat(
            fagsakRepository.findBySøkerIdent(setOf(fagsakMedFlereIdenter.personIdenter.map { it.ident }.first())),
        ).hasSize(1)
        assertThat(
            fagsakRepository.findBySøkerIdent(fagsakMedFlereIdenter.personIdenter.map { it.ident }.toSet()),
        ).hasSize(1)
    }

    @Nested
    inner class HentFagsakMetadata {
        @Test
        fun `en fagsak med 2 identer skal hente site opprettede`() {
            val sporbar1ÅrSiden =
                Sporbar(
                    opprettetTid = LocalDateTime.now().minusYears(1),
                    endret = Endret(endretTid = LocalDateTime.now().minusYears(1)),
                )
            val identer =
                setOf(
                    PersonIdent("1", sporbar1ÅrSiden),
                    PersonIdent("2"),
                    PersonIdent("3", sporbar1ÅrSiden),
                )
            val fagsak = testoppsettService.lagreFagsak(fagsak(identer))

            val metadata = fagsakRepository.hentFagsakMetadata(fagsakIder = setOf(fagsak.id))
            assertThat(metadata).hasSize(1)
            assertThat(metadata.single().ident).isEqualTo("2")
        }

        @Test
        fun `henter alle som finnes`() {
            val fagsak1 = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("2"))))

            val metadata = fagsakRepository.hentFagsakMetadata(fagsakIder = setOf(fagsak1.id, fagsak2.id))
            assertThat(metadata).hasSize(2)
            assertThat(metadata.map { it.id }).containsExactlyInAnyOrder(fagsak1.id, fagsak2.id)
        }

        @Test
        fun `fagsak finnes ikke`() {
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1"))))

            val metadata = fagsakRepository.hentFagsakMetadata(fagsakIder = setOf(FagsakId.random()))
            assertThat(metadata).isEmpty()
        }
    }

    private fun opprettFagsakMedFlereIdenter(
        ident: String = "1",
        ident2: String = "2",
        ident3: String = "3",
    ): Fagsak {
        val endret2DagerSiden = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2)))
        return fagsak(
            setOf(
                PersonIdent(ident = ident),
                PersonIdent(ident = ident2, sporbar = endret2DagerSiden),
                PersonIdent(ident = ident3),
            ),
        )
    }
}
