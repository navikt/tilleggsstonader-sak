package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime

internal class VilkårperioderGrunnlagRepositoryTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository

    @Test
    internal fun `skal kunne lagre grunnlag for vilkårsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val grunnlagJson =
            VilkårperioderGrunnlag(
                aktivitet = grunnlagAktivitet(),
                ytelse = grunnlagYtelse(),
                hentetInformasjon = hentetInformasjon(),
            )

        vilkårperioderGrunnlagRepository.insert(
            VilkårperioderGrunnlagDomain(
                behandlingId = behandling.id,
                grunnlag = grunnlagJson,
            ),
        )

        val lagretGrunnlag = vilkårperioderGrunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(lagretGrunnlag.behandlingId).isEqualTo(behandling.id)
        assertThat(lagretGrunnlag.grunnlag).isEqualTo(grunnlagJson)
    }

    @Test
    internal fun `skal håndtere at stønadstype for enslig forsørger periode ikke er lagret`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val grunnlagJson =
            VilkårperioderGrunnlag(
                aktivitet = grunnlagAktivitet(),
                ytelse =
                    grunnlagYtelseOk(
                        perioder =
                            listOf(
                                PeriodeGrunnlagYtelse.EnsligForsørger(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(1),
                                    erNyttRegelverk2026 = false,
                                ),
                            ),
                    ),
                hentetInformasjon = hentetInformasjon(),
            )

        vilkårperioderGrunnlagRepository.insert(
            VilkårperioderGrunnlagDomain(
                behandlingId = behandling.id,
                grunnlag = grunnlagJson,
            ),
        )

        val lagretGrunnlag = vilkårperioderGrunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(lagretGrunnlag.behandlingId).isEqualTo(behandling.id)
        assertThat(
            lagretGrunnlag.grunnlag.ytelse.perioder
                .first()
                .let { it as? PeriodeGrunnlagYtelse.EnsligForsørger }
                ?.subtype,
        ).isNull()
    }

    private fun grunnlagYtelse() =
        grunnlagYtelseOk(
            perioder =
                listOf(
                    PeriodeGrunnlagYtelse.AAP(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1),
                    ),
                ),
        )

    private fun grunnlagAktivitet() =
        GrunnlagAktivitet(
            aktiviteter =
                listOf(
                    periodeGrunnlagAktivitet(
                        id = "123",
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusMonths(1),
                        type = "TYPE",
                        typeNavn = "Type navn",
                        status = StatusAktivitet.AKTUELL,
                        statusArena = "AKTUL",
                        antallDagerPerUke = 5,
                        prosentDeltakelse = 100.toBigDecimal(),
                        erStønadsberettiget = true,
                        erUtdanning = false,
                        arrangør = "Arrangør",
                        kilde = Kilde.ARENA,
                    ),
                ),
        )

    @Test
    fun `PeriodeGrunnlagYtelse lagres kun med relevante felt - ingen fremmede nullfelt`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
        val perioder =
            listOf(
                PeriodeGrunnlagYtelse.AAP(fom = 1 januar 2025, tom = 31 januar 2025),
                PeriodeGrunnlagYtelse.Dagpenger(fom = 1 januar 2025, tom = 31 januar 2025),
                PeriodeGrunnlagYtelse.EnsligForsørger(fom = 1 januar 2025, tom = 31 januar 2025, erNyttRegelverk2026 = false),
                PeriodeGrunnlagYtelse.Omstillingsstønad(fom = 1 januar 2025, tom = 31 januar 2025),
                PeriodeGrunnlagYtelse.TiltakspengerTPSak(fom = 1 januar 2025, tom = 31 januar 2025),
                PeriodeGrunnlagYtelse.TiltakspengerArena(fom = 1 januar 2025, tom = 31 januar 2025),
            )
        vilkårperioderGrunnlagRepository.insert(
            VilkårperioderGrunnlagDomain(
                behandlingId = behandling.id,
                grunnlag =
                    VilkårperioderGrunnlag(
                        aktivitet = grunnlagAktivitet(),
                        ytelse = grunnlagYtelseOk(perioder = perioder),
                        hentetInformasjon = hentetInformasjon(),
                    ),
            ),
        )

        val periodeJson = (0..5).map { hentPeriodeJson(behandling.id, it).feltNavn() }
        val aap = periodeJson[0]
        val dagpenger = periodeJson[1]
        val ensligForsørger = periodeJson[2]
        val omstillingsstønad = periodeJson[3]
        val tiltakspengerTPSak = periodeJson[4]
        val tiltakspengerArena = periodeJson[5]

        assertThat(aap).doesNotContain("gjenståendeDagerFraTelleverk", "erNyttRegelverk2026")
        assertThat(dagpenger).doesNotContain("subtype", "erNyttRegelverk2026")
        assertThat(ensligForsørger).doesNotContain("gjenståendeDagerFraTelleverk")
        assertThat(omstillingsstønad).doesNotContain("subtype", "gjenståendeDagerFraTelleverk", "erNyttRegelverk2026")
        assertThat(tiltakspengerTPSak).doesNotContain("subtype", "gjenståendeDagerFraTelleverk", "erNyttRegelverk2026")
        assertThat(tiltakspengerArena).doesNotContain("subtype", "gjenståendeDagerFraTelleverk", "erNyttRegelverk2026")
    }

    private fun hentPeriodeJson(
        behandlingId: BehandlingId,
        index: Int = 0,
    ): JsonNode {
        val json =
            jdbcTemplate.queryForObject(
                "SELECT grunnlag::text FROM vilkarperioder_grunnlag WHERE behandling_id = :id",
                mapOf("id" to behandlingId.id),
                String::class.java,
            )!!
        return jsonMapper.readTree(json)["ytelse"]["perioder"][index]
    }

    private fun JsonNode.feltNavn(): Set<String> = propertyNames().toSet()

    private fun hentetInformasjon() =
        HentetInformasjon(
            fom = LocalDate.now().minusMonths(3),
            tom = LocalDate.now().plusYears(1),
            tidspunktHentet = LocalDateTime.now(),
        )
}
