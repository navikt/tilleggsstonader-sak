# language: no
# encoding: UTF-8

Egenskap: Beregning av rammevedtak begrenset av vedtaksperiode

  Scenario: en vedtaksperiode som er kortere enn reisen
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

  Scenario: splitt i vedtaksperioden havner midt i uke og starter ikke på en mandag
    Gitt følgende vedtaksperioder for daglig reise privat bil
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2026 | 13.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 15.01.2026 | 22.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende vilkår for daglig reise med privat bil
      | Fom        | Tom        | Antall reisedager per uke | Reiseavstand |
      | 01.01.2026 | 31.01.2026 | 5                         | 10           |

    Når beregner for daglig reise privat bil

    Så forventer vi følgende beregningsrsultat for daglig reise privatBil
      | Reisenr | Fom        | Tom        | Antall dager dekt i uke | Beløp | Inkluderer helg |
      | 1       | 06.01.2026 | 11.01.2026 | 5                       | 294   | Ja              |
      | 1       | 12.01.2026 | 18.01.2026 | 5                       | 294   | Ja              |
      | 1       | 19.01.2026 | 22.01.2026 | 4                       | 235   | Nei             |
