# language: no
# encoding: UTF-8

Egenskap: Beregning av faste utgifter

  Bakgrunn:
    Gitt følgende oppfylte aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 08.08.2024 | 31.06.2025 | UTDANNING |

    Og følgende oppfylte målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe |
      | 08.08.2024 | 31.06.2025 | AAP       |

  Scenario: Vedtaksperiode inneholder utgift til én bolig
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |

  Scenario: Vedtaksperiode inneholder utgift til to boliger
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_TO_BOLIGER for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |

  Scenario: Vedtaksperiode inneholder flere utgiftsperioder
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_TO_BOLIGER for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 30.04.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 30.04.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.02.2025 | 28.02.2025 | 1000         | 4953      | 01.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.03.2025 | 31.03.2025 | 1000         | 4953      | 01.03.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.04.2025 | 30.04.2025 | 1000         | 4953      | 01.04.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1000  | BOUTGIFTER_AAP | 01.01.2025      |
      | 03.02.2025 | 1000  | BOUTGIFTER_AAP | 01.02.2025      |
      | 03.03.2025 | 1000  | BOUTGIFTER_AAP | 01.03.2025      |
      | 01.04.2025 | 1000  | BOUTGIFTER_AAP | 01.04.2025      |


  Scenario: Hvis det finnes en beregningsperiode som overlapper med flere utgiftsperioder, skal feil kastes fordi det ikke er støttet i løsningen enda
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |
      | 01.02.2025 | 28.02.2025 | 1000   |
      | 01.03.2025 | 31.03.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 15.01.2025 | 31.03.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende feilmelding: Vi støtter foreløpig ikke at utbetalingsperioder overlapper mer enn én løpende utgift.

  Scenario: Vedtaksperiode krysser nyttår skal få satsBekreftet=false

    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_TO_BOLIGER for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.11.2024 | 28.02.2025 | 9000   |


    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.11.2024 | 18.02.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 15.11.2024 | 14.12.2024 | 4809         | 4809      | 15.11.2024      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 15.12.2024 | 14.01.2025 | 4809         | 4809      | 15.12.2024      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 15.01.2025 | 14.02.2025 | 4953         | 4953      | 15.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 15.02.2025 | 18.02.2025 | 4953         | 4953      | 15.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Og følgende andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.11.2024 | 4809  | BOUTGIFTER_AAP | 15.11.2024      |
      | 02.12.2024 | 4809  | BOUTGIFTER_AAP | 15.12.2024      |
      | 01.01.2025 | 4953  | BOUTGIFTER_AAP | 15.01.2025      |
      | 03.02.2025 | 4953  | BOUTGIFTER_AAP | 15.02.2025      |

  Scenario: To påfølgende vedtaksperioder som dekker utgiften
    Gitt følgende boutgifter av type LØPENDE_UTGIFTER_TO_BOLIGER for behandling=1
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 28.02.2025 | 1000   |

    Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |
      | 01.02.2025 | 28.02.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Så kan vi forvente følgende beregningsresultat for behandling=1
      | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.02.2025 | 28.02.2025 | 1000         | 4953      | 01.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

  Regel: Dersom deler av en utgiftsperiode går inn i en ny løpende måned, skal hele utgiften utbetales (opp til makssats)
    Scenario: Bruker tar utdanning fra 15. april til 20. juni, og leier bolig på utdanningsstedet

      Gitt følgende boutgifter av type LØPENDE_UTGIFTER_EN_BOLIG for behandling=1
        | Fom        | Tom        | Utgift |
        | 01.04.2025 | 30.06.2025 | 4000   |

      Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 15.04.2025 | 20.06.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

      Så kan vi forvente følgende beregningsresultat for behandling=1
        | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
        | 15.04.2025 | 14.05.2025 | 4000         | 4953      | 15.04.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.05.2025 | 14.06.2025 | 4000         | 4953      | 15.05.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.06.2025 | 20.06.2025 | 4000         | 4953      | 15.06.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Scenario: Bruker leier ekstrabolig mye lengre enn hva som er nødvendig for utdanningen
      Gitt følgende boutgifter av type LØPENDE_UTGIFTER_TO_BOLIGER for behandling=1
        | Fom        | Tom        | Utgift |
        | 01.04.2024 | 31.06.2025 | 4000   |

      Når vi innvilger boutgifter for behandling=1 med følgende vedtaksperioder
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 15.01.2025 | 20.02.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

      Så kan vi forvente følgende beregningsresultat for behandling=1
        | Fom        | Tom        | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
        | 15.01.2025 | 14.02.2025 | 4000         | 4953      | 15.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.02.2025 | 20.02.2025 | 4000         | 4953      | 15.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
