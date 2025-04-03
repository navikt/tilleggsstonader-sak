package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

val alleVedtaksstatistikkJsonConverters =
    listOf(
        ÅrsakerAvslagDvhReader(),
        ÅrsakerAvslagDvhWriter(),
        ÅrsakerOpphørDvhWriter(),
        VedtaksperioderDvhV2Writer(),
        UtbetalingerDvhV2Writer(),
    )

@WritingConverter
private class VedtaksperioderDvhV2Writer : Converter<VedtaksperioderDvhV2.JsonWrapper, PGobject> {
    override fun convert(data: VedtaksperioderDvhV2.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.vedtaksperioder)
        }
}

@WritingConverter
private class UtbetalingerDvhV2Writer : Converter<UtbetalingerDvhV2.JsonWrapper, PGobject> {
    override fun convert(data: UtbetalingerDvhV2.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.utbetalinger)
        }
}

private class ÅrsakerAvslagDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakAvslagDvh.JsonWrapper>()

private class ÅrsakerOpphørDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakOpphørDvh.JsonWrapper>()


private class ÅrsakerAvslagDvhReader : DatabaseConfiguration.JsonReader<ÅrsakAvslagDvh.JsonWrapper>(ÅrsakAvslagDvh.JsonWrapper::class)
