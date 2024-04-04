package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.MottarPengestøtteTyper
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.OppholdUtenforNorge as OppholdUtenforNorgeKontrakt

class ArbeidOgOppholdMapperTest {
    @Test
    fun `skal mappe alle verdier`() {
        val verdi = mapArbeidOgOpphold(arbeidOgOpphold())
        val expected = ArbeidOgOpphold(
            jobberIAnnetLand = JaNei.JA,
            jobbAnnetLand = "Sverige",
            harPengestøtteAnnetLand = listOf(MottarPengestøtteTyper.ANNEN_PENGESTØTTE),
            pengestøtteAnnetLand = "Sverige",
            harOppholdUtenforNorgeSiste12mnd = JaNei.NEI,
            oppholdUtenforNorgeSiste12mnd = listOf(
                OppholdUtenforNorge(
                    "Sverige",
                    listOf(ÅrsakOppholdUtenforNorge.JOBB),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 2),
                ),
            ),
            harOppholdUtenforNorgeNeste12mnd = JaNei.JA,
            oppholdUtenforNorgeNeste12mnd = listOf(
                OppholdUtenforNorge(
                    "Finland",
                    listOf(ÅrsakOppholdUtenforNorge.FERIE),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 2),
                ),
            ),
        )
        assertThat(verdi).isEqualTo(expected)
    }

    private fun arbeidOgOpphold() = ArbeidOgOppholdKontrakt(
        jobberIAnnetLand = enumJaNei("jobberIAnnetLand"),
        jobbAnnetLand = SelectFelt("jobbAnnetLand", "SWE", "Sverige"),
        harPengestøtteAnnetLand = harPengestøtteAnnetLand(),
        pengestøtteAnnetLand = SelectFelt("pengestøtteAnnetLand", "SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd = enumJaNei("harOppholdUtenforNorgeSiste12mnd", JaNei.NEI),
        oppholdUtenforNorgeSiste12mnd = listOf(
            OppholdUtenforNorgeKontrakt(
                SelectFelt("land", "SWE", "Sverige"),
                årsakJobb(),
                DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
                DatoFelt("Tom", LocalDate.of(2024, 1, 2)),
            ),
        ),
        harOppholdUtenforNorgeNeste12mnd = enumJaNei("harOppholdUtenforNorgeNeste12mnd"),
        oppholdUtenforNorgeNeste12mnd = listOf(
            OppholdUtenforNorgeKontrakt(
                SelectFelt("land", "FIN", "Finland"),
                årsakFerie(),
                DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
                DatoFelt("Tom", LocalDate.of(2024, 1, 2)),
            ),
        ),
    )

    private fun årsakJobb() = EnumFlereValgFelt(
        "årsak",
        listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.JOBB, "Jobb")),
        emptyList(),
    )

    private fun årsakFerie() = EnumFlereValgFelt(
        "årsak",
        listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.FERIE, "Ferie")),
        emptyList(),
    )

    private fun harPengestøtteAnnetLand() = EnumFlereValgFelt(
        label = "harPengestøtteAnnetLand",
        verdier = listOf(VerdiFelt(MottarPengestøtteTyper.ANNEN_PENGESTØTTE, "Annen pengestøtte")),
        alternativer = emptyList(),
    )

    private fun enumJaNei(
        label: String = "",
        verdi: JaNei = JaNei.JA,
    ) = EnumFelt(label, verdi, "", emptyList())
}
