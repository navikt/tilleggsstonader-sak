# language: no
# encoding: UTF-8

Egenskap: Beregning av rammevedtak for kjøring med privat bil daglig reise

  Scenario: to fulle uker
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering |
      | 06.01.2025 | 19.01.2025 | 5                         | 10           | 50        |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 06.01.2025 | 12.01.2025 | 5                       | 538   | Nei             |
      | 1       | 13.01.2025 | 19.01.2025 | 5                       | 538   | Nei             |

  Scenario: halve uke inkluderer helg i beregningen
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering |
      | 01.01.2025 | 05.01.2025 | 5                         | 10           | 50        |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 01.01.2025 | 05.01.2025 | 5                       | 538   | Ja              |

  Scenario: får mindre dekt om uke er kortere enn antall dager man skal reise
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering |
      | 24.03.2025 | 31.03.2025 | 3                         | 10           | 50        |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 24.03.2025 | 30.03.2025 | 3                       | 323   | Nei             |
      | 1       | 31.03.2025 | 31.03.2025 | 1                       | 108   | Nei             |

  Scenario: får mindre dekt om uke er kortere enn antall dager man skal reise (inkludert helg)
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering |
      | 01.05.2025 | 11.05.2025 | 5                         | 10           | 50        |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 01.05.2025 | 04.05.2025 | 4                       | 430   | Ja              |
      | 1       | 05.05.2025 | 11.05.2025 | 5                       | 538   | Nei             |

  Scenario: skal legge til ekstrakostnader i tillegg til kjøring
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering | Bompenger | Piggdekkavgift | Fergekostnad |
      | 06.01.2025 | 12.01.2025 | 5                         | 10           | 50        | 100       | 100            | 100          |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 06.01.2025 | 12.01.2025 | 5                       | 1038  | Nei             |

  Scenario: skal ikke få høyere sum dersom ekstrakostnader er 0
    Gitt følgende dummyperioder for daglig reise privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand | Parkering | Bompenger | Piggdekkavgift | Fergekostnad |
      | 06.01.2025 | 12.01.2025 | 5                         | 10           | 0         | 0         | 0              | 0            |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 06.01.2025 | 12.01.2025 | 5                       | 288   | Nei             |
