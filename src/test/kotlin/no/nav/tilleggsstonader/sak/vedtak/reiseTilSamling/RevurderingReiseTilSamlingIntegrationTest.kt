package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingPrivatBilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RevurderingReiseTilSamlingIntegrationTest(
    @Autowired private val vedtakRepository: VedtakRepository,
) : IntegrationTest() {
    private fun hentBeregningsresultat(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseReiseTilSamling>()
            .data.beregningsresultat

    private fun hentBeregningsplan(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseReiseTilSamling>()
            .data.beregningsplan

    @Test
    fun `ny reise legges til i tillegg til uendret reise - ny reberegnes, gammel gjenbrukes`() {
        val reiseA = ReiseId.random()
        val reiseB = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseA)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(28 februar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(28 februar 2025)
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reiseB)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport).hasSize(2)

        val gammelReise = resultat.offentligTransport.single { it.reiseId == reiseA }
        val nyReise = resultat.offentligTransport.single { it.reiseId == reiseB }

        assertThat(gammelReise.fraTidligereVedtak).isTrue()
        assertThat(nyReise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `ny reise legges til i tillegg til uendret privatbil-reise - ny reberegnes, gammel gjenbrukes`() {
        val reiseA = ReiseId.random()
        val reiseB = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseA)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(28 februar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(28 februar 2025)
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reiseB)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.privatBil).hasSize(2)

        val gammelReise = resultat.privatBil.single { it.reiseId == reiseA }
        val nyReise = resultat.privatBil.single { it.reiseId == reiseB }

        assertThat(gammelReise.fraTidligereVedtak).isTrue()
        assertThat(nyReise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `endret beløp på eksisterende offentlig transport-reise reberegnes med nytt beløp`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(fom = 1 januar 2025, tom = 31 januar 2025)
            }

        val opprinneligBeløp =
            hentBeregningsresultat(førstegangsbehandling.behandlingId)
                .offentligTransport
                .single()
                .beløp

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                vilkår {
                    endreReiseTilSamling {
                        copy(
                            fakta =
                                (fakta as FaktaReiseTilSamlingOffentligTransportDto)
                                    .copy(utgifterOffentligTransport = 999.toBigDecimal()),
                        )
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport).hasSize(1)
        val reise = resultat.offentligTransport.single()
        assertThat(reise.beløp).isEqualTo(999.toBigDecimal())
        assertThat(reise.beløp).isNotEqualTo(opprinneligBeløp)
        assertThat(reise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `endret reiseavstand på eksisterende privatbil-reise reberegnes med nytt beløp`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(1 januar 2025, 31 januar 2025, reiseavstand = 40.toBigDecimal())
                    }
                }
            }

        val opprinneligBeløp =
            hentBeregningsresultat(førstegangsbehandling.behandlingId)
                .privatBil
                .single()
                .beløp

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                vilkår {
                    endreReiseTilSamling {
                        copy(
                            fakta =
                                (fakta as FaktaReiseTilSamlingPrivatBilDto)
                                    .copy(reiseavstand = 80.toBigDecimal()),
                        )
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.privatBil).hasSize(1)
        val reise = resultat.privatBil.single()
        assertThat(reise.beløp).isNotEqualTo(opprinneligBeløp)
        assertThat(reise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `ny sluttdato på eksisterende reise med uendret beløp gir samme utbetaling som før`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(fom = 1 januar 2025, tom = 31 januar 2025)
            }

        val opprinneligBeløp =
            hentBeregningsresultat(førstegangsbehandling.behandlingId)
                .offentligTransport
                .single()
                .beløp

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(15 januar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(15 januar 2025)
                }
                vilkår {
                    endreReiseTilSamling {
                        copy(tom = 15 januar 2025)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        val reise = resultat.offentligTransport.single()
        assertThat(reise.beløp).isEqualTo(opprinneligBeløp)
    }

    @Test
    fun `forkortet reise reberegnes selv om ny sluttdato er før beregnFra, slik at gammel sluttdato ikke henger igjen`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(fom = 1 januar 2025, tom = 31 januar 2025)
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        // Reisen forkortes fra 31 januar til 15 januar. beregnFra blir dagen etter ny sluttdato (16 januar),
        // som gjør at den forkortede reisen sin nye tom (15 januar) er før beregnFra. Uten fiksen for
        // "forkortet reise kan beholde foreldet sluttdato ved gjenbruk" ville reisen da blitt feilaktig
        // gjenbrukt fra forrige vedtak med den gamle, lengre sluttdatoen (31 januar).
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(15 januar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(15 januar 2025)
                }
                vilkår {
                    endreReiseTilSamling {
                        copy(tom = 15 januar 2025)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        val reise = resultat.offentligTransport.single()

        assertThat(reise.fraTidligereVedtak).isFalse()
        assertThat(reise.grunnlag.tom).isEqualTo(15 januar 2025)
    }

    @Test
    fun `ny startdato på eksisterende reise med uendret beløp gir samme utbetaling som før`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(fom = 1 januar 2025, tom = 31 januar 2025)
            }

        val opprinneligBeløp =
            hentBeregningsresultat(førstegangsbehandling.behandlingId)
                .offentligTransport
                .single()
                .beløp

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdater { perioder, behandlingId ->
                        val periode = perioder.single()
                        periode.id to periode.tilLagreVilkårperiodeAktivitet(behandlingId).copy(fom = 15 januar 2025)
                    }
                }
                målgruppe {
                    oppdater { perioder, behandlingId ->
                        val periode = perioder.single()
                        periode.id to periode.tilLagreVilkårperiodeMålgruppe(behandlingId).copy(fom = 15 januar 2025)
                    }
                }
                vilkår {
                    endreReiseTilSamling {
                        copy(fom = 15 januar 2025)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        val reise = resultat.offentligTransport.single()
        assertThat(reise.beløp).isEqualTo(opprinneligBeløp)
    }

    @Test
    fun `to overlappende reiser - endring på en av dem gjør at begge reberegnes`() {
        val reiseA = ReiseId.random()
        val reiseB = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(
                            1 januar 2025,
                            31 januar 2025,
                            reiseId = reiseA,
                            utgifterOffentligTransport = 300.toBigDecimal(),
                        )
                        offentligTransportReiseTilSamling(
                            1 januar 2025,
                            31 januar 2025,
                            reiseId = reiseB,
                            utgifterOffentligTransport = 100.toBigDecimal(),
                        )
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                vilkår {
                    endreReiseTilSamling(reiseId = reiseA) {
                        copy(
                            fakta =
                                (fakta as FaktaReiseTilSamlingOffentligTransportDto)
                                    .copy(utgifterOffentligTransport = 555.toBigDecimal()),
                        )
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport).hasSize(2)

        val endretReise = resultat.offentligTransport.single { it.reiseId == reiseA }
        val andreReise = resultat.offentligTransport.single { it.reiseId == reiseB }

        assertThat(endretReise.beløp).isEqualTo(555.toBigDecimal())
        assertThat(endretReise.fraTidligereVedtak).isFalse()
        assertThat(andreReise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `saksbehandler sletter en eksisterende reise - ingen post for slettet reise i resultatet`() {
        val reiseA = ReiseId.random()
        val reiseB = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 28 februar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 28 februar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseA)
                        offentligTransportReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reiseB)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(31 januar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(31 januar 2025)
                }
                vilkår {
                    fjernReiseTilSamling(reiseId = reiseB)
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport).hasSize(1)
        assertThat(resultat.offentligTransport.map { it.reiseId }).containsOnly(reiseA)
        assertThat(resultat.offentligTransport.none { it.reiseId == reiseB }).isTrue()
        assertThat(resultat.privatBil).isEmpty()
    }

    @Test
    fun `revurdering uten endringer gjenbruker forrige resultat 1 til 1`() {
        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                defaultReiseTilSamlingTSOTestdata(fom = 1 januar 2025, tom = 31 januar 2025)
            }

        val forrigeResultat = hentBeregningsresultat(førstegangsbehandling.behandlingId)

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {}

        val beregningsplan = hentBeregningsplan(revurderingId)
        assertThat(beregningsplan.omfang).isEqualTo(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT)

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport.map { it.reiseId to it.beløp })
            .isEqualTo(forrigeResultat.offentligTransport.map { it.reiseId to it.beløp })
        assertThat(resultat.offentligTransport.all { it.fraTidligereVedtak }).isTrue()
        assertThat(resultat.privatBil.all { it.fraTidligereVedtak }).isTrue()
    }

    @Test
    fun `ny privatbil-reise lagt til i tillegg til uendret offentlig transport-reise`() {
        val reiseOffentligTransport = ReiseId.random()
        val reisePrivatBil = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseOffentligTransport)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdaterTomPåEnesteAktivitet(28 februar 2025)
                }
                målgruppe {
                    oppdaterTomPåEnesteMålgruppe(28 februar 2025)
                }
                vilkår {
                    opprett {
                        privatBilReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reisePrivatBil)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport).hasSize(1)
        assertThat(resultat.privatBil).hasSize(1)

        val gammelReise = resultat.offentligTransport.single { it.reiseId == reiseOffentligTransport }
        val nyReise = resultat.privatBil.single { it.reiseId == reisePrivatBil }

        assertThat(gammelReise.fraTidligereVedtak).isTrue()
        assertThat(nyReise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `slettet reise og ny reise i samme revurdering - slettet reise borte, ny reise reberegnet`() {
        val reiseA = ReiseId.random()
        val reiseB = ReiseId.random()
        val reiseC = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 28 februar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 28 februar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseA)
                        offentligTransportReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reiseB)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                vilkår {
                    fjernReiseTilSamling(reiseId = reiseB)
                    opprett {
                        offentligTransportReiseTilSamling(1 februar 2025, 28 februar 2025, reiseId = reiseC)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        assertThat(resultat.offentligTransport.map { it.reiseId }).containsExactlyInAnyOrder(reiseA, reiseC)

        val gammelReise = resultat.offentligTransport.single { it.reiseId == reiseA }
        val nyReise = resultat.offentligTransport.single { it.reiseId == reiseC }

        assertThat(gammelReise.fraTidligereVedtak).isTrue()
        assertThat(nyReise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `endring i aktivitetens startdato trigger reberegning av reise som overlapper, selv om reisevilkåret er uendret`() {
        val reiseId = ReiseId.random()

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(1 januar 2025, 31 januar 2025)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(1 januar 2025, 31 januar 2025)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(1 januar 2025, 31 januar 2025, reiseId = reiseId)
                    }
                }
            }

        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandling.behandlingId,
            ) {
                aktivitet {
                    oppdater { perioder, behandlingId ->
                        val periode = perioder.single()
                        periode.id to periode.tilLagreVilkårperiodeAktivitet(behandlingId).copy(fom = 25 desember 2024)
                    }
                }
            }

        val resultat = hentBeregningsresultat(revurderingId)
        val reise = resultat.offentligTransport.single()

        // Selve reisevilkåret er identisk, men fordi aktiviteten (som reisen overlapper med) endret seg,
        // skal reisen reberegnes fra bunnen og ikke gjenbrukes fra forrige vedtak.
        assertThat(reise.fraTidligereVedtak).isFalse()
    }
}
