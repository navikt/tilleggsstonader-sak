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
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.StegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.SøknadRoutingKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.TotrinnskontrollKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VedtakKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårDagligReiseKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårKall
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.VilkårperiodeKall

class Kall(
    test: IntegrationTest,
) {
    val arena = ArenaKall(test)
    val behandling = BehandlingKall(test)
    val brev = BrevKall(test)
    val gjenopprettOppgave = GjenopprettOppgaveKall(test)
    val journalpost = JournalpostKall(test)
    val person = PersonKall(test)
    val satsjustering = SatsjusteringKall(test)
    val settPaVent = SettPåVentKall(test)
    val simulering = SimuleringKall(test)
    val steg = StegKall(test)
    val søknadRouting = SøknadRoutingKall(test)
    val totrinnskontroll = TotrinnskontrollKall(test)
    val vedtak = VedtakKall(test)
    val vilkår = VilkårKall(test)
    val vilkårDagligReise = VilkårDagligReiseKall(test)
    val vilkårperiode = VilkårperiodeKall(test)
}
