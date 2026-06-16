# language: no
# encoding: UTF-8

Egenskap: Utled tidligste endring av vilkår

  Scenario: Vilkår er forlenget
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.01.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.02.2024

  Scenario: Vilkår er forkortet
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 30.06.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.04.2024

  Scenario: Nytt vilkår lagt inn før eksisterende
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.02.2024 | 29.02.2024 | PASS_BARN | OPPFYLT  | NY      |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | UENDRET |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.02.2024

  Scenario: Nytt vilkår lagt inn etter eksisterende
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | UENDRET |
      | 01.04.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | NY      |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.04.2024

  Scenario: Endring i utgift
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status | Utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     | 1000   |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status | Utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET | 1500   |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

#    Endring i resultat tilsier at det har vært en endring i delvilkår som må tas hensyn til
  Scenario: Endring i resultat
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat     | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | IKKE_OPPFYLT | ENDRET |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Scenario: Utgift endrer seg fra å være fremtidig til å ikke være fremtidig
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status | Er fremtidig utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     | Ja                  |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat     | Status | Er fremtidig utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | IKKE_OPPFYLT | ENDRET | Nei                 |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Scenario: Vilkår blir slettet
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | SLETTET |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Scenario: Vilkår blir slettet og nytt senere vilkår lagt til
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | SLETTET |
      | 01.05.2024 | 31.05.2024 | PASS_BARN | OPPFYLT  | NY      |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Scenario: Vilkår slettet i tidligere behandling, tilsvarende vilkår lagt til i revurdering
    Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | SLETTET |
      | 01.04.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | NY      |

    Gitt følgende vilkår i revurdering - utledTidligsteEndring
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |
      | 01.04.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | NY     |

    Når utleder tidligste endring

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Regel: Endring på vilkår for ett barn skal kun sammenlignes med vilkår for det aktuelle barnet
    Scenario: Forlengelse av vilkår for ett barn
      Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Status |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 1      | NY     |
        | 01.03.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | 2      | NY     |

      Gitt følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Status  |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 1      | UENDRET |
        | 01.03.2024 | 31.05.2024 | PASS_BARN | OPPFYLT  | 2      | ENDRET  |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.05.2024

      # To barn ender i en revurdering med like perioder, men ulike utgifter.
    # Skal håndtere at endringen er en utvidelse og ikke en endring av fakta på annet barn
    Scenario: Forlengelse av vilkår for ett barn slik at periode blir likt som eksisterende barn
      Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Utgift | Status |
        | 01.03.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | 1      | 1500   | NY     |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 2      | 1000   | NY     |

      Gitt følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Utgift | Status  |
        | 01.03.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | 1      | 1500   | UENDRET |
        | 01.03.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | 2      | 1000   | ENDRET  |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.04.2024

    Scenario: Et barn slettes og samme periode legges inn for nytt barn
      # Feks at utgift ble feilregistrert på barn 1
      Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Utgift | Status |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 1      | 1000   | NY     |

      Gitt følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | BarnId | Utgift | Status  |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 2      | 1000   | NY      |
        | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | 1      | 1000   | SLETTET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.03.2024

  Regel: Endring på vilkår skal kun sjekkes opp mot andre vilkår av samme type
    # Usikker på hva som bør testes her...
    Scenario: Nytt vilkår av annen type er lagt inn etter eksisterende
      Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type                      | Resultat | Status |
        | 01.03.2024 | 31.03.2024 | LØPENDE_UTGIFTER_EN_BOLIG | OPPFYLT  | NY     |

      Gitt følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type                      | Resultat | Status  |
        | 01.03.2024 | 31.03.2024 | LØPENDE_UTGIFTER_EN_BOLIG | OPPFYLT  | UENDRET |
        | 03.03.2024 | 04.03.2024 | UTGIFTER_OVERNATTING      | OPPFYLT  | NY      |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 03.03.2024

    Scenario: Nytt vilkår av annen type er lagt inn etter eksisterende
      Gitt følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type                      | Resultat | Status |
        | 01.03.2024 | 31.03.2024 | LØPENDE_UTGIFTER_EN_BOLIG | OPPFYLT  | NY     |

      Gitt følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type                      | Resultat | Status  |
        | 24.02.2024 | 25.02.2024 | UTGIFTER_OVERNATTING      | OPPFYLT  | NY      |
        | 01.03.2024 | 31.03.2024 | LØPENDE_UTGIFTER_EN_BOLIG | OPPFYLT  | UENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 24.02.2024

    Scenario: Privat bil forkortes og delperiode forkortes
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 31.01.2024 | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 20.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 20.01.2024 | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 21.01.2024

    Scenario: Privat bil forkortes og delperiode forsvinner
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 5                         |
        | 1       | 16.01.2024 | 31.01.2024 | 3                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 15.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 16.01.2024

    Scenario: Privat bil utvides og delperiode utvides
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 20.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 20.01.2024 | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 31.01.2024 | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 21.01.2024

    Scenario: Privat bil utvides og ny delperiode kommer inn
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 15.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 5                         |
        | 1       | 16.01.2024 | 31.01.2024 | 3                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 16.01.2024

    Scenario: Privat bil endrer bompenger i delperiode uten endring i overordnet vilkårsperiode
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 10        | 5                         |
        | 1       | 16.01.2024 | 31.01.2024 | 0         | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 15.01.2024 | 50        | 5                         |
        | 1       | 16.01.2024 | 31.01.2024 | 0         | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.01.2024

    Scenario: Privat bil flytter grense mellom to delperioder fra en uke til to uker
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 21.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 07.01.2024 | 10        | 5                         |
        | 1       | 08.01.2024 | 21.01.2024 | 10        | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 21.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 14.01.2024 | 10        | 5                         |
        | 1       | 15.01.2024 | 21.01.2024 | 10        | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 08.01.2024

    Scenario: Privat bil flytter grense mellom to delperioder fra to uker til en uke
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 21.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 14.01.2024 | 10        | 5                         |
        | 1       | 15.01.2024 | 21.01.2024 | 10        | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 21.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 07.01.2024 | 10        | 5                         |
        | 1       | 08.01.2024 | 21.01.2024 | 10        | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 08.01.2024

    Scenario: Privat bil endrer bompenger i delperiode 1 og 3
      Gitt følgende vilkår for daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | NY     |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i forrige behandling - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 10.01.2024 | 10        | 5                         |
        | 1       | 11.01.2024 | 20.01.2024 | 20        | 5                         |
        | 1       | 21.01.2024 | 31.01.2024 | 30        | 5                         |

      Gitt følgende vilkår for daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Reiseavstand | Resultat | Status |
        | 1       | 01.01.2024 | 31.01.2024 | 10           | OPPFYLT  | ENDRET |

      Gitt følgende delperioder for vilkår daglig reise med privat bil i revurdering - utledTidligsteEndring
        | Reisenr | Fom        | Tom        | Bompenger | Antall reisedager per uke |
        | 1       | 01.01.2024 | 10.01.2024 | 50        | 5                         |
        | 1       | 11.01.2024 | 20.01.2024 | 20        | 5                         |
        | 1       | 21.01.2024 | 31.01.2024 | 60        | 5                         |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.01.2024
