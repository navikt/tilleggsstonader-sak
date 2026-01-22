# language: no
# encoding: UTF-8

Egenskap: Beregning av privat bil og vedtaksperioder
  Regel: Det må finnes både en vedtaksperiode og en reise i samme periode for at det skal bli et beregningsresultat

    Scenario: vedtaksperioden er kortere enn reisen
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 12.01.2026 | 15.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 01.01.2026 | 31.01.2026 | 5                         | 10           |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 12.01.2026 | 15.01.2026 | 4                       | 235   | Nei             |

  Scenario: reisen er kortere enn vedtaksperioden
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2026 | 31.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 12.01.2026 | 15.01.2026 | 5                         | 10           |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 12.01.2026 | 15.01.2026 | 4                       | 235   | Nei             |

    Scenario: 2 vedtaksperioder hvor oppholdet gjør at en uke ikke dekkes at all av en vedtaksperiode
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 19.01.2026 | 25.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forventer vi følgende beregningsrsultat for daglig reise privatBil
        | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
        | 1       | 05.01.2026 | 11.01.2026 | 5                       | 294   | Nei             |
        | 1       | 19.01.2026 | 25.01.2026 | 5                       | 294   | Nei             |

    Scenario: Vedtaksperiode og reise overlapper ikke
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 12.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forvent at det ikke finnes et beregninsresultat for privat bil


  Regel: En uke kan kun ha en relevant vedtaksperiode for å beregnes

    Scenario: 2 vedtaksperioder som kan slås sammen for å dekke hele uka
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 07.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 08.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forventer vi følgende beregningsrsultat for daglig reise privatBil
        | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
        | 1       | 05.01.2026 | 11.01.2026 | 5                       | 294   | Nei             |

    Scenario: En uke som kun dekkes delvis pga. splitt i vedtaksperioder
      ## Det er nødvendig å ha to vedtaksperioder slik at selve reisen ikke blir kuttet før den splittes til uker
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 07.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 12.01.2026 | 31.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forventer vi følgende beregningsrsultat for daglig reise privatBil
        | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
        | 1       | 05.01.2026 | 07.01.2026 | 3                       | 176   | Nei             |
        | 1       | 12.01.2026 | 18.01.2026 | 5                       | 294   | Nei             |
        | 1       | 19.01.2026 | 25.01.2026 | 5                       | 294   | Nei             |
        | 1       | 26.01.2026 | 31.01.2026 | 5                       | 294   | Nei             |

    Scenario: Opphold mellom to vedtaksperioder innenfor samme uke
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 07.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 09.01.2026 | 18.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forvent følgende feilmelding for beregning privat bil: opphold mellom to vedtaksperioder

  Regel: Vedtaksperioder må ha samme målgruppe og typeAktivitet innenfor en uke
    Scenario: ulike målgrupper
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 07.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 08.01.2026 | 18.01.2026 | ENSLIG_FORSØRGER    | UTDANNING |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forvent følgende feilmelding for beregning privat bil: flere ulike målgrupper

    Scenario: ulike tiltakstyper
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe    | Aktivitet | Type aktivitet |
        | 05.01.2026 | 07.01.2026 | ARBEIDSSØKER | TILTAK    | AAPLOK         |
        | 08.01.2026 | 18.01.2026 | ARBEIDSSØKER | TILTAK    | ABIST          |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forvent følgende feilmelding for beregning privat bil: flere ulike aktivitetstyper innenfor samme uke
