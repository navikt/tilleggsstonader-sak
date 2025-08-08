# language: no
# encoding: UTF-8

Egenskap: Skal foreslå vedtaksperioder fra og med minste datoet av første endringen etter tidligere vedtaksperioder

  Regel:
  Man har tidligere vilkårsperioder til sluttet på året men kun har innvilget i juni.
  Etter sommeren forlenger man vedtaket, uten å gjøre endringer i vilkårsperioder, eller gjør endringer i vilkårsperioder frem i tiden.
  Skal då forlenge vedtaksperioden fra og med første datoet etter siste forrige vedtaksperiode.

    Bakgrunn:

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.12.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.12.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 31.12.2023 | OPPFYLT  |

      Gitt følgende tidligere vedtaksperioder for vedtaksforslag
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Scenario: Hvis man ikke har endret noe skal man fylle på etter siste forrige vedtaksperiode

      Når forslag til vedtaksperioder behold id lages

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Scenario: Første endringen er en gang under høsten, skal legge til vedtaksperiode fra og med siste vedtaksperiode

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=28.10.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Scenario: Første endringen er en gang under våren, legge til vedtaksperiode etter siste vedtaksperioden

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=28.02.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      # Boundary tests
    Scenario: Første endringen er TOM på siste vedtaksperiode i forrige behandling, skal forlenge

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=30.06.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      # Boundary tests
    Scenario: Første endringen er dagen etter TOM på siste vedtaksperiode i forrige behandling

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=01.07.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

        # Boundary tests
    Scenario: Første endringen er 2 dager etter TOM på siste vedtaksperiode i forrige behandling, legger til en periode etter siste vedtaksperiode

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=02.07.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Scenario: Skal ikke opprette perioder mellom 2 tidligere perioder hvis tidligst endring er etter siste vedtaksperiode

      # overskriver bakgrunn
      Gitt følgende tidligere vedtaksperioder for vedtaksforslag
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 2  | 01.06.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=01.10.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 2  | 01.06.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Scenario: Skal opprette perioder mellom 2 tidligere perioder hvis tidligst endring er mellom tidligere vedtaksperioder

      # overskriver bakgrunn
      Gitt følgende tidligere vedtaksperioder for vedtaksforslag
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 2  | 01.06.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når forslag til vedtaksperioder behold id lages tidligsteEndring=01.05.2023

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.05.2023 | 31.05.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 2  | 01.06.2023 | 30.06.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | -1 | 01.07.2023 | 31.12.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
