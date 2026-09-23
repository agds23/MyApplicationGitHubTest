# Development Workflow

Este documento registra a forma de trabalho adotada para o projeto.

## Fonte de verdade

O GitHub é a fonte de verdade.

O código local no Android Studio deve ser sincronizado com o repositório antes de iniciar uma mudança e depois de merges feitos no GitHub.

Comandos básicos de sincronização local:

```powershell
git pull
git status
git log --oneline -5
```

## Fluxo padrão para mudanças

Para mudanças funcionais, arquiteturais ou de infraestrutura:

```text
main atualizada
  -> criar branch específica
  -> implementar uma mudança pequena e verificável
  -> testar localmente quando aplicável
  -> commit
  -> push
  -> abrir Pull Request para main
  -> GitHub Actions valida
  -> revisar resultado
  -> merge somente com CI verde
  -> git pull local
```

O objetivo é que cada PR tenha um propósito claro e que o CI consiga dizer se aquele incremento preservou o projeto.

## Branches

Usar nomes que indiquem intenção. Exemplos:

```text
feature/call-screening-baseline
feature/block-number-rule
feature/allow-list
feature/call-history
feature/sms-forwarding-baseline
fix/call-rule-priority
docs/project-handoff
ci/update-android-toolchain
```

Não é necessário manter branches antigas depois que o PR foi mesclado e não houver mais utilidade nelas.

## Commits

Preferir commits pequenos, com mensagem objetiva no imperativo ou descrevendo claramente a alteração.

Exemplos:

```text
Add call screening service baseline
Add exact-number blocking rule
Document Android CI recipe
Fix call rule priority
```

Evitar misturar, no mesmo commit, várias mudanças sem relação entre si.

## Pull Requests

Um PR deve explicar:

- o que mudou;
- por que mudou;
- como foi validado;
- limitações conhecidas;
- próximos passos quando existirem.

Em mudanças funcionais, não transformar o PR em uma grande reescrita se o mesmo objetivo puder ser alcançado em etapas menores.

## CI como validação, não como ambiente permanente

O GitHub Actions recria o ambiente em runner efêmero. O projeto deve compilar em VM nova usando apenas o conteúdo versionado e dependências declaradas.

A execução da `main` que serve como referência inicial da receita atual é a **Android CI #14**.

Mais detalhes em `docs/ANDROID_CI.md`.

## Quando trabalhar direto na `main`

O padrão é branch + PR.

Mudanças diretas na `main` devem ser exceção, por exemplo uma correção operacional simples e deliberadamente autorizada durante um experimento controlado. Para desenvolvimento normal do FoneGuard, preferir branch + PR.

## Desenvolvimento incremental

A regra principal para o FoneGuard é implementar uma capacidade por vez.

Exemplo para bloqueio de chamadas:

```text
1. assumir o papel de call screening
2. receber evento de chamada
3. permitir tudo por padrão
4. bloquear um número exato
5. registrar a decisão
6. adicionar lista de permissão
7. adicionar regras adicionais
```

Cada etapa deve poder ser testada antes de adicionar a seguinte.

## Testes locais e em CI

Quando uma mudança puder ser validada no computador:

- compilar no Android Studio;
- executar testes unitários relevantes;
- verificar Lint quando necessário.

Quando o comportamento depender de telefonia real, SMS, permissões especiais ou papel do sistema Android, a validação final deve incluir aparelho físico ou um cenário Android compatível. O CI continua sendo responsável por garantir que o projeto compila e passa pelos testes automatizados.

## Histórico do Actions

Não é necessário apagar execuções antigas por rotina.

O histórico pode ser útil para:

- comparar uma falha nova com uma antiga;
- encontrar quando uma regressão começou;
- consultar logs de uma configuração que funcionou;
- confirmar que determinado commit passou no CI.

Execuções antigas não contaminam uma nova VM.

Artifacts e caches têm ciclo de vida próprio e não devem ser confundidos com o histórico visível das runs.

## Segurança e repositório público

O repositório está público neste estágio. Portanto:

- nunca commitar tokens;
- nunca commitar senhas;
- nunca commitar chaves de API privadas;
- nunca commitar keystores de assinatura;
- nunca commitar `local.properties`;
- nunca commitar dados pessoais reais usados em testes;
- não incluir números de telefone reais em fixtures ou screenshots públicos.

Usar dados fictícios em documentação e testes.

## Mudanças em CI

O workflow atual já foi validado. Não alterar versões, runner, SDK ou actions apenas por serem mais novas.

Quando uma atualização for necessária:

```text
branch de CI
  -> alterar uma causa conhecida
  -> PR
  -> validar VM nova
  -> merge somente após sucesso
```

Registrar em `docs/ANDROID_CI.md` qualquer nova descoberta que seja útil para não repetir erros.

## Handoff entre chats

Um novo chat não deve depender de memória informal da conversa anterior.

Antes de trabalhar, deve ler:

1. `PROJECT_CONTEXT.md`
2. `docs/ANDROID_CI.md`
3. `docs/WORKFLOW.md`
4. `docs/FONEGUARD.md`

Se houver divergência entre uma conversa antiga e o repositório atual, o repositório deve prevalecer até que a divergência seja investigada.
