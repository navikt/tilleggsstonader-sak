package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ReiseOppstartAvslutningHjemreiseVilkårServiceTest {
    private val vilkårRepository = mockk<VilkårRepository>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val vilkårService = mockk<VilkårService>(relaxed = true)
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val unleashService = mockk<UnleashService>(relaxed = true)

    private val service =
        ReiseOppstartAvslutningHjemreiseVilkårService(
            vilkårRepository = vilkårRepository,
            behandlingService = behandlingService,
            vilkårService = vilkårService,
            vilkårperiodeService = vilkårperiodeService,
            unleashService = unleashService,
        )

    @Test
    fun `skal feile når reise ikke kan knyttes til aktivitet i hentVilkår`() {
        val behandlingId = BehandlingId.random()
        val aktivitetIdPåReise = VilkårperiodeGlobalId.random()
        val aktivitetIdIPerioder = VilkårperiodeGlobalId.random()
        val aktivitet = aktivitet(aktivitetIdIPerioder)

        every { vilkårRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                vilkår(
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseUbestemt(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksveien 1",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            aktivitetId = aktivitetIdPåReise,
                        ),
                ),
            )
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
            Vilkårperioder(målgrupper = emptyList(), aktiviteter = listOf(aktivitet))

        assertThatThrownBy { service.hentVilkårGruppertPåAktivitet(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("ikke kan knyttes til aktivitet")
    }

    @Test
    fun `skal inkludere reise med ubestemt fakta når aktivitet matcher`() {
        val behandlingId = BehandlingId.random()
        val aktivitetId = VilkårperiodeGlobalId.random()
        val aktivitet = aktivitet(aktivitetId)

        every { vilkårRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                vilkår(
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseUbestemt(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksveien 1",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            aktivitetId = aktivitetId,
                        ),
                ),
            )
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
            Vilkårperioder(målgrupper = emptyList(), aktiviteter = listOf(aktivitet))

        val hentetVilkår = service.hentVilkårGruppertPåAktivitet(behandlingId)

        assertThat(hentetVilkår).hasSize(1)
        assertThat(hentetVilkår.single().reiser).hasSize(1)
        assertThat(
            hentetVilkår
                .single()
                .reiser
                .single()
                .fakta,
        ).isInstanceOf(FaktaUbestemtType::class.java)
    }

    @Test
    fun `skal feile når ubestemt fakta mangler aktivitetId`() {
        val behandlingId = BehandlingId.random()

        every { vilkårRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                vilkår(
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseUbestemt(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksveien 1",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            aktivitetId = null,
                        ),
                ),
            )

        assertThatThrownBy { service.hentVilkårForBehandling(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("ubestemt fakta uten aktivitetId")
    }

    private fun aktivitet(aktivitetId: VilkårperiodeGlobalId): VilkårperiodeAktivitet {
        val aktivitet = mockk<VilkårperiodeAktivitet>()
        every { aktivitet.globalId } returns aktivitetId
        every { aktivitet.type } returns AktivitetType.TILTAK
        every { aktivitet.tiltaksvariant } returns null
        every { aktivitet.fom } returns 1.januar(2025)
        every { aktivitet.tom } returns 31.januar(2025)
        return aktivitet
    }
}
