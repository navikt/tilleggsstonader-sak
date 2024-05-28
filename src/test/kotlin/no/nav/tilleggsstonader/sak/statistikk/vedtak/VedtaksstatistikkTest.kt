package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.UUID

class VedtaksstatistikkTest : IntegrationTest() {

    @Autowired
    lateinit var vedtakstatistikkRepository: VedtakstatistikkRepository

    final val id: UUID = UUID.randomUUID()

    @Test
    fun `kan skrive vedtaksstatistikk til tabell når alle JSON-objekter er tomme lister`() {
        vedtakstatistikkRepository.insert(vedtaksstatistikk())
    }

    @Test
    fun `kan skrive og lese vedtaksstatistikk med flere aktiviteter`() {
        val flereAktiviteter = AktiviteterDvh.JsonWrapper(
            listOf(
                AktiviteterDvh(type = AktivitetTypeDvh.REELL_ARBEIDSSØKER, resultat = ResultatVilkårperiodeDvh.OPPFYLT),
                AktiviteterDvh(type = AktivitetTypeDvh.UTDANNING, resultat = ResultatVilkårperiodeDvh.IKKE_OPPFYLT),
            ),
        )

        vedtakstatistikkRepository.insert(vedtaksstatistikk().copy(aktiviteter = flereAktiviteter))

        val vedtaksstatistikkFraDb = vedtakstatistikkRepository.findAll().first()

        assertThat(vedtaksstatistikkFraDb.aktiviteter).isEqualTo(flereAktiviteter)
    }

    @Test
    fun `kan skrive og lese vedtaksstatistikk med ikke-tom vilkårsvurdering`() {
        val nøstetVilkårsvurdering = VilkårsvurderingerDvh.JsonWrapper(
            listOf(
                VilkårsvurderingerDvh(
                    resultat = VilkårsresultatDvh.OPPFYLT,
                    vilkår = listOf(
                        DelvilkårDvh(
                            resultat = Vilkårsresultat.OPPFYLT,
                            vurderinger = listOf(RegelId.HAR_FULLFØRT_FJERDEKLASSE, RegelId.UNNTAK_ALDER),
                        ),
                    ),
                ),
            ),
        )

        vedtakstatistikkRepository.insert(vedtaksstatistikk().copy(vilkårsvurderinger = nøstetVilkårsvurdering))

        val vedtaksstatistikkFraDb = vedtakstatistikkRepository.findAll().first()

        assertThat(vedtaksstatistikkFraDb.vilkårsvurderinger).isEqualTo(nøstetVilkårsvurdering)
    }

    @Test
    fun `målgrupper kan hentes ut og blir parset til riktig type`() {
        val målgrupper = MålgrupperDvh.JsonWrapper(
            listOf(
                MålgrupperDvh(type = MålgruppeTypeDvh.DAGPENGER, resultat = ResultatVilkårperiodeDvh.OPPFYLT),
                MålgrupperDvh(type = MålgruppeTypeDvh.AAP, resultat = ResultatVilkårperiodeDvh.IKKE_OPPFYLT),
            ),
        )

        vedtakstatistikkRepository.insert(vedtaksstatistikk().copy(målgrupper = målgrupper))

        val vedtaksstatistikkFraDb = vedtakstatistikkRepository.findAll().first()

        assertThat(vedtaksstatistikkFraDb.målgrupper).isEqualTo(målgrupper)
    }

    @Test
    fun `årsakAvslag kan mappes mellom databaseobjekt og domeneobjekt`() {
        val årsakerAvslag = ÅrsakAvslagDvh.fraDomene(listOf(ÅrsakAvslag.INGEN_AKTIVITET))

        vedtakstatistikkRepository.insert(
            vedtaksstatistikk().copy(
                årsakerAvslag = årsakerAvslag,
            ),
        )

        val vedtaksstatistikkFraDb = vedtakstatistikkRepository.findAll().first()

        assertThat(vedtaksstatistikkFraDb.årsakerAvslag).isEqualTo(årsakerAvslag)
    }

    @Test
    fun `skal sette opprettet_tid til nå`() {
        val tidNå = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        vedtakstatistikkRepository.insert(vedtaksstatistikk())
        val vedtaksstatistikkFraDb = vedtakstatistikkRepository.findAll().first()

        assertThat(vedtaksstatistikkFraDb.opprettetTid).isStrictlyBetween(tidNå, tidNå.plusSeconds(1))
    }

    private fun vedtaksstatistikk() = Vedtaksstatistikk(
        id = id,
        fagsakId = id,
        behandlingId = id,
        eksternFagsakId = 1722,
        eksternBehandlingId = 4005,
        relatertBehandlingId = null,
        adressebeskyttelse = AdressebeskyttelseDvh.UGRADERT,
        tidspunktVedtak = LocalDateTime.of(2024, Month.MAY, 7, 20, 30),
        målgrupper = MålgrupperDvh.JsonWrapper(målgrupper = listOf()),
        aktiviteter = AktiviteterDvh.JsonWrapper(aktivitet = listOf()),
        vilkårsvurderinger = VilkårsvurderingerDvh.JsonWrapper(
            vilkårsvurderinger = listOf(),
        ),
        person = "Pelle",
        barn = BarnDvh.JsonWrapper(barn = listOf()),
        behandlingType = BehandlingTypeDvh.FØRSTEGANGSBEHANDLING,
        behandlingÅrsak = BehandlingÅrsakDvh.MANUELT_OPPRETTET,
        vedtakResultat = VedtakResultatDvh.INNVILGET,
        vedtaksperioder = VedtaksperioderDvh.JsonWrapper(vedtaksperioder = listOf()),
        utbetalinger = UtbetalingerDvh.JsonWrapper(utbetalinger = listOf()),
        stønadstype = StønadstypeDvh.BARNETILSYN,
        kravMottatt = null,
        årsakerAvslag = null,
    )
}
