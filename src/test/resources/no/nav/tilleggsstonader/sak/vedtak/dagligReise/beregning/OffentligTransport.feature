# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport daglig reise

  Scenario: Test at oppsett funker

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 31.01.2025 | 44    |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Beløp |
      | 1320  |