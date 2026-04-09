# language: no
# encoding: UTF-8

Egenskap: Beregning av privat bil og vedtaksperioder
  Regel: Det må finnes både en vedtaksperiode og en reise i samme periode for at det skal bli et beregningsresultat

    Scenario: vedtaksperioden er kortere enn reisen
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 12.01.2026 | 15.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 01.01.2026 | 31.01.2026 | 10           |

      Gitt følgende delperioder for vilkår daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2026 | 31.01.2026 | 5                         |

      Når beregner for daglig reise privat bil

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 12.01.2026 | 15.01.2026 | 10           |

      Og vi forventer følgende delperioder for rammevedtak
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 12.01.2026 | 15.01.2026 | 5                         |

      Og vi forventer følgende satser for delperioder
        | Reisenr | DelperiodeNr | Fom        | Tom        | Dagsats uten parkering | Kilometersats | Sats bekreftet |
        | 1       | 1            | 12.01.2026 | 15.01.2026 | 58.80                  | 2.94          | Ja             |

    Scenario: reisen er kortere enn vedtaksperioden
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 01.01.2026 | 31.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 12.01.2026 | 15.01.2026 | 10           |

      Gitt følgende delperioder for vilkår daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 12.01.2026 | 15.01.2026 | 5                         |

      Når beregner for daglig reise privat bil

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 12.01.2026 | 15.01.2026 | 10           |

    Scenario: Vedtaksperiode og reise overlapper ikke
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 12.01.2026 | 31.01.2026 | 10           |

      Gitt følgende delperioder for vilkår daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 12.01.2025 | 31.01.2025 | 5                         |

      Når beregner for daglig reise privat bil

      Så forvent at det ikke finnes et beregninsresultat for privat bil


  Regel: En uke kan kun ha en relevant vedtaksperiode for å beregnes

    Scenario: 2 vedtaksperioder som kan slås sammen for å dekke hele uka
      Gitt følgende vedtaksperioder for daglig reise privat bil
        | Fom        | Tom        | Målgruppe           | Aktivitet |
        | 05.01.2026 | 07.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |
        | 08.01.2026 | 11.01.2026 | NEDSATT_ARBEIDSEVNE | TILTAK    |

      Gitt følgende vilkår for daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 01.01.2026 | 31.01.2026 | 10           |

      Gitt følgende delperioder for vilkår daglig reise med privat bil
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2026 | 31.01.2026 | 5                         |

      Når beregner for daglig reise privat bil

      Så forventer vi rammevedtak for følgende periode
        | Reisenr | Fom        | Tom        | Reiseavstand |
        | 1       | 05.01.2026 | 11.01.2026 | 10           |
