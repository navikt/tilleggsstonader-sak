# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av læremidler - revurdering

  Scenario: Legger til ny vedtaksperiode(feb) etter forrige vedtaksperiode(jan)
  Revurderer fra etter forrige periode.
  Resultat: Blir en ny utbetalingsperiode, som utbetales i feb

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 28.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 03.02.2025      | Nei                         |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |
      | 03.02.2025 | 451   | LÆREMIDLER_AAP | 03.02.2025      |

  Scenario: Jan-mars. Endrer 100 til 50% fra februar.
  Resultat: Beholder utbetalingsdato for februar og mars til januar, då det var då den opprinnelige utbetalingen ble utført

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1353  | LÆREMIDLER_AAP | 01.01.2025      |

    # Endrer % fra og med februar
    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.02.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 50            |

    Gitt følgende målgrupper for læremidler behandling=2
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.03.2025 | 31.03.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 903   | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Legger til en ny vedtaksperiode innenfor en tidligere løpende måned
  Har 01jan-10feb, legger til vedtaksperiode 20feb-31mars
  Resultat: perioden i januar påvirker ikke noe, og den nye løpende måneden for mars blir utbetalt i mars

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=20.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      # Ny vedtaksperiode
      | 20.02.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 03.03.2025      | Nei                         |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 902   | LÆREMIDLER_AAP | 01.01.2025      |
      | 03.03.2025 | 451   | LÆREMIDLER_AAP | 03.03.2025      |

  Scenario: Endrer målgruppe for februar som tidligere er utbetalt i januar, beholder utbetalingsdatoet

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 28.02.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=2
      | Fom        | Tom        | Målgruppe       |
      | 01.12.2024 | 31.01.2025 | AAP             |
      | 01.02.2024 | 31.03.2025 | OVERGANGSSTØNAD |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.02.2025 | 28.02.2025 | ENSLIG_FORSØRGER    | UTDANNING |

    # Endrer målgruppe fra februar
    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Aktivitet | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | UTDANNING | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | ENSLIG_FORSØRGER    | UTDANNING | 01.01.2025      | Ja                          |

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

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 20.12.2024 | 31.12.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    # Legger til ny vedtaksperiode etter nyttår
    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 20.12.2024 | 31.12.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 20.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 20.12.2024      | Ja                          |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Nei                         |

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

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 20.12.2024 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    # Endrer prosent fra 1 januar
    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.12.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 50            |

    Gitt følgende målgrupper for læremidler behandling=2
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 20.12.2024 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 20.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 20.12.2024      | Ja                          |
      | 01.01.2025 | 31.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 20.12.2024 | 438   | LÆREMIDLER_AAP | 20.12.2024      |
      | 01.01.2025 | 226   | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Avkorter og setter ny vedtaksperiode
  Beholder utbetalingsdato for de vedtaksperioder som allerede er vurder fra tidligere
  Dette burde egentlige ikke gjøres av saksbehandler

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.12.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 50            |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.12.2024 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.02.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.03.2025 | 31.03.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 678   | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Verifiserer at beregningsresultat før revurder ikke blir reberegnet

    Gitt lagrer andeler behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 300   | LÆREMIDLER_AAP | 01.01.2025      |

    # Legger inn 100 i beløp for å verifisere at perioder før revurder ikke blir reberegnet
    Gitt lagrer beregningsresultatet behandling=1
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.03.2025 | 31.03.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 50            |
      | 01.02.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=2
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=01.02.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1002  | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Verifiserer at periode som løper i revurder fra blir reberegnet

    Gitt lagrer andeler behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 300   | LÆREMIDLER_AAP | 01.01.2025      |

    # Legger inn 100 i beløp for å verifisere at perioder før revurder ikke blir reberegnet
    Gitt lagrer beregningsresultatet behandling=1
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.02.2025 | 28.02.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.03.2025 | 31.03.2025 | 100   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Gitt følgende aktiviteter for læremidler behandling=2
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 50            |
      | 01.02.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=2
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=31.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 01.01.2025 | 31.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.02.2025 | 28.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |
      | 01.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1128  | LÆREMIDLER_AAP | 01.01.2025      |

  Scenario: Hvis man forlenger vedtaksperiode før tidligere vedtaksperiode skal man beholde utbetalingsdatoet
    # Hvis det fortsatt blir en løpende måned i den måned man tidligere har innvilget, beholder utbetalingsdatoet for den måneden

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 10.02.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=06.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 06.01.2025 | 05.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 06.01.2025      | Nei                         |
      # utbetalingsdatoet er fortsatt 10.02 då det var datoet det var då perioden ble utbetalt i førstegangsbehandlingen
      | 06.02.2025 | 10.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 10.02.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 06.01.2025 | 451   | LÆREMIDLER_AAP | 06.01.2025      |
      | 10.02.2025 | 451   | LÆREMIDLER_AAP | 10.02.2025      |

  Scenario: Hvis man legger inn en vedtaksperiode før tidligere vedtaksperiode skal man beholde utbetalingsdatoet

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 10.02.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=06.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.02.2025 | 09.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 10.02.2025 | 10.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      # utbetalingsdatoet er fortsatt 10.02 då det var datoet det var då perioden ble utbetalt i førstegangsbehandlingen
      | 01.02.2025 | 10.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 10.02.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 10.02.2025 | 451   | LÆREMIDLER_AAP | 10.02.2025      |

  Scenario: Hvis man legger inn en periode som gjør at løpende måned for akkurat den tidligere utbetalte måneden endrer seg
  vil man endre utbetalingsdatoet då det er vanskelig å fortsatt beholde det i riktig måned

    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 10.02.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når innvilger revurdering med vedtaksperioder for behandling=2 med revurderFra=06.01.2025
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 20.01.2025 | 09.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 10.02.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato | Del av tidligere utbetaling |
      | 20.01.2025 | 19.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 20.01.2025      | Nei                         |
      # løpende måneden som begynner i februar endrer fra og med dato då den påvirkes av løpende måneden som begynner i januar og forskyves
      | 20.02.2025 | 19.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 10.02.2025      | Ja                          |
      | 20.03.2025 | 31.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 10.02.2025      | Ja                          |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 20.01.2025 | 451   | LÆREMIDLER_AAP | 20.01.2025      |
      | 10.02.2025 | 902   | LÆREMIDLER_AAP | 10.02.2025      |
