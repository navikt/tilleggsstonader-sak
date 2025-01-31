# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av læremidler - revurdering

  Scenario: Legger til ny vedtaksperiode(feb) etter forrige vedtaksperiode(jan)
  Revurderer fra etter forrige periode.
  Resultat: Blir en ny utbetalingsperiode, som utbetales i feb

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 31.01.2025 |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        |
      | 01.01.2025 | 31.01.2025 |
      | 01.02.2025 | 28.02.2025 |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 03.02.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |
      | 01.02.2025 | 451   | LÆREMIDLER_AAP | 03.02.2025      |

  Scenario: Jan-mars. Endrer 100 til 50% fra februar.
  Resultat: Beholder utbetalingsdato for februar og mars til januar, då det var då den opprinnelige utbetalingen ble utført

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 31.03.2025 |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1353  | LÆREMIDLER_AAP | 01.01.2025      |

    # Endrer % fra og med februar
    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.02.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 50            |

    Gitt følgende stønadsperioder for læremidler behandling=2
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        |
      | 01.01.2025 | 31.03.2025 |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 226   | VIDEREGÅENDE | 50            | 451  | AAP       | 01.01.2025      |
      | 01.03.2025 | 31.03.2025 | 226   | VIDEREGÅENDE | 50            | 451  | AAP       | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 903   | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Legger til en ny vedtaksperiode innenfor en tidligere løpende måned
    Har 01jan-10feb, legger til vedtaksperiode 20feb-31mars
    Resultat: perioden i januar påvirker ikke noe, og den nye løpende måneden for mars blir utbetalt i mars

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 10.02.2025 |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=20.02.2025
      | Fom        | Tom        |
      | 01.01.2025 | 10.02.2025 |
      # Ny vedtaksperiode
      | 20.02.2025 | 31.03.2025 |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |
      | 01.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 03.03.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 902   | LÆREMIDLER_AAP | 01.01.2025      |
      | 03.03.2025 | 451   | LÆREMIDLER_AAP | 03.03.2025      |

  Scenario: Endrer målgruppe for februar som tidligere er utbetalt i januar, beholder utbetalingsdatoet

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | UTDANNING |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 28.02.2025 |

    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=2
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 01.12.2024 | 31.01.2025 | AAP             | UTDANNING |
      | 01.02.2024 | 31.03.2025 | OVERGANGSSTØNAD | UTDANNING |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        |
      | 01.01.2025 | 28.02.2025 |

    # Endrer målgruppe fra februar
    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe       | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP             | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | OVERGANGSSTØNAD | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type                        | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP              | 01.01.2025      |
      | 01.01.2025 | 451   | LÆREMIDLER_ENSLIG_FORSØRGER | 01.01.2025      |

  Scenario: Tidligere innvilget til 31 des. Revurderer fra 1 januar.
  Januar er en del av forrige løpende måned, men er likevel et nytt år og skal håndteres adskilt fra forrige år.
  Blir nytt utbetalingsdato.

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 20.12.2024 | 31.12.2024 |

    Når kopierer perioder fra forrige behandling for behandling=2

    # Legger til ny vedtaksperiode etter nyttår
    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.01.2025
      | Fom        | Tom        |
      | 20.12.2024 | 31.12.2024 |
      | 01.01.2025 | 31.01.2025 |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 20.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 20.12.2024      |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 20.12.2024 | 438   | LÆREMIDLER_AAP | 20.12.2024      |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Vedtaksperiode går over nyttår, revurderer fra 1 januar og endrer %
  Januar er en del av forrige løpende måned, men er likevel et nytt år og skal håndteres adskilt fra forrige år.
  Blir nytt utbetalingsdato.

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 20.12.2024 | 31.01.2025 |

    # Endrer prosent fra 1 januar
    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.12.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 50            |

    Gitt følgende stønadsperioder for læremidler behandling=2
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.01.2025
      | Fom        | Tom        |
      | 20.12.2024 | 31.01.2025 |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 20.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 20.12.2024      |
      | 01.01.2025 | 31.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | AAP       | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 20.12.2024 | 438   | LÆREMIDLER_AAP | 20.12.2024      |
      | 01.01.2025 | 226   | LÆREMIDLER_AAP | 01.01.2025      |