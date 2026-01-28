package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførInngangsvilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførVilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningRevurderingServiceTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `ved forlengelse av en reise skal vi bare reberegne perioder som påvirkes av revurderingen`() {
        val førsteJanuar = 1 januar 2025
        val førsteJanuarPlussEnTrettidagersperiode = førsteJanuar.plusDays(30)
        val førsteJanuarPlussToTrettidagersperioder = førsteJanuar.plusDays(60)

        val fagsakId =
            gjennomførEnFørstegangsbehandling(
                reiseFom = førsteJanuar,
                reiseTom = førsteJanuarPlussToTrettidagersperioder,
            )

        endreAlleBeløpTilNoeHeltTulleteStort()

        val revurderingId = opprettRevurderingDagligReise(fagsakId)

        val førsteJanuarPlussTreTrettidagersperioder = førsteJanuar.plusDays(89)
        forlengReiseperioden(
            revurderingId = revurderingId,
            fra = førsteJanuar,
            til = førsteJanuarPlussTreTrettidagersperioder,
        )

        gjennomførBeregningSteg(revurderingId, Stønadstype.DAGLIG_REISE_TSO)

        with(hentBeregnedeReiser(revurderingId).single().perioder) {
            assertThat(size).isEqualTo(3)

            // Forventer at første andel, som er langt unna tidligste endring-datoen, ikke blir reberegnet
            assertThat(first().fom).isEqualTo(1 januar 2025)
            assertThat(first().beløp).isEqualTo(999999999)

            // Forventer at andre andel, ikke har endret grunnlaget sitt, ikke blir reberegnet
            assertThat(get(1).fom).isEqualTo(`førsteJanuarPlussEnTrettidagersperiode`)
            assertThat(get(1).beløp).isEqualTo(999999999)

            // Forventer at tredje andel, som er helt ny i revurderingen, blir reberegnet
            assertThat(last().fom).isEqualTo(førsteJanuarPlussToTrettidagersperioder)
            assertThat(last().beløp).isEqualTo(800)
        }
    }

    @Test
    fun `ved forlengelse av en reise skal vi reberegne perioder nær tidligste endring-datoen gitt at de har endret grunnlaget sitt`() {
        val reiseFom = 1 januar 2025
        val reiseOpprinneligTom = 16 februar 2025
        val reiseForlengetTom = 30 mars 2025

        val fagsakId = gjennomførEnFørstegangsbehandling(reiseFom, reiseOpprinneligTom)

        endreAlleBeløpTilNoeHeltTulleteStort()

        val revurderingId = opprettRevurderingDagligReise(fagsakId)

        forlengReiseperioden(revurderingId, reiseFom, reiseForlengetTom)

        gjennomførBeregningSteg(revurderingId, Stønadstype.DAGLIG_REISE_TSO)

        with(hentBeregnedeReiser(revurderingId).single().perioder) {
            assertThat(size).isEqualTo(3)

            // Forventer at første andel, som er langt unna tidligste endring-datoen, ikke blir reberegnet
            assertThat(first().fom).isEqualTo(1 januar 2025)
            assertThat(first().beløp).isEqualTo(999999999)

            // Forventer at andre andel, som har endret tom-dato, blir reberegnet
            assertThat(get(1).fom).isEqualTo(31 januar 2025)
            assertThat(get(1).beløp).isEqualTo(800)

            // Forventer at tredje andel, som er helt ny i revurderingen, blir reberegnet
            assertThat(last().fom).isEqualTo(2 mars 2025)
            assertThat(last().beløp).isEqualTo(800)
        }
    }

    @Test
    fun `hvis en ny helt reise legges til i revurdering, skal andre reiser i førstegangsvedtaket ikke reberegnes`() {
        // Reise i førstegangsvedtaket
        val førsteJanuar = 1 januar 2025
        val truefemteJanuar = 25 januar 2025

        val fagsakId = gjennomførEnFørstegangsbehandling(førsteJanuar, truefemteJanuar)

        endreAlleBeløpTilNoeHeltTulleteStort()

        val revurderingId = opprettRevurderingDagligReise(fagsakId)

        // Ny reise i revurderingen
        val tjuefjerdeDesember = 24 desember 2024
        val femteJanuar = 5 januar 2025

        leggTilNyReise(tjuefjerdeDesember, femteJanuar, revurderingId)

        gjennomførBeregningSteg(revurderingId, Stønadstype.DAGLIG_REISE_TSO)

        val beregnedeReiser = hentBeregnedeReiser(revurderingId)

        // Forventer at første reise (den nye i revurderingen) består av én andel som har blitt reberegnet
        with(beregnedeReiser.first().perioder.single()) {
            assertThat(fom).isEqualTo(tjuefjerdeDesember)
            assertThat(tom).isEqualTo(femteJanuar)
            assertThat(beløp).isEqualTo(720)
        }

        // Forventer at andre andel (den fra førstegnagsvedtaket) ikke har blitt reberegnet
        with(beregnedeReiser.last().perioder.single()) {
            assertThat(fom).isEqualTo(førsteJanuar)
            assertThat(tom).isEqualTo(truefemteJanuar)
            assertThat(beløp).isEqualTo(999999999)
        }
    }

    private fun leggTilNyReise(
        fom: LocalDate,
        tom: LocalDate,
        revurderingId: BehandlingId,
    ) {
        gjennomførVilkårSteg(
            medVilkår = listOf(lagreDagligReiseDto(fom = fom, tom = tom)),
            behandlingId = revurderingId,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )
    }

    private fun hentBeregnedeReiser(revurderingId: BehandlingId): List<BeregningsresultatForReiseDto> =
        kall.vedtak
            .hentVedtak(
                Stønadstype.DAGLIG_REISE_TSO,
                revurderingId,
            ).expectOkWithBody<InnvilgelseDagligReiseResponse>()
            .beregningsresultat.offentligTransport!!
            .reiser

    private fun gjennomførEnFørstegangsbehandling(
        reiseFom: LocalDate,
        reiseTom: LocalDate,
    ): FagsakId {
        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        add(::lagreAktivitet)
                    }
                }
                målgruppe {
                    opprett {
                        add(::lagreMålgruppe)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(reiseFom, reiseTom)
                    }
                }
            }

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
        lagreVilkårperiodeAktivitet(behandlingId, fom = 1 januar 2024, tom = 30 mars 2026)

    private fun lagreMålgruppe(behandlingId: BehandlingId): LagreVilkårperiode =
        lagreVilkårperiodeMålgruppe(behandlingId, fom = 1 januar 2024, tom = 30 mars 2026)

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
