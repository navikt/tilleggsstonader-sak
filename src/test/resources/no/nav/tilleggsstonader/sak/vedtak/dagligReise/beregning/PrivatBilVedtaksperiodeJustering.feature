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

    Så forventer vi rammevedtak for følgende periode
    | Reisenr | Fom        | Tom        | Antall reisedager per uke |
    | 1       | 12.01.2026 | 15.01.2026 | 5                         |

    Og vi forventer følgende innvilgede perioder i rammevedtaket
    | 1       | 12.01.2026 | 15.01.2026 |

    Scenario: reisen er kortere enn vedtaksperioden
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2026 | 31.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 12.01.2026 | 15.01.2026 | 5                         | 10           |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 12.01.2026 | 15.01.2026 | 5                         |

    Og vi forventer følgende innvilgede perioder i rammevedtaket
      | 1       | 12.01.2026 | 15.01.2026 |

    Scenario: 2 vedtaksperioder hvor oppholdet gjør at hele uker ikke dekkes av minst én vedtaksperiode
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 19.01.2026 | 25.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
        | 01.01.2026 | 31.01.2026 | 5                         | 10           |

      Når beregner for daglig reise privat bil

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 05.01.2026 | 25.01.2026 | 5                         |

      Og vi forventer følgende innvilgede perioder i rammevedtaket
        | Reisenr | Fom        | Tom        |
        | 1       | 05.01.2026 | 11.01.2026 |
        | 1       | 19.01.2026 | 25.01.2026 |

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

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 05.01.2026 | 11.01.2026 | 5                         |

      Og vi forventer følgende innvilgede perioder i rammevedtaket
        | Reisenr | Fom        | Tom        |
        | 1       | 05.01.2026 | 07.01.2026 |
        | 1       | 08.01.2026 | 11.01.2026 |

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

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 05.01.2026 | 31.01.2026 | 5                         |

      Og vi forventer følgende innvilgede perioder i rammevedtaket
        | Reisenr | Fom        | Tom        |
        | 1       | 05.01.2026 | 07.01.2026 |
        | 1       | 12.01.2026 | 31.01.2026 |
