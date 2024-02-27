# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av stønadsperioder

  Scenario: Sender inn tomme stønadsperioder

    Gitt følgende støndsperioder
      | Fom | Tom |

    Når beregner

    Så forvent følgende feil: Stønadsperioder mangler