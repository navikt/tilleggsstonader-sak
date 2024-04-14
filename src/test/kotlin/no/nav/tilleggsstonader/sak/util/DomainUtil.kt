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
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BREVDATO_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun oppgave(
    behandling: Behandling,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): OppgaveDomain = oppgave(behandling.id, erFerdigstilt, gsakOppgaveId, type)

fun oppgave(
    behandlingId: UUID?,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): OppgaveDomain = OppgaveDomain(
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
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    vedtakstidspunkt: LocalDateTime? = null,
    kravMottatt: LocalDate? = null,
): Behandling = Behandling(
    fagsakId = fagsak.id,
    forrigeBehandlingId = forrigeBehandlingId,
    id = id,
    type = type,
    status = status,
    steg = steg,
    kategori = kategori,
    resultat = resultat,
    sporbar = Sporbar(opprettetTid = opprettetTid),
    årsak = årsak,
    henlagtÅrsak = henlagtÅrsak,
    vedtakstidspunkt = vedtakstidspunkt ?: if (resultat != BehandlingResultat.IKKE_SATT) SporbarUtils.now() else null,
    kravMottatt = kravMottatt,
)

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    kravMottatt: LocalDate? = null,
): Saksbehandling = saksbehandling(
    fagsak,
    Behandling(
        fagsakId = fagsak.id,
        forrigeBehandlingId = forrigeBehandlingId,
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
    ),
)

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    behandling: Behandling = behandling(),
): Saksbehandling = Saksbehandling(
    id = behandling.id,
    eksternId = 0,
    forrigeBehandlingId = behandling.forrigeBehandlingId,
    type = behandling.type,
    status = behandling.status,
    steg = behandling.steg,
    kategori = behandling.kategori,
    årsak = behandling.årsak,
    resultat = behandling.resultat,
    vedtakstidspunkt = behandling.vedtakstidspunkt,
    henlagtÅrsak = behandling.henlagtÅrsak,
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
)

fun Behandling.innvilgetOgFerdigstilt() = this.copy(
    resultat = BehandlingResultat.INNVILGET,
    status = BehandlingStatus.FERDIGSTILT,
    vedtakstidspunkt = SporbarUtils.now(),
)

fun behandlingBarn(
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID = UUID.randomUUID(),
    personIdent: String = "1",
    søknadBarnId: UUID? = null,
) = BehandlingBarn(
    id = id,
    behandlingId = behandlingId,
    søknadBarnId = søknadBarnId,
    ident = personIdent,
)

val defaultIdenter = setOf(PersonIdent("15"))
fun fagsakPerson(
    identer: Set<PersonIdent> = defaultIdenter,
) = FagsakPerson(identer = identer)

fun fagsak(
    identer: Set<PersonIdent> = defaultIdenter,
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    id: UUID = UUID.randomUUID(),
    eksternId: EksternFagsakId = EksternFagsakId(fagsakId = id),
    sporbar: Sporbar = Sporbar(),
    fagsakPersonId: UUID = UUID.randomUUID(),
): Fagsak {
    return fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), eksternId, sporbar)
}

fun fagsak(
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    id: UUID = UUID.randomUUID(),
    person: FagsakPerson,
    eksternId: EksternFagsakId = EksternFagsakId(fagsakId = id),
    sporbar: Sporbar = Sporbar(),
): Fagsak {
    return Fagsak(
        id = id,
        fagsakPersonId = person.id,
        personIdenter = person.identer,
        stønadstype = stønadstype,
        eksternId = eksternId,
        sporbar = sporbar,
    )
}

fun fagsakDomain(
    id: UUID = UUID.randomUUID(),
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    personId: UUID = UUID.randomUUID(),
): FagsakDomain = FagsakDomain(
    id = id,
    fagsakPersonId = personId,
    stønadstype = stønadstype,
)

fun Fagsak.tilFagsakDomain() = FagsakDomain(
    id = id,
    fagsakPersonId = fagsakPersonId,
    stønadstype = stønadstype,
    sporbar = sporbar,
)

fun stønadsperiode(
    behandlingId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    målgruppe: MålgruppeType = MålgruppeType.AAP,
    aktivitet: AktivitetType = AktivitetType.TILTAK,
): Stønadsperiode = Stønadsperiode(
    behandlingId = behandlingId,
    fom = fom,
    tom = tom,
    målgruppe = målgruppe,
    aktivitet = aktivitet,
)

fun vilkår(
    behandlingId: UUID,
    resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    type: VilkårType = VilkårType.EKSEMPEL,
    delvilkår: List<Delvilkår> = emptyList(),
    barnId: UUID? = null,
    opphavsvilkår: Opphavsvilkår? = null,
): Vilkår = Vilkår(
    behandlingId = behandlingId,
    resultat = resultat,
    type = type,
    barnId = barnId,
    delvilkårwrapper = DelvilkårWrapper(delvilkår),
    opphavsvilkår = opphavsvilkår,
)

fun fagsakpersoner(vararg identer: String): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it)
}.toSet()

fun fagsakpersoner(identer: Set<String>): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it)
}.toSet()

fun fagsakpersonerAvPersonIdenter(identer: Set<PersonIdent>): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it.ident, sporbar = it.sporbar)
}.toSet()

/*
fun årsakRevurdering(
    behandlingId: UUID = UUID.randomUUID(),
    opplysningskilde: Opplysningskilde = Opplysningskilde.MELDING_MODIA,
    årsak: Revurderingsårsak = Revurderingsårsak.ANNET,
    beskrivelse: String? = null,
) =
    ÅrsakRevurdering(
        behandlingId = behandlingId,
        opplysningskilde = opplysningskilde,
        årsak = årsak,
        beskrivelse = beskrivelse,
    )
*/
/*
fun revurderingsinformasjon() = RevurderingsinformasjonDto(
    LocalDate.now(),
    ÅrsakRevurderingDto(Opplysningskilde.MELDING_MODIA, Revurderingsårsak.ANNET, "beskrivelse"),
)
 */

fun vedtaksbrev(
    behandlingId: UUID = UUID.randomUUID(),
    saksbehandlerHtml: String = "Brev med ${BESLUTTER_SIGNATUR_PLACEHOLDER} og ${BREVDATO_PLACEHOLDER}",
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
) =
    Dokumentvariant(
        variantformat = variantformat,
        saksbehandlerHarTilgang = saksbehandlerHarTilgang,
        filnavn = filnavn,
    )
/*
fun vedtak(
    behandlingId: UUID,
    resultatType: ResultatType = ResultatType.INNVILGE,
    år: Int = 2021,
    inntekter: InntektWrapper = InntektWrapper(listOf(inntektsperiode(år = år))),
    perioder: PeriodeWrapper = PeriodeWrapper(listOf(vedtaksperiode(år = år))),
): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = resultatType,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder = perioder,
        inntekter = inntekter,
        saksbehandlerIdent = "VL",
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
    )
 */
/*
fun vedtakBarnetilsyn(
    behandlingId: UUID,
    barn: List<UUID>,
    resultatType: ResultatType = ResultatType.INNVILGE,
    beløp: Int = 1000,
    kontantstøtteWrapper: KontantstøtteWrapper = KontantstøtteWrapper(emptyList()),
    fom: YearMonth,
    tom: YearMonth,
) = Vedtak(
    behandlingId = behandlingId,
    resultatType = resultatType,
    barnetilsyn = BarnetilsynWrapper(listOf(barnetilsynperiode(barn = barn, beløp = beløp, fom = fom, tom = tom)), "begrunnelse"),
    kontantstøtte = kontantstøtteWrapper,
    tilleggsstønad = TilleggsstønadWrapper(false, emptyList(), null),
    saksbehandlerIdent = "VL",
    opprettetAv = "VL",
    opprettetTid = LocalDateTime.now(),
)
 */
/*
fun barnetilsynperiode(
    år: Int = 2022,
    fom: YearMonth = YearMonth.of(år, 1),
    tom: YearMonth = YearMonth.of(år, 12),
    beløp: Int = 1000,
    barn: List<UUID>,
    sanksjonsårsak: Sanksjonsårsak? = null,
    periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
    aktivitetstype: AktivitetstypeBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
) = Barnetilsynperiode(
    periode = Månedsperiode(fom, tom),
    utgifter = beløp,
    barn = barn,
    sanksjonsårsak = sanksjonsårsak,
    periodetype = periodetype,
    aktivitet = aktivitetstype,
)
 */

/*
fun vedtaksperiode(
    år: Int = 2021,
    startDato: LocalDate = LocalDate.of(år, 1, 1),
    sluttDato: LocalDate = LocalDate.of(år, 12, 1),
    vedtaksperiodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitetstype: AktivitetType =
        if (vedtaksperiodeType == VedtaksperiodeType.SANKSJON) AktivitetType.IKKE_AKTIVITETSPLIKT else AktivitetType.BARN_UNDER_ETT_ÅR,
    sanksjonsårsak: Sanksjonsårsak? =
        if (vedtaksperiodeType == VedtaksperiodeType.SANKSJON) Sanksjonsårsak.SAGT_OPP_STILLING else null,
) =
    Vedtaksperiode(startDato, sluttDato, aktivitetstype, vedtaksperiodeType, sanksjonsårsak)
 */
/*
fun vedtaksperiodeDto(
    årMånedFra: LocalDate = LocalDate.of(2021, 1, 1),
    årMånedTil: LocalDate = LocalDate.of(2021, 12, 1),
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
) =
    vedtaksperiodeDto(
        årMånedFra = YearMonth.from(årMånedFra),
        årMånedTil = YearMonth.from(årMånedTil),
        periodeType = periodeType,
        aktivitet = aktivitet,
    )
 */

/*
fun vedtaksperiodeDto(
    årMånedFra: YearMonth = YearMonth.of(2021, 1),
    årMånedTil: YearMonth = YearMonth.of(2021, 12),
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
) =
    VedtaksperiodeDto(
        årMånedFra = årMånedFra,
        årMånedTil = årMånedTil,
        periode = Månedsperiode(årMånedFra, årMånedTil),
        aktivitet = aktivitet,
        periodeType = periodeType,
    )

 */

/*
fun behandlingBarn(
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID,
    søknadBarnId: UUID? = null,
    personIdent: String? = null,
    navn: String? = null,
    fødselTermindato: LocalDate? = null,
): BehandlingBarn {
    return BehandlingBarn(
        id = id,
        behandlingId = behandlingId,
        søknadBarnId = søknadBarnId,
        personIdent = personIdent,
        navn = navn,
        fødselTermindato = fødselTermindato,
        sporbar = Sporbar(opprettetAv = "opprettetAv"),
    )
}
 */

/*
fun barnMedIdent(fnr: String, navn: String, fødsel: Fødsel = fødsel(LocalDate.now())): BarnMedIdent =
    BarnMedIdent(
        adressebeskyttelse = emptyList(),
        bostedsadresse = emptyList(),
        dødsfall = emptyList(),
        forelderBarnRelasjon = emptyList(),
        fødsel = listOf(fødsel),
        navn = Navn(
            fornavn = navn.split(" ")[0],
            mellomnavn = null,
            etternavn = navn.split(" ")[1],
            metadata = Metadata(
                historisk = false,
            ),
        ),
        personIdent = fnr,
    )
 */

/*

fun søker(): Søker =
    Søker(
        adressebeskyttelse = Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, Metadata(false)),
        bostedsadresse = listOf(),
        dødsfall = null,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        Navn("fornavn", null, "etternavn", Metadata(false)),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
    )
 */
