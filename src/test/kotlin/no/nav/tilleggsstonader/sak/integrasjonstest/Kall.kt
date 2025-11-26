package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.ArenaKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.BehandlingKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.BrevKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.GjenopprettOppgaveKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.JournalpostKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.PersonKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.SatsjusteringKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.SettPåVentKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.SimuleringKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.SkjemaRoutingKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.StegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.TotrinnskontrollKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VedtakKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårDagligReiseKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårperiodeKall

class Kall(
    kontekst: IntegrationTest,
) {
    val testklient = Testklient(kontekst)

    val arena = ArenaKall(testklient)
    val behandling = BehandlingKall(testklient)
    val brev = BrevKall(testklient)
    val gjenopprettOppgave = GjenopprettOppgaveKall(testklient)
    val journalpost = JournalpostKall(testklient)
    val person = PersonKall(testklient)
    val satsjustering = SatsjusteringKall(testklient)
    val settPaVent = SettPåVentKall(testklient)
    val simulering = SimuleringKall(testklient)
    val steg = StegKall(testklient)
    val skjemaRouting = SkjemaRoutingKall(testklient)
    val totrinnskontroll = TotrinnskontrollKall(testklient)
    val vedtak = VedtakKall(testklient)
    val vilkår = VilkårKall(testklient)
    val vilkårDagligReise = VilkårDagligReiseKall(testklient)
    val vilkårperiode = VilkårperiodeKall(testklient)
}
