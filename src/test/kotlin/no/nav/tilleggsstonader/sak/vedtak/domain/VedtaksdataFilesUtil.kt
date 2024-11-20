package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.util.FileUtil
import kotlin.io.path.name

object VedtaksdataFilesUtil {

    val jsonFiler = FileUtil.listDir("vedtak").flatMap { dir ->
        val dirPath = "vedtak/${dir.fileName.fileName}"
        FileUtil.listFiles(dirPath)
            .map { JsonFil(dirPath, it.fileName.name) }
    }

    /**
     * Brukes også som dir-name for json-filer
     */
    fun Stønadstype.tilTypeVedtaksdataSuffix() = when (this) {
        Stønadstype.BARNETILSYN -> "TILSYN_BARN"
        else -> name
    }

    /**
     * @param dirPath vedtak/TILSYN_BARN
     * @param fileName INNVILGELSE_TILSYN_BARN.json
     */
    data class JsonFil(
        val dirPath: String,
        val fileName: String,
    ) {

        /**
         * Ex vedtak/TILSYN_BARN/INNVILGELSE_TILSYN_BARN.json
         */
        fun fullPath() = "$dirPath/$fileName"

        /**
         * Forventer at alle mapper har strukturen
         * /<Stønadstype>/<Type>_<Stønadstype>.json, der vi henter ut <Type>
         */
        fun typeVedtaksdata(): TypeVedtaksdata {
            val type = fileName.substringBefore(".")
            return alleEnumTypeVedtaksdata
                .single { it.second.enumName() == type }
                .second
        }

        override fun toString(): String {
            return fileName
        }
    }
}
