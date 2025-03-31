# language: no
# encoding: UTF-8

Egenskap: Beregning av faste utgifter

  Scenario: Vedtaksperiode inneholder utgift en bolig
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.01.2025 | 31.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_EN_BOLIG
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når beregner stønad for boutgifter

    Så skal stønaden for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | AAp       | TILTAK    |

  Scenario: Vedtaksperiode inneholder utgift en to boliger
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.01.2025 | 31.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: LØPENDE_UTGIFTER_TO_BOLIGER
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Når beregner stønad for boutgifter

    Så skal stønaden for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | AAP       | TILTAK    |

#  Scenario: Vedtaksperiode krysser nyttår
#    Gitt følgende vedtaksperioder for boutgifter
#      | Fom        | Tom        | Aktivitet | Målgruppe |
#      | 15.11.2024 | 15.02.2025 | TILTAK    | AAP       |
#
#    Gitt følgende utgifter for: FASTE_UTGIFTER_TO_BOLIGER
#      | Fom        | Tom        | Utgift |
#      | 15.11.2024 | 15.02.2025 | 9000   |
#
#    Når beregner stønad for boutgifter
#
#    Så skal stønaden for boutgifter være
#      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
#      | 15.11.2024 | 14.12.2024 | 1              | 4809         | 4809      | 15.11.2024      | AAP       | TILTAK    |
#      | 15.12.2024 | 14.01.2025 | 1              | 4809         | 4809      | 15.12.2024      | AAP       | TILTAK    |
#      | 15.01.2025 | 14.02.2025 | 1              | 4953         | 4953      | 15.01.2025      | AAP       | TILTAK    |
#      | 15.02.2025 | 15.02.2025 | 1              | 4953         | 4953      | 15.02.2025      | AAP       | TILTAK    |

