package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførInngangsvilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførVilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningRevurderingServiceTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `forlengelse av en reise skal ikke påvirke tidligere perioder`() {
        val reiseFom = 1 januar 2025
        val reiseOpprinneligTom = 16 februar 2025
        val reiseForlengetTom = 30 mars 2025

        val fagsakId = gjennomførEnFørstegangsbehandling(reiseFom, reiseOpprinneligTom)

        endreAlleBeløpTilNoeHeltTulleteStort()

        val revurderingId = opprettRevurderingDagligReise(fagsakId)

        forlengReiseperioden(revurderingId, reiseFom, reiseForlengetTom)

        gjennomførBeregningSteg(revurderingId, Stønadstype.DAGLIG_REISE_TSO)

        with(hentBeregningsresultat(revurderingId)) {
            assertThat(size).isEqualTo(3)

            // Forventer at første andel, som er langt unna tidligste endring-datoen, ikke har blitt reberegnet
            assertThat(first().fom).isEqualTo(1 januar 2025)
            assertThat(first().beløp).isEqualTo(999999999)

            assertThat(get(1).fom).isEqualTo(31 januar 2025)
            assertThat(get(1).beløp).isEqualTo(800)

            assertThat(last().fom).isEqualTo(2 mars 2025)
            assertThat(last().beløp).isEqualTo(800)
        }
    }

    private fun hentBeregningsresultat(revurderingId: BehandlingId): List<BeregningsresultatForPeriodeDto> =
        kall.vedtak
            .hentVedtak(
                Stønadstype.DAGLIG_REISE_TSO,
                revurderingId,
            ).expectOkWithBody<InnvilgelseDagligReiseResponse>()
            .beregningsresultat.offentligTransport!!
            .reiser
            .single()
            .perioder

    private fun gjennomførEnFørstegangsbehandling(
        reiseFom: LocalDate,
        reiseOpprinneligTom: LocalDate,
    ): FagsakId {
        val førstegangsbehandlingId =
            gjennomførBehandlingsløp(
                medAktivitet = ::lagreAktivitet,
                medMålgruppe = ::lagreMålgruppe,
                medVilkår = listOf(lagreDagligReiseDto(fom = reiseFom, tom = reiseOpprinneligTom)),
            )

        val førstegangsbehandling = kall.behandling.hent(førstegangsbehandlingId)
        return førstegangsbehandling.fagsakId
    }

    private fun opprettRevurderingDagligReise(fagsakId: FagsakId): BehandlingId {
        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 15 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )
        gjennomførInngangsvilkårSteg(behandlingId = revurderingId)
        return revurderingId
    }

    private fun lagreAktivitet(behandlingId: BehandlingId): LagreVilkårperiode =
        lagreVilkårperiodeAktivitet(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)

    private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
        lagreVilkårperiodeMålgruppe(behandlingId, fom = 1 januar 2025, tom = 30 mars 2025)

    private fun endreAlleBeløpTilNoeHeltTulleteStort() {
        jdbcTemplate.update(
            """
            UPDATE vedtak
            SET data = replace(data::text, '"beløp": 800', '"beløp": 999999999')::jsonb            
            """.trimIndent(),
            emptyMap<String, Any>(),
        )
    }

    private fun OffentligTransportBeregningRevurderingServiceTest.forlengReiseperioden(
        revurderingId: BehandlingId,
        fra: LocalDate,
        til: LocalDate,
    ) {
        val vilkår = kall.vilkårDagligReise.hentVilkår(revurderingId).single()
        kall.vilkårDagligReise.oppdaterVilkår(
            lagreVilkår = lagreDagligReiseDto(fom = fra, tom = til),
            vilkårId = vilkår.id,
            behandlingId = revurderingId,
        )

        gjennomførVilkårSteg(
            medVilkår = emptyList(),
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )
    }
}
