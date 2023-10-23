package no.nav.tilleggsstonader.sak.util

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
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun oppgave(
    behandling: Behandling,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): OppgaveDomain =
    OppgaveDomain(
        behandlingId = behandling.id,
        gsakOppgaveId = gsakOppgaveId,
        type = type,
        erFerdigstilt = erFerdigstilt,
    )

fun behandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    eksternId: EksternBehandlingId = EksternBehandlingId(),
    vedtakstidspunkt: LocalDateTime? = null,
    kravMottatt: LocalDate? = null,
): Behandling =
    Behandling(
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
        eksternId = eksternId,
        vedtakstidspunkt = vedtakstidspunkt
            ?: if (resultat != BehandlingResultat.IKKE_SATT) SporbarUtils.now() else null,
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
): Saksbehandling =
    saksbehandling(
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
): Saksbehandling =
    Saksbehandling(
        id = behandling.id,
        eksternId = behandling.eksternId.id,
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
        eksternFagsakId = fagsak.eksternId.id,
        stønadstype = fagsak.stønadstype,
        opprettetAv = behandling.sporbar.opprettetAv,
        opprettetTid = behandling.sporbar.opprettetTid,
        endretTid = behandling.sporbar.endret.endretTid,
        kravMottatt = behandling.kravMottatt,
    )

fun Behandling.innvilgetOgFerdigstilt() =
    this.copy(
        resultat = BehandlingResultat.INNVILGET,
        status = BehandlingStatus.FERDIGSTILT,
        vedtakstidspunkt = SporbarUtils.now(),
    )

fun behandlingBarn(
    behandlingId: UUID = UUID.randomUUID(),
    personIdent: String = "1",
    søknadBarnId: UUID? = null,
) = BehandlingBarn(
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
    eksternId: EksternFagsakId = EksternFagsakId(),
    sporbar: Sporbar = Sporbar(),
    fagsakPersonId: UUID = UUID.randomUUID(),
): Fagsak {
    return fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), eksternId, sporbar)
}

fun fagsak(
    stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
    id: UUID = UUID.randomUUID(),
    person: FagsakPerson,
    eksternId: EksternFagsakId = EksternFagsakId(),
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
    eksternId: EksternFagsakId = EksternFagsakId(),
): FagsakDomain =
    FagsakDomain(
        id = id,
        fagsakPersonId = personId,
        stønadstype = stønadstype,
        eksternId = eksternId,
    )

fun Fagsak.tilFagsakDomain() =
    FagsakDomain(
        id = id,
        fagsakPersonId = fagsakPersonId,
        stønadstype = stønadstype,
        eksternId = eksternId,
        sporbar = sporbar,
    )

fun vilkår(
    behandlingId: UUID,
    resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    type: VilkårType = VilkårType.EKSEMPEL,
    delvilkår: List<Delvilkår> = emptyList(),
    barnId: UUID? = null,
    opphavsvilkår: Opphavsvilkår? = null,
): Vilkår =
    Vilkår(
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

fun tilkjentYtelse(
    behandlingId: UUID,
    stønadsår: Int = 2021,
    startdato: LocalDate? = null,
    beløp: Int = 11554,
): TilkjentYtelse {
    val andeler = listOf(
        AndelTilkjentYtelse(
            beløp = beløp,
            stønadFom = LocalDate.of(stønadsår, 1, 1),
            stønadTom = LocalDate.of(stønadsår, 12, 31),
            kildeBehandlingId = behandlingId,
        ),
    )
    return TilkjentYtelse(
        behandlingId = behandlingId,
        startdato = min(startdato, andeler.minOfOrNull { it.stønadFom }) ?: error("Må sette startdato"),
        andelerTilkjentYtelse = andeler,
    )
}

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
        deltBosted = emptyList(),
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

fun søker(sivilstand: List<SivilstandMedNavn> = emptyList()): Søker =
    Søker(
        adressebeskyttelse = Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, Metadata(false)),
        bostedsadresse = listOf(),
        dødsfall = null,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        KjønnType.KVINNE,
        listOf(),
        Navn("fornavn", null, "etternavn", Metadata(false)),
        listOf(),
        listOf(),
        sivilstand = sivilstand,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
    )
 */
