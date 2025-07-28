package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

class DetaljertVedtaksperiodeDagligReiseTSR(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
) : Periode<LocalDate>
