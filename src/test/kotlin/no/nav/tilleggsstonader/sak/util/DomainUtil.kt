package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerKilde
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BREVDATO_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

fun oppgave(
    behandling: Behandling,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): OppgaveDomain = oppgave(behandling.id, erFerdigstilt, gsakOppgaveId, type)

fun oppgave(
    behandlingId: BehandlingId?,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): OppgaveDomain =
    OppgaveDomain(
        behandlingId = behandlingId,
        gsakOppgaveId = gsakOppgaveId,
        type = type,
        erFerdigstilt = erFerdigstilt,
    )

fun behandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.INNGANGSVILKÅR,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    id: BehandlingId = BehandlingId.random(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeIverksatteBehandlingId: BehandlingId? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    henlagtBegrunnelse: String? = "Registrert feil",
    vedtakstidspunkt: LocalDateTime? = null,
    kravMottatt: LocalDate? = null,
    revurderFra: LocalDate? = null,
    nyeOpplysningerMetadata: NyeOpplysningerMetadata? = null,
): Behandling =
    Behandling(
        fagsakId = fagsak.id,
        forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
        id = id,
        type = type,
        status = status,
        steg = steg,
        kategori = kategori,
        resultat = resultat,
        sporbar = Sporbar(opprettetTid = opprettetTid),
        årsak = årsak,
        henlagtÅrsak = henlagtÅrsak,
        henlagtBegrunnelse = henlagtBegrunnelse,
        vedtakstidspunkt =
            vedtakstidspunkt
                ?: if (resultat != BehandlingResultat.IKKE_SATT) SporbarUtils.now() else null,
        kravMottatt = kravMottatt,
        revurderFra = revurderFra,
        nyeOpplysningerMetadata = nyeOpplysningerMetadata,
    )

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    id: BehandlingId = BehandlingId.random(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeIverksatteBehandlingId: BehandlingId? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    kravMottatt: LocalDate? = null,
    revurderFra: LocalDate? = null,
): Saksbehandling =
    saksbehandling(
        fagsak,
        Behandling(
            fagsakId = fagsak.id,
            forrigeIverksatteBehandlingId = forrigeIverksatteBehandlingId,
            id = id,
            type = type,
            status = status,
            steg = steg,
            resultat = resultat,
            sporbar = Sporbar(opprettetTid = opprettetTid),
            årsak = årsak,
            henlagtÅrsak = henlagtÅrsak,
            kravMottatt = kravMottatt,
            kategori = BehandlingKategori.NASJONAL,
            revurderFra = revurderFra,
        ),
    )

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    behandling: Behandling = behandling(),
): Saksbehandling =
    Saksbehandling(
        id = behandling.id,
        eksternId = EksternBehandlingId(behandlingId = behandling.id).id,
        forrigeIverksatteBehandlingId = behandling.forrigeIverksatteBehandlingId,
        type = behandling.type,
        status = behandling.status,
        steg = behandling.steg,
        kategori = behandling.kategori,
        årsak = behandling.årsak,
        resultat = behandling.resultat,
        vedtakstidspunkt = behandling.vedtakstidspunkt,
        henlagtÅrsak = behandling.henlagtÅrsak,
        henlagtBegrunnelse = behandling.henlagtBegrunnelse,
        ident = fagsak.hentAktivIdent(),
        fagsakId = fagsak.id,
        fagsakPersonId = fagsak.fagsakPersonId,
        eksternFagsakId = fagsak.eksternId.id,
        stønadstype = fagsak.stønadstype,
        opprettetAv = behandling.sporbar.opprettetAv,
        opprettetTid = behandling.sporbar.opprettetTid,
        endretAv = behandling.sporbar.endret.endretAv,
        endretTid = behandling.sporbar.endret.endretTid,
        kravMottatt = behandling.kravMottatt,
        revurderFra = behandling.revurderFra,
    )

fun behandlingBarn(
    id: BarnId = BarnId.random(),
    behandlingId: BehandlingId = BehandlingId.random(),
    personIdent: String = "1",
) = BehandlingBarn(
    id = id,
    behandlingId = behandlingId,
    ident = personIdent,
)

val defaultIdenter = setOf(PersonIdent("15"))

fun fagsakPerson(identer: Set<PersonIdent> = defaultIdenter) = FagsakPerson(identer = identer)

fun fagsak(
    identer: Set<PersonIdent> = defaultIdenter,
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    id: FagsakId = FagsakId.random(),
    eksternId: EksternFagsakId = EksternFagsakId(fagsakId = id),
    sporbar: Sporbar = Sporbar(),
    fagsakPersonId: FagsakPersonId = FagsakPersonId.random(),
): Fagsak = fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), eksternId, sporbar)

fun fagsak(
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    id: FagsakId = FagsakId.random(),
    person: FagsakPerson,
    eksternId: EksternFagsakId = EksternFagsakId(fagsakId = id),
    sporbar: Sporbar = Sporbar(),
): Fagsak =
    Fagsak(
        id = id,
        fagsakPersonId = person.id,
        personIdenter = person.identer,
        stønadstype = stønadstype,
        eksternId = eksternId,
        sporbar = sporbar,
    )

fun fagsakDomain(
    id: FagsakId = FagsakId.random(),
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    personId: FagsakPersonId = FagsakPersonId.random(),
): FagsakDomain =
    FagsakDomain(
        id = id,
        fagsakPersonId = personId,
        stønadstype = stønadstype,
    )

fun Fagsak.tilFagsakDomain() =
    FagsakDomain(
        id = id,
        fagsakPersonId = fagsakPersonId,
        stønadstype = stønadstype,
        sporbar = sporbar,
    )

fun vilkår(
    behandlingId: BehandlingId,
    type: VilkårType,
    resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    status: VilkårStatus = VilkårStatus.NY,
    delvilkår: List<Delvilkår> = emptyList(),
    barnId: BarnId? = null,
    opphavsvilkår: Opphavsvilkår? = null,
    fom: LocalDate? = YearMonth.now().atDay(1),
    tom: LocalDate? = YearMonth.now().atEndOfMonth(),
    utgift: Int? = 100,
    erFremtidigUtgift: Boolean = false,
): Vilkår =
    Vilkår(
        behandlingId = behandlingId,
        resultat = resultat,
        status = status,
        type = type,
        barnId = barnId,
        delvilkårwrapper = DelvilkårWrapper(delvilkår),
        opphavsvilkår = opphavsvilkår,
        fom = fom,
        tom = tom,
        utgift = utgift,
        erFremtidigUtgift = erFremtidigUtgift,
        gitVersjon = Applikasjonsversjon.versjon,
    )

fun fagsakpersoner(vararg identer: String): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it)
        }.toSet()

fun fagsakpersoner(identer: Set<String>): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it)
        }.toSet()

fun fagsakpersonerAvPersonIdenter(identer: Set<PersonIdent>): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it.ident, sporbar = it.sporbar)
        }.toSet()

fun vedtaksbrev(
    behandlingId: BehandlingId = BehandlingId.random(),
    saksbehandlerHtml: String = "Brev med $BESLUTTER_SIGNATUR_PLACEHOLDER og $BREVDATO_PLACEHOLDER",
    saksbehandlersignatur: String = "Saksbehandler Signatur",
    besluttersignatur: String? = "Beslutter signatur",
    beslutterPdf: Fil? = Fil("123".toByteArray()),
    saksbehandlerIdent: String = "saksbehandlerIdent",
    beslutterIdent: String = "beslutterIdent",
) = Vedtaksbrev(
    behandlingId = behandlingId,
    saksbehandlerHtml = saksbehandlerHtml,
    saksbehandlersignatur = saksbehandlersignatur,
    besluttersignatur = besluttersignatur,
    beslutterPdf = beslutterPdf,
    saksbehandlerIdent = saksbehandlerIdent,
    beslutterIdent = beslutterIdent,
)

fun journalpost(
    journalpostId: String = UUID.randomUUID().toString(),
    journalposttype: Journalposttype = Journalposttype.U,
    journalstatus: Journalstatus = Journalstatus.FERDIGSTILT,
    tema: String = Tema.TSO.toString(),
    dokumenter: List<DokumentInfo>? = null,
    bruker: Bruker? = null,
) = Journalpost(
    journalpostId = journalpostId,
    journalposttype = journalposttype,
    journalstatus = journalstatus,
    tema = tema,
    dokumenter = dokumenter,
    bruker = bruker,
)

fun dokumentInfo(
    dokumentInfoId: String = UUID.randomUUID().toString(),
    dokumentvarianter: List<Dokumentvariant>? = null,
) = DokumentInfo(
    dokumentInfoId = dokumentInfoId,
    dokumentvarianter = dokumentvarianter,
)

fun dokumentvariant(
    variantformat: Dokumentvariantformat = Dokumentvariantformat.ARKIV,
    saksbehandlerHarTilgang: Boolean = true,
    filnavn: String? = "filnavn",
) = Dokumentvariant(
    variantformat = variantformat,
    saksbehandlerHarTilgang = saksbehandlerHarTilgang,
    filnavn = filnavn,
)

fun nyeOpplysningerMetadata(
    kilde: NyeOpplysningerKilde = NyeOpplysningerKilde.ETTERSENDING,
    endringer: List<NyeOpplysningerEndring> = listOf(NyeOpplysningerEndring.AKTIVITET),
    beskrivelse: String? = "tralala",
) = NyeOpplysningerMetadata(
    kilde = kilde,
    endringer = endringer,
    beskrivelse = beskrivelse,
)
