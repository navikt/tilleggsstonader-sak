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

    Og vi forventer følgende innvilgede perioder i rammevedtaket
      | Reisenr | Fom        | Tom        |
      | 1       | 06.01.2025 | 19.01.2025 |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats |
      | 1       | 06.01.2025 | 19.01.2025 | 57.60                  | 2.88          |

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

    Og vi forventer følgende innvilgede perioder i rammevedtaket
      | Reisenr | Fom        | Tom        |
      | 1       | 06.01.2025 | 12.01.2025 |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats |
      | 1       | 06.01.2025 | 12.01.2025 | 457.60                 | 2.88          |

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

    Og vi forventer følgende innvilgede perioder i rammevedtaket
      | Reisenr | Fom        | Tom        |
      | 1       | 06.01.2025 | 12.01.2025 |

    Og vi forventer følgende satser for rammevedtak
      | Reisenr | Fom        | Tom        | Dagsats uten parkering | Kilometersats |
      | 1       | 06.01.2025 | 12.01.2025 | 57.60                  | 2.88          |
