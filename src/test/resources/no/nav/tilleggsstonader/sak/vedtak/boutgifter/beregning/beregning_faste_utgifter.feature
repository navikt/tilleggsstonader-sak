# language: no
# encoding: UTF-8

Egenskap: Beregning av faste utgifter

  Scenario: Vedtaksperiode inneholder utgift til én bolig
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_EN_BOLIG
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når beregner boutgifter

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Vedtaksperiode inneholder utgift til to boliger
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når beregner boutgifter

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Vedtaksperiode inneholder flere utgiftsperioder
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 30.04.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 30.04.2025 | 1000   |

    Når beregner boutgifter

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 28.02.2025 | 1              | 1000         | 4953      | 01.02.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.03.2025 | 31.03.2025 | 1              | 1000         | 4953      | 01.03.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.04.2025 | 30.04.2025 | 1              | 1000         | 4953      | 01.04.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |


  Scenario: Vedtaksperiode krysser nyttår
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 15.11.2024 | 18.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
      | Fom        | Tom        | Utgift |
      | 15.11.2024 | 18.02.2025 | 9000   |

    Når beregner boutgifter

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 15.11.2024 | 14.12.2024 | 1              | 4809         | 4809      | 15.11.2024      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 15.12.2024 | 14.01.2025 | 1              | 4809         | 4809      | 15.12.2024      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 15.01.2025 | 14.02.2025 | 1              | 4953         | 4953      | 15.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 15.02.2025 | 14.03.2025 | 1              | 4953         | 4953      | 15.02.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: To påfølgende vedtaksperioder som dekker utgiften
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2025 | 31.01.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 01.02.2025 | 28.02.2025 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 28.02.2025 | 1000   |

    Når beregner boutgifter

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 28.02.2025 | 1              | 1000         | 4953      | 01.02.2025      | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Utgiftsperiodene er lengre enn vedtaksperiodene
  Regel: Når deler av en utgiftsperiode går inn i en ny løpende måned, skal hele utgiften utbetales (opp til makssats)
    Eksempel: Bruker tar utdanning fra 15. januar til 20. juni, og leier bolig på utdanningsstedet
      Gitt følgende vedtaksperioder for boutgifter
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 15.01.2025 | 20.06.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

      Gitt følgende utgifter for: LØPENDE_UTGIFTER_EN_BOLIG
        | Fom        | Tom        | Utgift |
        | 01.01.2025 | 20.06.2025 | 4000   |

      Når beregner boutgifter

      Så skal beregnet stønad for boutgifter være
        | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
        | 15.01.2025 | 14.02.2025 | 1              | 4000         | 4953      | 15.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.02.2025 | 14.03.2025 | 1              | 4000         | 4953      | 15.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.03.2025 | 14.04.2025 | 1              | 4000         | 4953      | 15.03.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.04.2025 | 14.05.2025 | 1              | 4000         | 4953      | 15.04.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.05.2025 | 14.06.2025 | 1              | 4000         | 4953      | 15.05.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.06.2025 | 14.07.2025 | 1              | 4000         | 4953      | 15.06.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Eksempel: Bruker leier ekstrabolig mye lengre enn hva som er nødvendig for utdanningen
      Gitt følgende vedtaksperioder for boutgifter
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 15.01.2025 | 20.02.2025 | UTDANNING | NEDSATT_ARBEIDSEVNE |

      Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
        | Fom        | Tom        | Utgift |
        | 01.04.2024 | 01.06.2025 | 4000   |

      Når beregner boutgifter

      Så skal beregnet stønad for boutgifter være
        | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe           | Aktivitet |
        | 15.01.2025 | 14.02.2025 | 1              | 4000         | 4953      | 15.01.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
        | 15.02.2025 | 14.03.2025 | 1              | 4000         | 4953      | 15.02.2025      | NEDSATT_ARBEIDSEVNE | UTDANNING |
