package no.nav.tilleggsstonader.sak

import io.mockk.every
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandlingsjournalpost
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagretBrev
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagretFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.migrering.routing.SkjemaRouting
import no.nav.tilleggsstonader.sak.oppfølging.Oppfølging
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.TilbakekrevingHendelse
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingLogg
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateOperations

abstract class CleanDatabaseIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @BeforeEach
    fun resetDatabase() {
        listOf(
            FagsakUtbetalingId::class,
            Hendelse::class,
            TaskLogg::class,
            Task::class,
            SkjemaRouting::class,
            BrevmottakerFrittståendeBrev::class,
            FrittståendeBrev::class,
            Oppfølging::class,
            FaktaGrunnlag::class,
            Vedtak::class,
            Simuleringsresultat::class,
            TilkjentYtelse::class,
            Vilkårperiode::class,
            Vilkår::class,
            BehandlingBarn::class,
            SøknadBehandling::class,
            SøknadBarnetilsyn::class,
            SettPåVent::class,
            OppgaveDomain::class,
            Totrinnskontroll::class,
            Vedtaksbrev::class,
            BrevmottakerVedtaksbrev::class,
            MellomlagretFrittståendeBrev::class,
            MellomlagretBrev::class,
            VilkårperioderGrunnlagDomain::class,
            Behandlingshistorikk::class,
            Behandlingsjournalpost::class,
            EksternBehandlingId::class,
            TilbakekrevingHendelse::class,
            Kjøreliste::class,
            Behandling::class,
            EksternFagsakId::class,
            FagsakDomain::class,
            PersonIdent::class,
            FagsakPerson::class,
            IverksettingLogg::class,
        ).forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    @BeforeEach
    fun togglePrivatBil() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns false
    }
}
