# FoneGuard

**FoneGuard** é o nome provisório do aplicativo. O objetivo é evoluir o projeto de teste atual para um app Android útil, mantendo desenvolvimento incremental, comportamento explícito e arquitetura simples no começo.

## Ideia geral

A visão atual combina dois grupos de funcionalidades:

1. **controle e bloqueio de chamadas recebidas**;
2. **encaminhamento controlado de mensagens recebidas**.

Os aplicativos abaixo são referências de produto, não especificações obrigatórias e nem fontes de código a copiar:

- SMS Forwarder: https://play.google.com/store/apps/details?id=com.frzinapps.smsforward
- Calls Blacklist: https://play.google.com/store/apps/details?id=com.vladlee.easyblacklist

A intenção é observar ideias úteis, rejeitar o que não fizer sentido e criar nosso próprio comportamento e interface.

## Princípios do produto

- começar com poucas funcionalidades confiáveis;
- adicionar regras uma por vez;
- manter decisões do app explicáveis ao usuário;
- evitar backend próprio enquanto não houver necessidade real;
- preferir processamento local para regras de chamadas;
- não encaminhar mensagens para destinos que não tenham sido configurados explicitamente pelo usuário;
- não criar comportamento oculto ou silencioso que possa exfiltrar mensagens sem conhecimento do usuário;
- registrar limitações reais do Android em vez de prometer recursos que a plataforma não oferece ao app comum;
- considerar políticas da Google Play desde cedo, mas sem bloquear o desenvolvimento local do APK por causa de uma possível publicação futura.

## Estado atual

Ainda **não foi implementada funcionalidade de telefonia ou SMS**.

A infraestrutura Android/GitHub/CI já está pronta e validada. O próximo passo é definição funcional, começando pelo módulo de chamadas.

## Módulo 1 — Bloqueio e triagem de chamadas

A base técnica pretendida é a API oficial `CallScreeningService`, com o app solicitando ao usuário o papel `RoleManager.ROLE_CALL_SCREENING`.

A documentação oficial informa que o sistema vincula um único app escolhido pelo usuário para triagem de chamadas. O serviço precisa responder a uma chamada recebida em até 5 segundos, portanto as regras principais devem ser rápidas e preferencialmente locais.

Fonte oficial:

https://developer.android.com/reference/android/telecom/CallScreeningService

### Tipos de regras em discussão

Não implementar todas de uma vez. Esta lista é um mapa de possibilidades.

| Regra | Exemplo | Situação inicial |
| --- | --- | --- |
| Bloquear número exato | `+55 41 99999-0000` | prioridade para V1 |
| Sempre permitir número | contato/número que vence outras regras | prioridade para V1 |
| Bloquear números fora dos contatos | qualquer número não salvo | avaliar após baseline |
| Bloquear por prefixo | números que começam com determinado prefixo | V2 |
| Bloquear por DDD/faixa | determinado padrão regional | V2 |
| Regras por horário | bloquear regra X entre 22h e 07h | posterior |
| Regras por dia da semana | regra especial em fins de semana | posterior |
| Exceção para repetição | segunda chamada em poucos minutos pode passar | posterior |
| Verificação de número da operadora | considerar status de verificação do chamador | investigar |
| Regras por SIM | comportamento diferente por SIM | investigar compatibilidade |
| Número privado/oculto | chamador sem identificação | possui limitação específica da API |

### Prioridade de regras — hipótese inicial

Uma hierarquia simples para discutir no próximo chat:

```text
chamada chegou
  -> proteção está ativa?
       não -> permitir
       sim
  -> está em "sempre permitir"?
       sim -> permitir
       não
  -> número exato está bloqueado?
       sim -> bloquear
       não
  -> corresponde a regra de prefixo/faixa?
       sim -> bloquear
       não
  -> regra "fora dos contatos" está ativa e se aplica?
       sim -> bloquear
       não
  -> permitir
```

Essa ordem ainda **não é decisão final**. O próximo chat deve discutir conflitos, precedência, exceções e experiência do usuário antes da implementação completa.

### Histórico explicável

Uma característica desejada é registrar a decisão tomada pelo FoneGuard e o motivo.

Exemplo conceitual:

```text
14:32  +55 41 99999-0000
BLOQUEADA
Motivo: número exato na lista de bloqueio

15:07  +55 11 3333-4444
PERMITIDA
Motivo: nenhuma regra de bloqueio aplicável
```

Isso é útil para o usuário e para depuração durante o desenvolvimento.

### Limitações importantes da API de triagem

Pontos documentados pelo Android que precisam orientar o design:

- chamadas fora dos contatos são fornecidas ao `CallScreeningService`;
- chamadas de contatos também podem ser fornecidas quando o usuário conceder `READ_CONTACTS`;
- chamadas com apresentação `RESTRICTED`, `UNKNOWN`, `UNAVAILABLE` ou `PAYPHONE` não são fornecidas ao `CallScreeningService`;
- o serviço precisa responder em até 5 segundos;
- a API permite responder permitindo, silenciando ou bloqueando/rejeitando chamadas recebidas.

Portanto, **"bloquear número oculto" não deve ser tratado como se fosse automaticamente equivalente a bloquear um número normal pela mesma API**. Esse caso precisa ser investigado separadamente.

Outra API relacionada, com acesso mais restrito ao bloqueio do sistema:

https://developer.android.com/reference/android/provider/BlockedNumberContract

## Primeira entrega funcional sugerida

A primeira versão testável do módulo de chamadas deve ser pequena:

1. tela indicando se o FoneGuard possui o papel de triagem de chamadas;
2. fluxo para solicitar `ROLE_CALL_SCREENING` ao usuário;
3. serviço de triagem funcional;
4. comportamento padrão: permitir chamadas;
5. cadastro manual de um número de teste;
6. regra de bloqueio por número exato;
7. registro local da decisão e do motivo;
8. teste em aparelho físico com uma chamada real.

Somente depois dessa base funcionar devemos adicionar lista de permissão, contatos, prefixos, horários etc.

## Módulo 2 — Encaminhamento de mensagens

É um objetivo planejado, mas não é a primeira implementação.

Possibilidades futuras:

- ativar/desativar encaminhamento;
- destino configurado explicitamente pelo usuário;
- encaminhar todas ou aplicar filtro;
- filtro por remetente;
- filtro por palavra-chave;
- múltiplas regras;
- histórico de encaminhamentos e falhas;
- posteriormente avaliar destinos como outro número, e-mail ou webhook, conforme arquitetura e políticas permitirem.

O SMS Forwarder de referência demonstra que encaminhamento/sincronização de SMS pode ser o núcleo de um produto desse tipo, mas o FoneGuard deve ter seu próprio modelo de regras e consentimento.

## Política da Google Play para SMS e Call Log

Permissões de SMS e registro de chamadas são tratadas como sensíveis/restritas pela Google Play. A publicação de um app que use essas permissões pode exigir que o aplicativo seja o manipulador padrão apropriado ou se enquadre em um uso/exceção permitido e aprovado.

Isso deve ser revisto antes de preparar uma publicação pública. O fato de um APK funcionar localmente não significa automaticamente que uma versão com as mesmas permissões será aceita na Play Store.

Fonte oficial:

https://support.google.com/googleplay/android-developer/answer/10208820?hl=pt-br

## Privacidade e segurança

Como chamadas e mensagens são dados altamente sensíveis, algumas regras de projeto devem ser mantidas desde o início:

- configurações de encaminhamento devem ser visíveis ao usuário;
- destinos devem ser cadastrados conscientemente pelo usuário;
- não ocultar logs ou estado de encaminhamento com a intenção de monitorar terceiros;
- evitar armazenar conteúdo de mensagens quando não for necessário;
- usar dados fictícios nos testes versionados;
- não colocar números de telefone reais, SMS reais ou contatos reais no repositório público;
- documentar claramente qualquer transmissão de dados para fora do aparelho antes de implementá-la.

## Perguntas para o próximo chat

O próximo chat deve continuar **aqui**, antes de codificar todas as regras:

1. Quais tipos de bloqueio realmente queremos na primeira versão?
2. Qual deve ser a ordem de prioridade quando duas regras entram em conflito?
3. "Sempre permitir" deve vencer qualquer regra de bloqueio?
4. Queremos bloquear apenas números adicionados manualmente ou já incluir não-contatos na primeira versão?
5. O comportamento será somente "permitir/bloquear" ou também queremos "silenciar" como ação configurável?
6. O histórico deve registrar também chamadas permitidas ou apenas as bloqueadas?
7. Como queremos representar/normalizar números brasileiros e internacionais?
8. Quando será necessário pedir `READ_CONTACTS`?
9. Como tratar o caso de chamadas privadas/ocultas sabendo da limitação do `CallScreeningService`?
10. Quais recursos ficam explicitamente fora da V1 para evitar crescimento excessivo de escopo?

## Próximo marco

Depois que as perguntas acima estiverem resolvidas, criar uma branch de feature para o **baseline do CallScreeningService**, sem ainda implementar todas as regras avançadas.
