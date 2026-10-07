package no.nav.tilleggsstonader.sak.vedtak

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.avrundetStønadsbeløp(): BigDecimal = setScale(0, RoundingMode.HALF_UP)
