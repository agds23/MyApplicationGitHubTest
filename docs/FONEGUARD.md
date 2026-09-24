# FoneGuard

**FoneGuard** é o nome provisório do aplicativo. O objetivo é evoluir o projeto Android de teste para um app útil de gerenciamento de chamadas e, posteriormente, encaminhamento controlado de mensagens.

O foco imediato é o módulo de chamadas. A infraestrutura Android/GitHub/CI já está validada e não deve ser reconstruída sem necessidade concreta.

## Ideia geral

A visão atual combina dois módulos:

1. **gerenciamento de chamadas recebidas por políticas configuráveis**;
2. **encaminhamento controlado de SMS/MMS recebidos**, em etapa posterior.

Aplicativos como Calls Blacklist e SMS Forwarder são referências de produto e comportamento, não especificações obrigatórias e nem fontes de código a copiar.

## Princípios do produto

- processamento local para decisões de chamadas;
- decisões explicáveis ao usuário;
- arquitetura preparada para crescer sem reescrever o motor a cada novo recurso;
- desenvolvimento incremental: capacidade por capacidade, com testes entre etapas;
- não exigir Google Contatos nem aplicativo de contatos específico;
- usar os contatos expostos pelo Android através do Contacts Provider;
- grupos e políticas importantes para o FoneGuard são estruturas próprias do FoneGuard;
- permitir números digitados manualmente além dos contatos do aparelho;
- evitar backend próprio enquanto não houver necessidade real;
- não criar encaminhamento oculto de mensagens;
- considerar limitações reais do Android e políticas da Google Play desde cedo;
- não colocar números, contatos, mensagens ou outros dados pessoais reais no repositório público.

## Estado atual

Ainda **não foi implementada funcionalidade de telefonia ou SMS**.

A próxima etapa é implementar o primeiro marco funcional do módulo de chamadas a partir do modelo definido neste documento.

# Módulo 1 — Gerenciamento de chamadas

## Papel inicial no Android

A primeira arquitetura deve usar a API oficial `CallScreeningService`, solicitando ao usuário o papel:

```text
RoleManager.ROLE_CALL_SCREENING
```

O serviço precisa responder rapidamente à chamada recebida, portanto a avaliação das políticas deve ser local e simples em tempo de execução.

Fonte oficial:

https://developer.android.com/reference/android/telecom/CallScreeningService

### ROLE_DIALER — investigação paralela

O FoneGuard **não deve assumir `ROLE_DIALER` na primeira implementação**.

Essa possibilidade fica registrada como investigação paralela para determinar se vale a pena o FoneGuard tornar-se também o aplicativo de telefone padrão, principalmente para:

- avaliar maior controle sobre chamadas privadas/sem identificação;
- entender precedência em relação a recursos nativos de fabricantes;
- verificar o comportamento de chamadas de saída quando outro aplicativo de telefone deixa de ser o padrão;
- medir o mínimo de UI e integração exigidos por `InCallService` e `ACTION_DIAL`.

A adoção futura de `ROLE_DIALER` não deve exigir reconstrução do motor de políticas. O motor deve permanecer independente da camada Android que entrega a chamada.

Fontes oficiais:

https://developer.android.com/reference/android/telecom/InCallService
https://developer.android.com/reference/android/app/role/RoleManager

## Requisitos para entrar em funcionamento

O FoneGuard deve possuir uma etapa inicial de preparação. Sempre que o Android permitir verificar programaticamente um requisito, o fluxo desejado é:

```text
verificar estado
  -> requisito atendido: continuar
  -> requisito não atendido:
       explicar ao usuário
       abrir a configuração apropriada
       usuário altera manualmente
       verificar novamente
```

Requisitos planejados para o módulo completo de chamadas:

- `ROLE_CALL_SCREENING`;
- `READ_CONTACTS`;
- localização precisa quando políticas por localização estiverem habilitadas;
- localização em segundo plano quando políticas por localização precisarem funcionar com o app fora de primeiro plano;
- notificações necessárias ao funcionamento/feedback do produto;
- estados de bateria e dados em segundo plano que sejam verificáveis e relevantes ao comportamento confiável.

O FoneGuard não deve alterar silenciosamente configurações do sistema. Deve detectar, explicar, encaminhar o usuário para a tela correta e verificar novamente quando possível.

## Contatos

### Fonte dos contatos

O FoneGuard deve ler os contatos disponibilizados pelo **Contacts Provider do Android** mediante `READ_CONTACTS`.

Não exigir:

- Google Contatos;
- Samsung Contatos;
- aplicativo específico de fabricante;
- que o FoneGuard se torne um aplicativo de gerenciamento de contatos.

Contatos armazenados em um aplicativo externo que não os disponibilize ao Contacts Provider podem ficar fora do escopo.

Fonte oficial:

https://developer.android.com/identity/providers/contacts-provider

### Identidade e números

O motor não deve usar a string digitada/exibida como identidade simples do telefone.

Um mesmo número pode chegar ou ser cadastrado em formatos diferentes, por exemplo:

```text
(41) 99999-0000
41 99999-0000
+55 41 99999-0000
+5541999990000
```

A arquitetura deve prever uma camada própria de normalização/comparação de números para reconhecer, com segurança, formas equivalentes quando possível.

A estratégia/biblioteca definitiva para números brasileiros e internacionais deve ser escolhida durante a implementação, sem acoplar o restante do motor ao formato visual.

## Membros do FoneGuard

Uma política poderá ser associada a:

- um contato do Android;
- um número digitado manualmente;
- um grupo próprio do FoneGuard.

Um grupo FoneGuard pode misturar contatos e números manuais.

Exemplo:

```text
Grupo: Família
- Maria (contato)
- João (contato)
- +55 41 99999-0000 (número manual)
```

Os grupos do Android/contas externas podem ser estudados futuramente para importação ou conveniência, mas o comportamento das políticas não deve depender deles.

## Modelo de política

A primeira versão deve nascer com um **motor genérico de políticas**, mesmo que a interface seja liberada incrementalmente.

Conceitualmente, uma política possui:

```text
POLÍTICA
- alvo(s): contato, número ou grupo
- ação
- condições opcionais
- estado: ativa/inativa
```

### Ações

As ações previstas são:

```text
ALLOW   -> permitir
SILENCE -> silenciar
BLOCK   -> bloquear/rejeitar
```

A implementação Android deve mapear essas ações apenas para capacidades oficialmente suportadas pela camada de chamada em uso.

### Condições

As condições previstas para o modelo inicial são:

- dia da semana;
- uma ou mais janelas de horário por dia;
- localização;
- combinação cumulativa das condições configuradas.

Exemplo:

```text
Política: Trabalho
Alvos: grupo Trabalho
Ação: ALLOW

Condições:
segunda a sexta
08:00-12:00
14:00-18:00
localização = Escritório
```

Quando várias condições existirem na mesma política, a hipótese inicial é que sejam combinadas por `E`:

```text
dia válido
E horário válido
E localização válida
```

Expressões arbitrárias com `OU`, `NÃO` ou árvores lógicas complexas ficam fora do primeiro marco.

### Como uma política participa da decisão

Uma política **só produz resultado quando suas condições são verdadeiras**.

Exemplo:

```text
João
segunda a sexta, 08:00-18:00
-> ALLOW
```

Se João ligar às 10:00, a política participa e retorna `ALLOW`.

Se ligar às 22:00, essa política simplesmente não se aplica. Outra política pode se aplicar; se nenhuma se aplicar, usa-se o comportamento padrão.

Esse modelo foi escolhido para permitir composição de políticas sem obrigar cada regra a definir simultaneamente o que acontece dentro e fora de suas condições.

## Múltiplas políticas para a mesma pessoa

Uma pessoa/número pode participar de mais de uma política ou grupo.

Exemplo permitido:

```text
Grupo Família
-> ALLOW sempre

Grupo Trabalho
-> ALLOW seg-sex 08:00-18:00
```

As duas políticas podem coexistir porque, quando ambas são aplicáveis, produzem o mesmo resultado.

O motor deve prever **detecção de conflito** quando políticas aplicáveis simultaneamente produzirem ações incompatíveis.

Exemplo:

```text
Política A -> ALLOW
Política B -> BLOCK
```

se as duas puderem valer para o mesmo alvo no mesmo contexto, existe uma ambiguidade que deve ser apresentada ao usuário em vez de ser resolvida silenciosamente por uma prioridade arbitrária.

A detecção de conflito pode ser introduzida incrementalmente, mas o modelo de dados não deve impedir múltiplas políticas desde o início.

## Comportamento padrão

Toda chamada avaliável pelo FoneGuard que não corresponder a nenhuma política aplicável deve cair em uma ação padrão configurada explicitamente pelo usuário.

Na interface, evitar termos técnicos como `fallback`. Usar linguagem semelhante a:

```text
Chamadas sem regra
- Permitir
- Silenciar
- Bloquear
```

A escolha deve ocorrer antes da primeira ativação efetiva do FoneGuard e deve permanecer visível/alterável na interface principal.

Nenhuma opção deve ser escondida como comportamento implícito que o usuário possa esquecer.

Ao criar/editar uma política, a interface deve conseguir informar que, fora das condições daquela política, outra política poderá decidir e, se nenhuma decidir, valerá o comportamento padrão atual.

## Fluxo funcional inicial

Fluxo conceitual definido antes do layout visual:

```text
instalação
  -> preparação/requisitos
  -> leitura dos contatos
  -> escolher "Chamadas sem regra"
  -> ativar FoneGuard
  -> tela principal
  -> grupos / individuais / números manuais
  -> criação e edição de políticas
  -> histórico das decisões
```

A interface final ainda deve ser desenhada depois que o fluxo e o motor estiverem suficientemente definidos.

## Localização

Localização faz parte da visão da primeira arquitetura, não apenas de uma versão distante.

O objetivo é suportar políticas como:

```text
ALLOW somente quando:
segunda a sexta
E 08:00-18:00
E local = Escritório
```

A decisão de uma chamada não deve depender de uma obtenção lenta de posição no instante da chamada.

A arquitetura deve favorecer estado local previamente atualizado, por exemplo através de geofencing/localização em segundo plano:

```text
Android detecta entrada/saída de uma área
  -> FoneGuard atualiza estado local
  -> chamada chega
  -> motor consulta o estado local imediatamente
```

Fontes oficiais:

https://developer.android.com/develop/sensors-and-location/location/geofencing
https://developer.android.com/develop/sensors-and-location/location/background

## Chamadas privadas ou sem identificação

Neste documento, **número privado/sem identificação** significa uma chamada na qual o número do chamador não é apresentado. Não confundir com um número normal que apenas não esteja salvo nos contatos.

O Android documenta apresentações como:

- `RESTRICTED`;
- `UNKNOWN`;
- `UNAVAILABLE`;
- `PAYPHONE`.

A documentação atual do `CallScreeningService` informa que chamadas nessas apresentações não são entregues ao serviço da mesma forma que chamadas numeradas normais.

Portanto, a primeira implementação **não deve prometer controle total de chamadas privadas apenas com `ROLE_CALL_SCREENING`**.

Esse comportamento deve ser testado em aparelho físico. A possibilidade de `ROLE_DIALER` permanece como investigação paralela justamente para avaliar maior cobertura desse cenário.

Fonte oficial:

https://developer.android.com/reference/android/telecom/CallScreeningService

## Histórico explicável

O FoneGuard deve registrar localmente a decisão e o motivo, tanto para uso do usuário quanto para depuração.

Exemplos:

```text
14:32  +55 41 99999-0000
BLOQUEADA
Motivo: política "Trabalho fora do horário"

15:07  Maria
PERMITIDA
Motivo: política "Família"

16:20  +55 11 3333-4444
SILENCIADA
Motivo: nenhuma política aplicável; comportamento padrão = SILENCIAR
```

O histórico não deve usar dados pessoais reais em testes versionados.

# Primeiro marco funcional

A primeira implementação deve ser pequena na quantidade de recursos expostos, mas deve construir a fundação correta para expansão.

## Fundação arquitetural

O primeiro marco deve preparar:

1. leitura de contatos via Contacts Provider;
2. camada de identidade/normalização de números;
3. modelo de contatos/números usados pelo FoneGuard;
4. grupos próprios do FoneGuard;
5. modelo genérico de políticas;
6. ações `ALLOW`, `SILENCE` e `BLOCK`;
7. condições de dia, horário e localização no modelo;
8. suporte estrutural a múltiplas políticas;
9. comportamento padrão configurável;
10. motor de avaliação independente da UI;
11. histórico local da decisão e do motivo;
12. integração inicial com `CallScreeningService`.

A UI pode expor e testar essas capacidades em incrementos menores. O objetivo é evitar uma arquitetura descartável baseada apenas em bloqueio de número exato.

## O que o primeiro marco precisa provar

Antes de ampliar recursos, o primeiro marco deve provar em aparelho físico:

```text
Android entrega a chamada
  -> FoneGuard identifica/normaliza o chamador quando possível
  -> encontra políticas candidatas
  -> avalia contexto
  -> escolhe ação
  -> aplica ação
  -> registra decisão e motivo
```

## Desenvolvimento incremental sugerido

Implementar e validar em pequenos PRs, por exemplo:

```text
1. baseline do ROLE_CALL_SCREENING + serviço permitindo tudo
2. READ_CONTACTS + leitura/seleção de contatos
3. persistência básica de membros e grupos
4. modelo de políticas + motor puro/testável
5. comportamento padrão
6. aplicação real de ALLOW/SILENCE/BLOCK
7. histórico
8. condições de dia/horário
9. localização/geofencing
10. múltiplas políticas + detecção de conflitos
```

A ordem pode ser ajustada durante a implementação se houver dependências técnicas, mas o modelo de dados deve preservar a visão completa desde o início.

# Módulo 2 — Encaminhamento de mensagens

É um módulo posterior e não deve bloquear a implementação do motor de chamadas.

Objetivo principal:

- receber SMS/MMS compatíveis;
- aplicar filtros/regras;
- encaminhar para destinos explicitamente configurados pelo usuário;
- registrar sucessos e falhas;
- manter comportamento e destinos visíveis ao usuário.

## App SMS padrão

A arquitetura futura deve investigar duas possibilidades:

1. receber SMS usando as APIs/broadcasts permitidos sem substituir o app de mensagens padrão;
2. assumir `ROLE_SMS` se isso se mostrar necessário para confiabilidade, APIs desejadas ou políticas da plataforma/Play Store.

Não assumir antecipadamente que o FoneGuard precisa ser o app SMS padrão apenas para o primeiro experimento de encaminhamento.

Fontes oficiais:

https://developer.android.com/reference/android/provider/Telephony
https://developer.android.com/reference/android/app/role/RoleManager

## RCS

RCS não deve ser tratado como equivalente a SMS/MMS.

A primeira versão do encaminhamento não deve prometer interceptação/encaminhamento de mensagens RCS recebidas por aplicativos como Google Messages.

Se o produto exigir encaminhamento confiável de mensagens recebidas pelo canal tradicional, a experiência do usuário pode precisar orientar a desativação de RCS para trabalhar apenas com SMS/MMS, até que exista uma arquitetura oficialmente suportada para o caso desejado.

Esse ponto deve ser revisitado antes da implementação do módulo de mensagens, pois APIs e políticas podem evoluir.

# Google Play e permissões sensíveis

Permissões de SMS e registro de chamadas podem ser tratadas como sensíveis/restritas pela Google Play. A publicação pública deve ser revisada contra as políticas vigentes no momento da publicação.

O fato de um APK funcionar localmente não significa automaticamente que uma versão com as mesmas permissões será aceita na Play Store.

Fonte oficial:

https://support.google.com/googleplay/android-developer/answer/10208820?hl=pt-br

# Privacidade e segurança

Como chamadas, contatos, localização e mensagens são dados sensíveis:

- configurações e estados importantes devem ser visíveis ao usuário;
- destinos de encaminhamento devem ser cadastrados conscientemente;
- evitar armazenar conteúdo de mensagens quando não for necessário;
- processamento de políticas de chamadas deve permanecer local sempre que possível;
- não incluir números reais, contatos reais, SMS reais ou localizações pessoais em fixtures, documentação ou screenshots públicos;
- documentar qualquer futura transmissão de dados para fora do aparelho antes de implementá-la.

# Próximo passo para um novo chat

O próximo chat deve:

1. ler `PROJECT_CONTEXT.md`, `docs/ANDROID_CI.md`, `docs/WORKFLOW.md` e este arquivo;
2. não reconstruir a CI já validada;
3. criar uma branch de feature seguindo `docs/WORKFLOW.md`;
4. iniciar o **primeiro incremento do primeiro marco funcional**, preferencialmente o baseline de `ROLE_CALL_SCREENING`/`CallScreeningService` permitindo chamadas por padrão;
5. preservar desde o começo separação entre camada Android, motor de políticas, persistência e UI;
6. validar cada incremento por PR + CI antes de avançar para a próxima capacidade.

A implementação deve ser incremental, mas não deve adotar uma arquitetura temporária que sabemos que será substituída quando grupos, horários, localização e múltiplas políticas forem adicionados.
