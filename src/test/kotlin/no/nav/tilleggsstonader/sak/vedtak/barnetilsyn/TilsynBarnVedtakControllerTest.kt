package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.kall.avslåVedtakTilsynBarn
import no.nav.tilleggsstonader.sak.kall.hentVedtakTilsynBarn
import no.nav.tilleggsstonader.sak.kall.hentVedtakTilsynBarnKall
import no.nav.tilleggsstonader.sak.kall.innvilgeVedtakTilsynBarn
import no.nav.tilleggsstonader.sak.kall.opphørVedtakTilsynBarn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class TilsynBarnVedtakControllerTest : IntegrationTest() {
    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val vedtaksperiodeDto =
        VedtaksperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 31),
            aktivitetType = AktivitetType.TILTAK,
            målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        )
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val målgruppe = målgruppe(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val vilkår =
        vilkår(
            behandlingId = behandling.id,
            barnId = barn.id,
            type = VilkårType.PASS_BARN,
            resultat = Vilkårsresultat.OPPFYLT,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 31),
            utgift = 100,
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        barnRepository.insert(barn)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(vilkår)
    }

    @Test
    fun `skal returnere empty body når det ikke finnes noe lagret`() {
        hentVedtakTilsynBarnKall(behandling.id)
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty
    }

    @Test
    fun `Skal lagre og hente innvilgelse med vedtaksperioder og begrunnelse`() {
        val vedtak = innvilgelseDto(listOf(vedtaksperiodeDto), "Jo du skjønner det, at...")
        innvilgeVedtakTilsynBarn(behandling.id, vedtak)

        val lagretDto = hentVedtakTilsynBarn<InnvilgelseTilsynBarnResponse>(behandling.id)

        assertThat(lagretDto.vedtaksperioder?.map { it.tilVedtaksperiodeDto() }).isEqualTo(vedtak.vedtaksperioder)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.INNVILGELSE)
    }

    @Test
    fun `skal lagre og hente avslag`() {
        val vedtak =
            AvslagTilsynBarnDto(
                årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                begrunnelse = "begrunnelse",
            )

        avslåVedtakTilsynBarn(behandling.id, vedtak)

        val lagretDto = hentVedtakTilsynBarn<AvslagTilsynBarnDto>(behandling.id)

        assertThat((lagretDto).årsakerAvslag).isEqualTo(vedtak.årsakerAvslag)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
    }

    @Test
    fun `skal lagre og hente opphør med vedtaksperioder`() {
        innvilgeVedtakTilsynBarn(
            behandling.id,
            InnvilgelseTilsynBarnRequest(
                listOf(vedtaksperiodeDto),
                begrunnelse = "Jo du skjønner det, at...",
            ),
        )
        testoppsettService.ferdigstillBehandling(behandling)
        val opphørsdato = LocalDate.of(2023, 1, 15)
        val behandlingLagreOpphør =
            testoppsettService.opprettRevurdering(
                forrigeBehandling = behandling,
                fagsak = fagsak,
            )

        vilkårRepository.insert(
            vilkår.copy(
                id = VilkårId.random(),
                behandlingId = behandlingLagreOpphør.id,
                status = VilkårStatus.UENDRET,
            ),
        )
        vilkårperiodeRepository.insert(
            aktivitet.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingLagreOpphør.id,
                status = Vilkårstatus.UENDRET,
            ),
        )
        vilkårperiodeRepository.insert(
            målgruppe.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingLagreOpphør.id,
                status = Vilkårstatus.UENDRET,
            ),
        )

        val vedtak =
            OpphørTilsynBarnRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "endre utgifter opphør",
                opphørsdato = opphørsdato,
            )

        opphørVedtakTilsynBarn(behandlingLagreOpphør.id, vedtak)

        val lagretDto = hentVedtakTilsynBarn<OpphørTilsynBarnResponse>(behandlingLagreOpphør.id)

        assertThat(lagretDto.årsakerOpphør).isEqualTo(vedtak.årsakerOpphør)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.OPPHØR)
    }
}
