# language: no
# encoding: UTF-8

Egenskap: Beregning av rammevedtak for kjøring med privat bil daglig reise

  Scenario: en reise uten ekstrakostnader
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 06.01.2025 | 19.01.2025 | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 06.01.2025 | 19.01.2025 | 5                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 06.01.2025 | 19.01.2025 | 10           |

    Og vi forventer følgende delperioder for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet | Antall reisedager per uke |
      | 1       | 06.01.2025 | 19.01.2025 | 57.60                  | 2.88          | Ja             | 5                         |

  Scenario: to reiser med bompenger og fergekostnader
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2026 | 31.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        |  Reiseavstand |
      | 1        | 01.01.2026 | 18.01.2026 |  10           |
      | 2        | 11.01.2026 | 31.01.2026 | 8            |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Bompenger | Fergekostnad | Antall reisedager per uke |
      | 1        | 01.01.2026 | 18.01.2026 | 50        | 80           | 5                         |
      | 2        | 11.01.2026 | 31.01.2026 | 60        | 80           | 4                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.01.2026 | 18.01.2026 | 10           |
      | 2       | 11.01.2026 | 31.01.2026 | 8            |

    Og vi forventer følgende delperioder for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet | Antall reisedager per uke |
      | 1       | 01.01.2026 | 18.01.2026 | 188.80                 | 2.94          | Ja             | 5                          |
      | 2       | 11.01.2026 | 31.01.2026 | 187.04                  | 2.94          | Ja            | 4                         |

  Scenario: skal ikke få høyere sum dersom ekstrakostnader er 0 (ikke null)
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 06.01.2025 | 12.01.2025 | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Bompenger | Fergekostnad | Antall reisedager per uke |
      | 1       | 06.01.2025 | 12.01.2025 | 0         | 0            | 5                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 06.01.2025 | 12.01.2025 | 10                         |

    Og vi forventer følgende delperioder for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |Antall reisedager per uke |
      | 1       | 06.01.2025 | 12.01.2025 | 57.60                  | 2.88          | Ja             |                         5 |

  Scenario: støtter ulike men sammenhengende vedtaksperioder innenfor reise
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 16.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.01.2025 | 30.01.2025 | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.01.2025 | 30.01.2025 | 5                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.01.2025 | 30.01.2025 | 10           |

    Og vi forventer følgende vedtaksperioder for rammevedtak med reiseNr=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 16.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |


  Scenario: må ha sammenhengende vedtaksperioder innenfor en reise
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 17.01.2025 | 30.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 1       | 01.01.2025 | 30.01.2025 | 5                         | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.01.2025 | 30.01.2025 | 5                         |

    Når beregner for daglig reise privat bil

    Så forvent følgende feilmelding for beregning privat bil: Alle vedtaksperioder må være sammenhengende

  # Øk med nytt år når sats for nytt år legges inn
  Scenario: periode innvilget frem i tid hvor sats ikke er bekreftet blir markert med sats ubekreftet
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.12.2026 | 15.01.2027 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.12.2026 | 15.01.2027 | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.12.2026 | 15.01.2027 | 5                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.12.2026 | 15.01.2027 | 10           |

    Og vi forventer følgende delperioder for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet | Antall reisedager per uke |
      | 1       | 01.12.2026 | 31.12.2026 | 58.80                  | 2.94          | Ja             | 5                         |
      | 1       | 01.01.2027 | 15.01.2027 | 58.80                  | 2.94          | Nei            | 5                         |

  Scenario: periode med flere delperioder og ulike satser
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.12.2025 | 15.01.2027 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.12.2025 | 15.01.2027 | 10           |

    Gitt følgende delperioder for vilkår daglig reise med privat bil
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.12.2025 | 21.12.2025 | 5                         |
      | 1       | 22.12.2025 | 04.01.2026 | 2                         |
      | 1       | 05.01.2026 | 27.09.2026 | 5                         |
      | 1       | 28.09.2026 | 17.01.2027 | 2                         |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Reiseavstand |
      | 1       | 01.12.2025 | 15.01.2027 | 10           |

    Og vi forventer følgende delperioder for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet | Antall reisedager per uke |
      | 1       | 01.12.2025 | 21.12.2025 | 57.60                  | 2.88          | Ja             | 5                         |
      | 1       | 22.12.2025 | 31.12.2025 | 57.60                  | 2.88          | Ja             | 2                         |
      | 1       | 01.01.2026 | 04.01.2026 | 58.80                  | 2.94          | Ja             | 2                         |
      | 1       | 05.01.2026 | 27.09.2026 | 58.80                  | 2.94          | Ja             | 5                         |
      | 1       | 28.09.2026 | 31.12.2026 | 58.80                  | 2.94          | Ja             | 2                         |
      | 1       | 01.01.2027 | 17.01.2027 | 58.80                  | 2.94          | Nei            | 2                         |
