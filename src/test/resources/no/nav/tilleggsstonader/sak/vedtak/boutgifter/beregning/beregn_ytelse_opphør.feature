# language: no
# encoding: UTF-8

Egenskap: Beregning ved opphør av boutgifter

  Bakgrunn:
    Gitt følgende oppfylte aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 01.01.2025 | 31.03.2025 | TILTAK    |

    Og følgende oppfylte målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

  Regel: Ved et opphør avkortes vedtaksperioder og beregningsresultat fra forrige vedtak.
  - beregningsperioder som i sin helhet ligger før revurder fra-datoen beholdes fra forrige vedtak
  - beregningsperioder som overlapper med revurder fra-datoen klippes til dagen før revurder fra, og reberegnes
  - beregningsperioder som i sin helhet ligger etter revurder fra-datoen fjernes

    Scenario: Revurdering fra midt i en beregningsperiode
      Og følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
        | Fom        | Tom        | Utgift |
        | 01.01.2025 | 31.03.2025 | 1000   |

      Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Så kan vi forvente følgende andeler for behandling=1
        | Fom        | Beløp | Type           | Utbetalingsdato |
        | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |
        | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 03.02.2025      |
        | 03.03.2025 | 1000  | BOUTGIFTER_AAP | 03.03.2025      |
    # Merk: 03.02.25 og 03.03.25 tilsvarer første ukedag i hhv februar og mars 2025

      Når vi kopierer perioder fra forrige behandling for behandling=2

      Og vi opphører boutgifter behandling=2 med revurderFra=15.02.2025

      Så kan vi forvente følgende beregningsresultat for behandling=2
        | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
        | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | Ja                          |
        | 01.02.2025 | 14.02.2025 | 1000         | 4953      | 01.02.2025      | Ja                          |

      Og følgende andeler for behandling=2
        | Fom        | Beløp | Type           | Utbetalingsdato |
        | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |
        | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 03.02.2025      |

      Og følgende vedtaksperioder for behandling=2
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 01.01.2025 | 14.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Scenario: Tidligere beregnet ytelse fra før revurder fra-datoen skal ikke reberegnes
      Og følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
        | Fom        | Tom        | Utgift |
        | 01.01.2025 | 28.02.2025 | 99999  |

      Og vi har lagret følgende beregningsresultat for behandling=1
        | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato |
        | 01.01.2025 | 31.01.2025 | 99999        | 4953      | 01.01.2025      |
        | 01.02.2025 | 28.02.2025 | 99999        | 4953      | 03.02.2025      |
            # En latterlig stort stønadsbeløp, bare for å gjøre det ekstra tydelig at den ikke blir reberegnet og klippet til makssats

      Og vi kopierer perioder fra forrige behandling for behandling=2

      Når vi opphører boutgifter behandling=2 med revurderFra=01.02.2025

      Så kan vi forvente følgende beregningsresultat for behandling=2
        | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Del av tidligere utbetaling |
        | 01.01.2025 | 31.01.2025 | 99999        | 4953      | 01.01.2025      | Ja                          |

      Og følgende andeler for behandling=2
        | Fom        | Beløp | Type           | Utbetalingsdato |
        | 01.01.2025 | 99999 | BOUTGIFTER_AAP | 01.01.2025      |

      Og følgende vedtaksperioder for behandling=2
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Scenario: Har innvilget to samlinger, og den siste skal opphøres
    Revurderer etter den første perioden, uten å fjerne utgiftene til samlingen i januar.
    Resultat: Forventer feilmelding, ettersom det ikke er støttet å ha utgifter til samling utenfor vedtaksperiodene
      Gitt følgende boutgifter av type UTGIFTER_OVERNATTING for behandling=1
        | Fom        | Tom        | Utgift |
        | 07.01.2025 | 09.01.2025 | 1000   |
        | 15.02.2025 | 18.02.2025 | 3000   |

      Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 07.01.2025 | 09.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 15.02.2025 | 18.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når vi kopierer perioder fra forrige behandling for behandling=2

      Og vi opphører boutgifter behandling=2 med revurderFra=2025-01-09

      Så forvent følgende feilmelding: Du har lagt inn utgifter til midlertidig overnatting som ikke er inneholdt i en vedtaksperiode. Foreløpig støtter vi ikke dette.
