package no.nav.tilleggsstonader.sak.cucumber

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val norskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val norskÅrMånedFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
val isoDatoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val isoÅrMånedFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

fun parseDato(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): LocalDate = parseDato(domenebegrep.nøkkel, rad)

fun parseValgfriDato(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): LocalDate? = parseValgfriDato(domenebegrep.nøkkel, rad)

fun parseÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): YearMonth = parseValgfriÅrMåned(domenebegrep, rad)!!

fun parseValgfriÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): YearMonth? {
    val verdi = rad[domenebegrep.nøkkel]
    if (verdi == null || verdi == "") {
        return null
    }

    return parseÅrMåned(verdi)
}

fun parseString(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): String = verdi(domenebegrep.nøkkel, rad)

fun parseValgfriString(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): String? = valgfriVerdi(domenebegrep.nøkkel, rad)

fun parseBooleanMedBooleanVerdi(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Boolean {
    val verdi = verdi(domenebegrep.nøkkel, rad)

    return when (verdi) {
        "true" -> true
        else -> false
    }
}

fun parseBooleanJaIsTrue(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Boolean =
    when (valgfriVerdi(domenebegrep.nøkkel, rad)) {
        "Ja" -> true
        else -> false
    }

fun parseBoolean(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Boolean {
    val verdi = verdi(domenebegrep.nøkkel, rad)

    return when (verdi) {
        "Ja" -> true
        else -> false
    }
}

fun parseBoolean(verdi: String): Boolean =
    when (verdi) {
        "Ja" -> true
        else -> false
    }

fun parseValgfriBoolean(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): Boolean? {
    val verdi = rad[domenebegrep.nøkkel]
    if (verdi == null || verdi == "") {
        return null
    }

    return when (verdi) {
        "Ja" -> true
        "Nei" -> false
        else -> null
    }
}

fun parseDato(
    domenebegrep: String,
    rad: Map<String, String>,
): LocalDate {
    val dato = rad[domenebegrep]!!

    return parseDato(dato)
}

fun parseDato(dato: String): LocalDate =
    if (dato.contains(".")) {
        LocalDate.parse(dato, norskDatoFormatter)
    } else {
        LocalDate.parse(dato, isoDatoFormatter)
    }

fun parseValgfriDato(
    domenebegrep: String,
    rad: Map<String, String?>,
): LocalDate? {
    val verdi = rad[domenebegrep]
    if (verdi == null || verdi == "") {
        return null
    }

    return if (verdi.contains(".")) {
        LocalDate.parse(verdi, norskDatoFormatter)
    } else {
        LocalDate.parse(verdi, isoDatoFormatter)
    }
}

fun parseÅrMåned(verdi: String): YearMonth =
    if (verdi.contains(".")) {
        YearMonth.parse(verdi, norskÅrMånedFormatter)
    } else {
        YearMonth.parse(verdi, isoÅrMånedFormatter)
    }

fun parseÅrMånedEllerDato(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): ÅrMånedEllerDato =
    parseValgfriÅrMånedEllerDato(domenebegrep, rad)
        ?: error("Mangler verdi for ${domenebegrep.nøkkel}")

fun parseValgfriÅrMånedEllerDato(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): ÅrMånedEllerDato? {
    val verdi = rad[domenebegrep.nøkkel]
    if (verdi == null || verdi == "") {
        return null
    }
    val dato =
        when (verdi.toList().count { it == '.' || it == '-' }) {
            2 -> parseDato(verdi)
            1 -> parseÅrMåned(verdi)
            else -> error("Er datoet=$verdi riktigt formatert? Trenger å være på norskt eller iso-format")
        }
    return ÅrMånedEllerDato(dato)
}

fun verdi(
    nøkkel: String,
    rad: Map<String, String>,
): String {
    val verdi = rad[nøkkel]

    if (verdi == null || verdi == "") {
        throw java.lang.RuntimeException("Fant ingen verdi for $nøkkel")
    }

    return verdi
}

fun valgfriVerdi(
    nøkkel: String,
    rad: Map<String, String>,
): String? = rad[nøkkel]

fun parseInt(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Int {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")
    return Integer.parseInt(verdi)
}

fun parseFloat(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Float {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")
    return verdi.toFloat()
}

fun parseBigDecimal(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): BigDecimal {
    val verdi = verdi(domenebegrep.nøkkel, rad)
    return verdi.toBigDecimal()
}

fun parseDouble(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Double {
    val verdi = verdi(domenebegrep.nøkkel, rad)
    return verdi.toDouble()
}

fun parseValgfriDouble(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Double? {
    return valgfriVerdi(domenebegrep.nøkkel, rad)?.toDouble() ?: return null
}

fun parseValgfriInt(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Int? {
    valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null

    return parseInt(domenebegrep, rad)
}

fun parseValgfriIntRange(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Pair<Int, Int>? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null

    return Pair(
        Integer.parseInt(verdi.split("-").first()),
        Integer.parseInt(verdi.split("-").last()),
    )
}

inline fun <reified T : Enum<T>> parseValgfriEnum(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): T? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null
    return enumValueOf<T>(verdi.uppercase())
}

inline fun <reified T : Enum<T>> parseEnumUtenUppercase(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): T? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null
    return enumValueOf<T>(verdi)
}

inline fun <reified T : Enum<T>> parseEnum(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): T = parseValgfriEnum<T>(domenebegrep, rad)!!
