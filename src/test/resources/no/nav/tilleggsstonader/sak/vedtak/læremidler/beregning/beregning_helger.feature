# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - helg

  Scenario: Vedtaksperiode er kun over helg
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.02.2025 | 02.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.03.2025 | AAP       | TILTAK    |

    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom | Tom | Beløp | Studienivå | Studieprosent | Sats | Målgruppe | Utbetalingsdato |

  Scenario: Vedtaksperioder er kun over helger
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.02.2025 | 02.02.2025 |
      | 08.02.2025 | 09.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.03.2025 | AAP       | TILTAK    |

    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom | Tom | Beløp | Studienivå | Studieprosent | Sats | Målgruppe | Utbetalingsdato |

  Scenario: Vedtaksperioder er over 2 helger og 1 ukesdag, men løpende måneden begynner første helgen
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.02.2025 | 02.02.2025 |
      | 08.02.2025 | 09.02.2025 |
      | 11.02.2025 | 11.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.03.2025 | AAP       | TILTAK    |

    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.02.2025 | 11.02.2025 | 901   | HØYERE_UTDANNING | 100           | 901  | AAP       | 03.02.2025      |
