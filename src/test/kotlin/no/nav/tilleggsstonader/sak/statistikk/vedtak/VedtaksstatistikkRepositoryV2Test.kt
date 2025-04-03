package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AdressebeskyttelseDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BarnDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingÅrsakDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.LovverketsMålgruppeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StudienivåDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StønadstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtakResultatDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class VedtaksstatistikkRepositoryV2Test : IntegrationTest() {
    @Autowired
    lateinit var vedtakstatistikkRepository: VedtaksstatistikkRepositoryV2

    final val id: BehandlingId = BehandlingId.fromString("3bc11bf2-1421-4d2b-978d-c33a36870ce2")
    final val fagsakId: FagsakId = FagsakId.fromString("76e6ea78-5abb-414b-85df-9387cc7db1f7")

    @Test
    fun `kan skrive vedtaksstatistikk til tabell og verifiserer at ingen breaking changes finnes`() {
        val vedtaksstatistikk = vedtakstatistikkRepository.insert(vedtaksstatistikk())

        val res =
            jdbcTemplate.query(
                "select * from vedtaksstatistikk_v2 where behandling_id=:id",
                mapOf("id" to vedtaksstatistikk.behandlingId.id),
            ) { rs, _ ->
                val columnNames = IntRange(1, rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
                columnNames
                    .filter { it != "endret_tid" } // endret_tid endrer seg hver gang man lagrer vedtaksstatistikk
                    .associate { it to rs.getString(it) }
            }
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res)
        assertFileIsEqual("statistikk/vedtaksstatistikk.json", json)
    }

    private fun vedtaksstatistikk() =
        VedtaksstatistikkV2(
            behandlingId = id,
            fagsakId = fagsakId,
            eksternFagsakId = 1722,
            eksternBehandlingId = 4005,
            relatertBehandlingId = 1000,
            adressebeskyttelse = AdressebeskyttelseDvh.UGRADERT,
            tidspunktVedtak = LocalDateTime.of(2024, Month.MAY, 7, 20, 30),
            søkerIdent = "søkerIdent",
            behandlingType = BehandlingTypeDvh.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsakDvh.MANUELT_OPPRETTET,
            vedtakResultat = VedtakResultatDvh.INNVILGET,
            vedtaksperioder = vedtaksperioderDvhV2JsonWrapper(),
            utbetalinger = utbetalinger(),
            stønadstype = StønadstypeDvh.BARNETILSYN,
            årsakerAvslag = ÅrsakAvslagDvh.JsonWrapper(listOf(ÅrsakAvslagDvh.INGEN_AKTIVITET)),
            årsakerOpphør = ÅrsakOpphørDvh.JsonWrapper(listOf(ÅrsakOpphørDvh.ENDRING_UTGIFTER)),
            opprettetTid = LocalDateTime.of(2024, Month.JANUARY, 7, 20, 30),
            endretTid = LocalDateTime.of(2024, Month.FEBRUARY, 7, 20, 30),
        )

    private fun vedtaksperioderDvhV2JsonWrapper(): VedtaksperioderDvhV2.JsonWrapper =
        VedtaksperioderDvhV2.JsonWrapper(
            vedtaksperioder =
                listOf(
                    VedtaksperioderDvhV2(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 2),
                        aktivitet = AktivitetTypeDvh.TILTAK,
                        lovverketsMålgruppe = LovverketsMålgruppeDvh.NEDSATT_ARBEIDSEVNE,
                        antallBarn = 3,
                        barn = BarnDvh.JsonWrapper(listOf(BarnDvh("fnr"))),
                        studienivå = StudienivåDvh.VIDEREGÅENDE,
                    ),
                ),
        )

    private fun utbetalinger(): UtbetalingerDvhV2.JsonWrapper =
        UtbetalingerDvhV2.JsonWrapper(
            utbetalinger =
                listOf(
                    UtbetalingerDvhV2(
                        fraOgMed = LocalDate.of(2025, 1, 1),
                        tilOgMed = LocalDate.of(2025, 1, 2),
                        type = AndelstypeDvh.TILSYN_BARN_AAP,
                        beløp = 1,
                        makssats = 100,
                        beløpErBegrensetAvMakssats = true,
                    ),
                ),
        )
}
