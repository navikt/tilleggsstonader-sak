package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

class VedtaksstatistikkTest : IntegrationTest() {

    @Autowired
    lateinit var vedtakstatistikkRepository: VedtakstatistikkRepository

    final val id = UUID.randomUUID()

    val dummyVedtaksstatistikk = Vedtaksstatistikk(
        id = id,
        fagsakId = id,
        behandlingId = id,
        eksternFagsakId = 1722,
        eksternBehandlingId = 4005,
        relatertBehandlingId = null,
        adressebeskyttelse = AdressebeskyttelseDvh.UGRADERT,
        tidspunktVedtak = LocalDateTime.of(2024, Month.MAY, 7, 20, 30),
        målgruppe = MålgruppeDvh.JsonWrapper(målgruppe = listOf()),
        aktivitet = AktivitetDvh.JsonWrapper(aktivitet = listOf()),
        vilkårsvurderinger = VilkårsvurderingDvh.JsonWrapper(
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
        årsakRevurdering = null,
        avslagÅrsak = null,
    )

    @Test
    fun `kan skrive vedtaksstatistikk til tabell når alle JSON-objekter er tomme lister`() {
        vedtakstatistikkRepository.insert(dummyVedtaksstatistikk)
    }

    @Test
    fun `kan skrive vedtaksstatistikk med flere aktiviteter til tabell`() {
        val nøstedeAktiviteter = AktivitetDvh.JsonWrapper(
            listOf(
                AktivitetDvh(type = AktivitetTypeDvh.REELL_ARBEIDSSØKER, resultat = ResultatVilkårperiodeDvh.OPPFYLT),
                AktivitetDvh(type = AktivitetTypeDvh.UTDANNING, resultat = ResultatVilkårperiodeDvh.IKKE_OPPFYLT),
            ),
        )

        vedtakstatistikkRepository.insert(dummyVedtaksstatistikk.copy(aktivitet = nøstedeAktiviteter))
    }
}
