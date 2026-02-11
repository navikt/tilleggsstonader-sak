package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseEnum
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseString
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.kjøreliste.AvviksbegrunnelseDag
import no.nav.tilleggsstonader.sak.kjøreliste.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.kjøreliste.Kjøreliste
import no.nav.tilleggsstonader.sak.kjøreliste.KjørelisteDag
import no.nav.tilleggsstonader.sak.kjøreliste.KjørelisteDto
import no.nav.tilleggsstonader.sak.kjøreliste.KjørelisteRepository
import no.nav.tilleggsstonader.sak.kjøreliste.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.kjøreliste.UtfyltDagResultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DomenenøkkelPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("unused", "ktlint:standard:function-naming")
class PrivatBilKjørelisteStepDefinitions : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val fom = 15 september 2025
    val tom = 14 oktober 2025

    var saksbehandling: Saksbehandling? = null
    var reiseId: ReiseId? = null
    var resultat: List<KjørelisteDto> = emptyList()

    @Gitt("gitt følgende ramme for daglig reise privat bil")
    fun `gitt følgende ramme for daglig reise privat bil`(dataTable: DataTable) {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val ramme = mapRamme(dataTable)

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = ramme.fom, tom = ramme.tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = ramme.fom, tom = ramme.tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBil(fom = ramme.fom, tom = ramme.tom, reisedagerPerUke = ramme.antallDagerPerUke)
                    }
                }
            }

        saksbehandling = behandlingRepository.finnSaksbehandling(behandlingId)

        val rammevedtak = kall.privatBil.hentRammevedtak(IdentRequest("12345678910"))
        reiseId = rammevedtak.single().reiseId
    }

    @Gitt("gitt følgende kjøreliste for privat bil")
    fun `gitt følgende kjøreliste for privat bil`(dataTable: DataTable) {
        kjørelisteRepository.insert(
            Kjøreliste(
                journalpostId = "",
                fagsakId = saksbehandling!!.fagsakId,
                datoMottatt = LocalDateTime.now(),
                data =
                    InnsendtKjøreliste(
                        reiseId = reiseId!!,
                        reisedager = mapKjøreliste(dataTable),
                    ),
            ),
        )
    }

    @Når("henter kjøreliste for behandling")
    fun `henter kjøreliste for behandling`() {
        resultat = kall.privatBil.hentKjørelisteForBehandling(saksbehandling!!.id)
    }

    @Så("forvent følgende kjøreliste for behandling")
    fun `forvent følgende kjøreliste for behandling`(dataTable: DataTable) {
        val forventetResultat = mapDager(dataTable).sortedBy { it.dato }
        val resultatSomDager = resultat.tilCucumberUker().sortedBy { it.dato }

        assertThat(resultatSomDager).isEqualTo(forventetResultat)
    }

    data class RammeVedtakCucumber(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDagerPerUke: Int,
    )

    private fun mapRamme(dataTable: DataTable) =
        dataTable
            .mapRad { rad ->
                RammeVedtakCucumber(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    antallDagerPerUke = parseInt(DomenenøkkelPrivatBil.ANTALL_REISEDAGER_PER_UKE, rad),
                )
            }.single()

    private fun mapKjøreliste(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            KjørelisteDag(
                dato = parseDato(DomenenøkkelFelles.FOM, rad),
                harKjørt = parseBoolean(DomenenøkkelPrivatBilIntegrasjon.HAR_KJØRT, rad),
                parkeringsutgift = parseInt(DomenenøkkelPrivatBilIntegrasjon.PARKERINGSUTGIFT, rad),
            )
        }

    data class DagDtoCucumber(
        val ukeNr: Int,
        val ukedag: String,
        val dato: LocalDate,
        val resultat: UtfyltDagResultat,
        val automatiskVurdering: UtfyltDagAutomatiskVurdering,
        val avviksbegrunnelse: AvviksbegrunnelseDag?,
        val parkeringsutgift: Int?,
    )

    private fun mapDager(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            DagDtoCucumber(
                ukeNr = parseInt(DomenenøkkelPrivatBilIntegrasjon.UKE_NR, rad),
                ukedag = parseString(DomenenøkkelPrivatBilIntegrasjon.UKEDAG, rad),
                dato = parseDato(DomenenøkkelPrivatBilIntegrasjon.DATO, rad),
                resultat = parseEnum(DomenenøkkelPrivatBilIntegrasjon.RESULTAT, rad),
                automatiskVurdering = parseEnum(DomenenøkkelPrivatBilIntegrasjon.AUTOMATISK_VURDERING, rad),
                avviksbegrunnelse =
                    parseValgfriEnum<AvviksbegrunnelseDag>(
                        DomenenøkkelPrivatBilIntegrasjon.AVVIKSBEGRUNNELSE,
                        rad,
                    ),
                parkeringsutgift = parseValgfriInt(DomenenøkkelPrivatBilIntegrasjon.PARKERINGSUTGIFT, rad),
            )
        }

    private fun List<KjørelisteDto>.tilCucumberUker(): List<DagDtoCucumber> =
        this.flatMap { it.uker }.flatMap { uke ->
            uke.dager.map { dag ->
                DagDtoCucumber(
                    ukeNr = uke.ukenummer,
                    ukedag = dag.ukedag,
                    dato = dag.dato,
                    resultat = dag.avklartDag!!.resultat,
                    automatiskVurdering = dag.avklartDag.automatiskVurdering,
                    avviksbegrunnelse = dag.avklartDag.avviksbegrunnelse,
                    parkeringsutgift = dag.avklartDag.parkeringsutgift,
                )
            }
        }

    enum class DomenenøkkelPrivatBilIntegrasjon(
        override val nøkkel: String,
    ) : Domenenøkkel {
        HAR_KJØRT("Har kjørt"),
        PARKERINGSUTGIFT("Parkeringsutgift"),
        UKE_NR("Uke nr"),
        UKEDAG("Ukedag"),
        DATO("Dato"),
        RESULTAT("Resultat"),
        AUTOMATISK_VURDERING("Automatisk vurdering"),
        AVVIKSBEGRUNNELSE("Avviksbegrunnelse"),
    }
}
