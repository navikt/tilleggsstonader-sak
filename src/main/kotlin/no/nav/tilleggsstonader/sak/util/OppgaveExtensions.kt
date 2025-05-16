package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum

fun Oppgave.erÅpen() =
    status == StatusEnum.OPPRETTET ||
        status == StatusEnum.UNDER_BEHANDLING ||
        status == StatusEnum.AAPNET
