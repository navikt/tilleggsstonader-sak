package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
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

        val grunnlagJson = VilkårperioderGrunnlag(
            aktivitet = GrunnlagAktivitet(
                aktiviteter = listOf(
                    AktivitetArenaDto(
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
                tidspunktHentet = LocalDateTime.now(),
            ),
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
}
