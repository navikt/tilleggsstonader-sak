package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

internal class VilkårperioderGrunnlagRepositoryTest : IntegrationTest() {
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
                                PeriodeGrunnlagYtelse(
                                    type = TypeYtelsePeriode.ENSLIG_FORSØRGER,
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(1),
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
                .subtype,
        ).isNull()
    }

    private fun grunnlagYtelse() =
        grunnlagYtelseOk(
            perioder =
                listOf(
                    PeriodeGrunnlagYtelse(
                        type = TypeYtelsePeriode.AAP,
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

    private fun hentetInformasjon() =
        HentetInformasjon(
            fom = LocalDate.now().minusMonths(3),
            tom = LocalDate.now().plusYears(1),
            tidspunktHentet = LocalDateTime.now(),
        )
}
