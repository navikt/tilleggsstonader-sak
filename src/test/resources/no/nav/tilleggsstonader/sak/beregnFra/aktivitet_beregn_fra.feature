# language: no
# encoding: UTF-8

Egenskap: Utled tidligste endring av aktivitet

  Regel: Endring av sluttdato skal gi tidligste endring som tidligste av ny og gammel fom

    Scenario: Aktivitet avslutter senere
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | ENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.02.2024

    Scenario: Aktivitet avslutter før
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | ENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.04.2024

  Regel: Endring av startdato skal gi tidligste endring som tidligste av ny og gammel tom
    Scenario: Aktivitet starter før
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 08.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | ENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 08.01.2024

    Scenario: Aktivitet starter senere
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 08.01.2024 | 30.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | ENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 08.01.2024

  Regel: Nye aktiviteter skal gi tidligste endring startdato på ny periode
    Scenario: Ny aktivitet lagt inn før eksisterende
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 12.05.2024 | 28.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY      |
        | 12.05.2024 | 28.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Ny aktivitet lagt inn etter eksisterende
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |
        | 12.05.2024 | 28.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY      |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 12.05.2024

    Scenario: Ny aktivitet lagt inn med samme startdato som eksisterende
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | UENDRET |
        | 14.03.2024 | 28.06.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY      |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

  Regel: Endring i fakta og vurderinger skal gi tidligste endring dato lik fom på periode
    Scenario: Endring i aktivitetsdager
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 4               | OPPFYLT  | ENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Endring i prosent deltakelse
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Studieprosent | Studienivå       | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | LÆREMIDLER  | TILTAK | 100           | HØYERE_UTDANNING | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Studieprosent | Studienivå       | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | LÆREMIDLER  | TILTAK | 49            | HØYERE_UTDANNING | OPPFYLT  | ENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Endring i studienivå
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Studieprosent | Studienivå       | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | LÆREMIDLER  | TILTAK | 100           | HØYERE_UTDANNING | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Studieprosent | Studienivå   | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | LÆREMIDLER  | TILTAK | 100           | VIDEREGÅENDE | OPPFYLT  | ENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Endring i resultat
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat     | Status |
        | 14.03.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5               | IKKE_OPPFYLT | ENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 14.03.2024

  Regel: Sletting av aktiviteter skal gi tidligste endring lik fom på slettet periode
    Scenario: Aktivitet blir slettet
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | SLETTET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Aktivitet blir slettet og ny aktivitet blir lagt til
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | SLETTET |
        | 12.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Aktivitet ble slettet i forrige behandling og tilsvarende blir lagt til i revurdering
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | SLETTET |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | UENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Aktivitet ble slettet i forrige behandling og tilsvarende blir lagt til i revurdering
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | SLETTET |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | UENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Aktivitet får ny fom før det blir slettet
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 15.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | UENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 12.03.2024

    Scenario: Aktivitet får ny tom før det blir slettet
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 12.03.2024 | 30.06.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY      |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 12.03.2024 | 14.05.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | SLETTET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 12.03.2024

  Regel: Skal håndtere endring i aktiviteter med samme fom og tom
    Scenario: En aktivitet blir utvidet
      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | UTDANNING | OPPFYLT  | NY     |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Resultat | Status  |
        | 04.02.2024 | 31.04.2024 | BOUTGIFTER  | UTDANNING | OPPFYLT  | ENDRET |
        | 04.02.2024 | 31.03.2024 | BOUTGIFTER  | TILTAK | OPPFYLT  | UENDRET |


      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.04.2024


#    Burde man ha noe om at kildeId er endret? Feks. samme periode men ny aktivitet?