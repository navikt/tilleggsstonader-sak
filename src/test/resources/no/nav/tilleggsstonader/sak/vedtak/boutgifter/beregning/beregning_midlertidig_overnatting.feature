# language: no
# encoding: UTF-8

Egenskap: Beregning av midlertidig overnatting

  Scenario: Vedtaksperiode inneholder utgift
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 07.01.2025 | 09.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 07.01.2025 | 09.01.2025 | 1              | 1000         | 4953      | 07.01.2025      | AAP       | TILTAK    |

  Scenario: Vedtaksperiode krysser nyttår
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 30.12.2024 | 02.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 30.12.2024 | 02.01.2025 | 8000   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 30.12.2024 | 02.01.2025 | 1              | 4809         | 4809      | 30.12.2024      | AAP       | TILTAK    |

  Scenario: Flere vedtaksperioder med ulik utbetalingsperiode
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 07.01.2025 | 09.01.2025 | TILTAK    | AAP       |
      | 12.03.2025 | 15.03.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 12.03.2025 | 15.03.2025 | 8000   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 07.01.2025 | 09.01.2025 | 1              | 1000         | 4953      | 07.01.2025      | AAP       | TILTAK    |
      | 12.03.2025 | 15.03.2025 | 1              | 4953         | 4953      | 12.03.2025      | AAP       | TILTAK    |


  Scenario: Flere vedtaksperioder med samme utbetalingsperiode
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 07.01.2025 | 09.01.2025 | TILTAK    | AAP       |
      | 21.01.2025 | 24.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 07.01.2025 | 09.01.2025 | 1000   |
      | 21.01.2025 | 24.01.2025 | 1000   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 07.01.2025 | 24.01.2025 | 1              | 2000         | 4953      | 07.01.2025      | AAP       | TILTAK    |

  Scenario: Vedtaksperiode kun i helg
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 11.01.2025 | 12.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 11.01.2025 | 12.01.2025 | 1500   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 11.01.2025 | 12.01.2025 | 1              | 1500         | 4953      | 11.01.2025      | AAP       | TILTAK    |

  Scenario: Vedtaksperiode krysser utbetalingsperiode
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 11.01.2025 | 12.01.2025 | TILTAK    | AAP       |
      | 09.02.2025 | 14.02.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 11.01.2025 | 12.01.2025 | 1500   |
      | 09.02.2025 | 14.02.2025 | 1500   |

    Så forvent følgende feil når vi beregner boutgifter: Vi støtter foreløpig ikke at utgifter krysser ulike utbetalingsperioder.

  Scenario: To påfølgende vedtaksperioder som dekker utgiften
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.01.2025 | 15.01.2025 | TILTAK    | AAP       |
      | 16.01.2025 | 31.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Så skal beregnet stønad for boutgifter være
      | Fom        | Tom        | Antall måneder | Stønadsbeløp | Maks sats | Utbetalingsdato | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.01.2025 | 1              | 1000         | 4953      | 01.01.2025      | AAP       | TILTAK    |

  Scenario: Vedtaksperiode som ikke dekker utgift
    Gitt følgende vedtaksperioder for boutgifter
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 01.01.2025 | 15.01.2025 | TILTAK    | AAP       |

    Gitt følgende utgifter for: UTGIFTER_OVERNATTING
      | Fom        | Tom        | Utgift |
      | 01.01.2025 | 31.01.2025 | 1000   |

    Så forvent følgende feil når vi beregner boutgifter: Du har lagt inn utgifter til midlertidig overnatting som ikke er inneholdt i en vedtaksperiode. Foreløpig støtter vi ikke dette.
