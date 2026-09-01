package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.InnvilgelseReiseOppstartAvslutningHjemreiseResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

// TODO: Utvid testen etter hvert som søknadsmapping og beregning for
//  STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO blir implementert.
class ReiseOppstartAvslutningHjemreiseTsoIntegrationTest : CleanDatabaseIntegrationTest() {
    val fom = 1 januar 2026
    val tom = 31 januar 2026
    val ident = "12345678910"

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `skal kunne lagre målgruppe, aktivitet og stønadsvilkår for reise oppstart, avslutning og hjemreise TSO`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)

        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.BEREGNE_YTELSE) {
            defaultReiseTilOppstartAvslutningOgHjemreiserTSOTestdata(fom, tom)
        }

        val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(vilkårperioder.målgrupper).hasSize(1)
        assertThat(vilkårperioder.aktiviteter).hasSize(1)

        val målgruppe = vilkårperioder.målgrupper.single()
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)

        val aktivitet = vilkårperioder.aktiviteter.single()
        assertThat(aktivitet.type).isEqualTo(AktivitetType.TILTAK)

        val vilkår = kall.vilkårReiseOppstartAvslutningHjemreise.hentVilkår(behandling.id)
        assertThat(vilkår).hasSize(1)

        val aktivitetMedReiser = vilkår.single()
        assertThat(aktivitetMedReiser.aktivitetId).isEqualTo(aktivitet.globalId)
        assertThat(aktivitetMedReiser.aktivitetType).isEqualTo(AktivitetType.TILTAK)
        assertThat(aktivitetMedReiser.reiser).hasSize(1)

        val reiseVilkår = aktivitetMedReiser.reiser.single()
        assertThat(reiseVilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(reiseVilkår.typeReiseformål).isEqualTo(TypeReiseformål.OPPSTART)
        assertThat(reiseVilkår.fakta).isInstanceOf(FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto::class.java)
        assertThat((reiseVilkår.fakta as FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto).aktivitetId)
            .isEqualTo(aktivitet.globalId)
    }

    @Test
    fun `skal kunne beregne ytelse og opprette andel tilkjent ytelse for reise oppstart TSO`() {
        val reisedato = 1 januar 2026 // torsdag

        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            )

        opprettOgTilordneOppgaveForBehandling(behandling.id)

        gjennomførBehandlingsløp(ident = ident, behandlingId = behandling.id, tilSteg = StegType.SIMULERING) {
            defaultReiseTilOppstartAvslutningOgHjemreiserTSOTestdata(reisedato, reisedato)
        }

        val vedtak =
            kall.vedtak.apiRespons
                .hentVedtak(Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO, behandling.id)
                .expectOkWithBody<InnvilgelseReiseOppstartAvslutningHjemreiseResponse>()

        val offentligTransport = vedtak.beregningsresultat.offentligTransport
        assertThat(offentligTransport).hasSize(1)
        assertThat(offentligTransport!!.single().beløp).isEqualByComparingTo(BigDecimal(40))
        assertThat(offentligTransport.single().fom).isEqualTo(reisedato)
        assertThat(offentligTransport.single().tom).isEqualTo(reisedato)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)
        assertThat(tilkjentYtelse).isNotNull
        assertThat(tilkjentYtelse!!.andelerTilkjentYtelse).hasSize(1)

        val andel = tilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.type).isEqualTo(TypeAndel.REISE_OPPSTART_AAP)
        assertThat(andel.satstype).isEqualTo(Satstype.DAG)
        assertThat(andel.fom).isEqualTo(reisedato)
        assertThat(andel.tom).isEqualTo(reisedato)
        assertThat(andel.beløp).isEqualTo(40)
    }
}
