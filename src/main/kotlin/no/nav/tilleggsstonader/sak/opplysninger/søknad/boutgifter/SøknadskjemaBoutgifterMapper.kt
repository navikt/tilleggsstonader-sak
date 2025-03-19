package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.ArsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.boutgifter.fyllutsendinn.MottarDuEllerHarDuNyligSoktOmNoeAvDette
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge

object SøknadskjemaBoutgifterMapper {
    fun map(mottarDuEllerHarDuNyligSoktOmNoeAvDette: MottarDuEllerHarDuNyligSoktOmNoeAvDette): List<Hovedytelse> =
        buildList {
            val list = this
            with(mottarDuEllerHarDuNyligSoktOmNoeAvDette) {
                list.add(overgangsstonad, Hovedytelse.OVERGANGSSTØNAD)
                list.add(gjenlevendepensjon, Hovedytelse.GJENLEVENDEPENSJON)
                list.add(uforetrygd, Hovedytelse.UFØRETRYGD)
                list.add(tiltakspenger, Hovedytelse.TILTAKSPENGER)
                list.add(dagpenger, Hovedytelse.DAGPENGER)
                list.add(sykepenger, Hovedytelse.SYKEPENGER)
                list.add(kvalifiseringsstonad, Hovedytelse.KVALIFISERINGSSTØNAD)
                list.add(mottarIngenPengestotte, Hovedytelse.INGEN_PENGESTØTTE)
                list.add(ingenAvAlternativenePasserForMeg, Hovedytelse.INGEN_PASSENDE_ALTERNATIVER)
            }
        }

    fun map(arsakOppholdUtenforNorge: ArsakOppholdUtenforNorge): List<ÅrsakOppholdUtenforNorge> =
        buildList {
            val list = this
            with(arsakOppholdUtenforNorge) {
                list.add(jobbet, ÅrsakOppholdUtenforNorge.JOBB)
                list.add(studerte, ÅrsakOppholdUtenforNorge.STUDIER)
                list.add(fikkMedisinskBehandling, ÅrsakOppholdUtenforNorge.MEDISINSK_BEHANDLING)
                list.add(varPaFerie, ÅrsakOppholdUtenforNorge.FERIE)
                list.add(besokteFamilie, ÅrsakOppholdUtenforNorge.FAMILIE_BESØK)
                list.add(annet, ÅrsakOppholdUtenforNorge.ANNET)
            }
        }

    private fun <T> MutableList<T>.add(
        boolean: Boolean,
        verdi: T,
    ) {
        if (boolean) {
            add(verdi)
        }
    }
}
