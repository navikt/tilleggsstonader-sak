# language: no
# encoding: UTF-8

Egenskap: Beregning av rammevedtak for kjøring med privat bil daglig reise

  Scenario: to fulle uker
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 06.01.2025 | 19.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 06.01.2025 | 19.01.2025 | 5                         | 10           |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 06.01.2025 | 19.01.2025 | 5                         |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |
      | 1       | 06.01.2025 | 19.01.2025 | 57.60                  | 2.88          | Ja           |

  Scenario: skal legge til ekstrakostnader i tillegg til kjøring
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Bompenger |  Fergekostnad |
      | 06.01.2025 | 12.01.2025 | 5                         | 10           | 100       |  100          |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 06.01.2025 | 12.01.2025 | 5                         |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |
      | 1       | 06.01.2025 | 12.01.2025 | 457.60                 | 2.88          | Ja           |

  Scenario: skal ikke få høyere sum dersom ekstrakostnader er 0
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 06.01.2025 | 12.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |
    
    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Bompenger | Fergekostnad |
      | 06.01.2025 | 12.01.2025 | 5                         | 10           | 0         | 0            |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 06.01.2025 | 12.01.2025 | 5                         |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |
      | 1       | 06.01.2025 | 12.01.2025 | 57.60                  | 2.88          | Ja             |

  Scenario: støtter ulike men sammenhengende vedtaksperioder innenfor reise
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |
      | 16.01.2025 | 30.02.2025 | NEDSATT_ARBEIDSEVNE       | UTDANNING    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Bompenger | Fergekostnad |
      | 01.01.2025 | 30.01.2025 | 5                         | 10           | 0         | 0            |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.01.2025 | 30.01.2025 | 5                         |

    Og vi forventer følgende vedtaksperioder for rammevedtak med reiseNr=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |
      | 16.01.2025 | 30.01.2025 | NEDSATT_ARBEIDSEVNE       | UTDANNING    |


  Scenario: må ha sammenhengende vedtaksperioder innenfor en reise
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 15.01.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |
      | 17.01.2025 | 30.02.2025 | NEDSATT_ARBEIDSEVNE       | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Bompenger | Fergekostnad |
      | 01.01.2025 | 30.01.2025 | 5                         | 10           | 0         | 0            |

    Når beregner for daglig reise privat bil

    Så forvent følgende feilmelding for beregning privat bil: Alle vedtaksperioder må være sammenhengende

  # Øk med nytt år når sats for nytt år legges inn
  Scenario: periode innvilget frem i tid hvor sats ikke er bekreftet blir markert med sats ubekreftet
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.12.2026 | 15.01.2027 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Bompenger | Fergekostnad |
      | 01.12.2026 | 15.01.2027 | 5                         | 10           | 0         | 0            |

    Når beregner for daglig reise privat bil

    Så forventer vi rammevedtak for følgende periode
      | Reisenr | Fom        | Tom        | Antall reisedager per uke |
      | 1       | 01.12.2026 | 15.01.2027 | 5                         |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |
      | 1       | 01.12.2026 | 31.12.2026 | 58.80                  | 2.94          | Ja             |
      | 1       | 01.01.2027 | 15.01.2027 | 58.80                  | 2.94          | Nei             |
