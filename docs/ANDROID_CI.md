# Android CI

Este documento registra a receita de CI Android que foi investigada, corrigida e validada neste repositório. O objetivo é evitar repetir a mesma tentativa e erro em projetos futuros.

## Princípio

O runner hospedado pelo GitHub é efêmero. Não existe uma VM permanente esperando a próxima compilação.

Em cada execução, o GitHub cria uma VM nova, executa a receita versionada em `.github/workflows/android-ci.yml`, produz logs/artifacts e depois descarta a VM.

O que persiste é a **receita** no repositório, além de mecanismos próprios do GitHub como cache e artifacts. A correção do build não deve depender de um cache existente.

Fluxo atual:

```text
Ubuntu 24.04 novo
  -> checkout
  -> JDK Temurin 25
  -> Android SDK tools
  -> Android CLI
  -> Android 17 / API 37
  -> Build Tools 37.0.0
  -> Gradle cache
  -> gradlew executável
  -> unit tests
  -> lint
  -> assembleDebug
  -> app-debug artifact
  -> VM descartada
```

## Triggers

O workflow atual roda em:

- `push` para `main`;
- `pull_request` direcionado para `main`;
- execução manual com `workflow_dispatch`.

Existe também `concurrency` com cancelamento de execução anterior do mesmo grupo para evitar trabalho redundante.

## Toolchain fixada

A receita validada usa:

- runner: `ubuntu-24.04`;
- checkout: `actions/checkout` v7.0.1, fixado por SHA;
- Java: Temurin `25.0.4+1`;
- setup-java: v6.0.1, fixado por SHA;
- setup-android: v4.0.4, fixado por SHA;
- Android SDK Command-line Tools: 22.0 (`15859902`);
- Android CLI esperada: `1.0.16406183`;
- plataforma: `platforms/android-37.0@2`;
- Build Tools: `build-tools/37.0.0`;
- Gradle Wrapper do projeto: `9.6.0`;
- Gradle Actions: v6.3.0, fixado por SHA;
- upload-artifact: v7.0.1, fixado por SHA.

As versões de AGP/Kotlin/Compose pertencem ao próprio projeto e ficam no Version Catalog.

## Por que a Android CLI é validada

O instalador oficial da Android CLI é obtido de uma URL `latest`. Para evitar uma mudança silenciosa de ambiente, o workflow instala pelo endpoint oficial e em seguida verifica se `android --version` corresponde a `ANDROID_CLI_VERSION`.

Se o Google trocar a versão distribuída, a execução falhará de forma explícita. Nesse caso, a atualização deve ser deliberada: testar a nova versão em branch, atualizar a expectativa e validar novamente.

## Cache

`gradle/actions/setup-gradle` usa o provider `basic`.

O cache existe para acelerar execuções. Ele não deve ser necessário para a correção do build. Uma VM com cache vazio precisa conseguir reconstruir o projeto.

## Gradle Wrapper no Linux

O projeto nasceu em Windows e o arquivo `gradlew` não ficou com bit executável no histórico Git. Em Linux isso provocou:

```text
./gradlew: Permission denied
```

A receita atual resolve isso explicitamente antes do build:

```bash
chmod +x gradlew
```

Em outro projeto também é possível versionar o bit executável no Git. O importante é não assumir que um `gradlew` criado/commitado no Windows estará executável no runner Linux.

## Etapas de validação

A receita executa, nessa ordem:

```bash
./gradlew test --stacktrace
./gradlew lint --stacktrace
./gradlew assembleDebug --stacktrace
```

Depois publica `app/build/outputs/apk/debug/*.apk` como artifact `app-debug`.

O artifact do APK tem retenção de **7 dias**. Essa retenção não significa que a linha da execução na aba Actions desaparecerá em 7 dias; são coisas distintas.

## Problemas encontrados e soluções

### 1. `sdkmanager` tentando instalar o pacote legado `tools`

Uma versão anterior do setup Android falhou porque tentou resolver o pacote histórico `tools`, que não estava disponível naquele ambiente.

**Lição:** não depender de comportamento legado implícito do setup; controlar explicitamente as ferramentas necessárias.

### 2. `platforms;android-37` não encontrado

O `sdkmanager` disponível no runner não encontrava `platforms;android-37`, inclusive após tentar canal preview/canary.

A nova Android CLI listou os pacotes reais da plataforma e mostrou que a identificação atual é:

```text
platforms/android-37.0
```

Depois, para maior reprodutibilidade, a receita foi fixada em:

```text
platforms/android-37.0@2
```

**Lição:** para este stack de Android 17/API 37, usar a identificação que a Android CLI atual realmente expõe, em vez de inferir o nome do pacote a partir apenas do `compileSdk`.

### 3. `gradlew: Permission denied`

Depois que a instalação do SDK passou, o runner finalmente chegou ao Gradle e revelou o problema do bit executável do wrapper.

**Solução atual:** `chmod +x gradlew` dentro do workflow.

### 4. Endurecimento da receita

Depois de obter uma execução verde, a receita foi validada novamente em um PR usando:

- runner explícito em vez de `ubuntu-latest`;
- actions fixadas por SHA;
- Java fixado em versão específica;
- revisão explícita da plataforma Android;
- validação da Android CLI;
- cache básico do Gradle.

Isso reduz mudanças involuntárias no ambiente entre execuções.

## Execuções de referência

A validação do PR endurecido passou em **Android CI #13**.

Depois do merge, a receita foi reconstruída na `main` e passou em **Android CI #14**:

https://github.com/agds23/MyApplicationGitHubTest/actions/runs/35924117212

A execução #14 é o marco de referência da receita atual na `main`.

## Como reutilizar em outro projeto

A receita pode ser copiada/adaptada, mas não deve ser tratada como texto universal imutável. Antes de reutilizar, comparar:

- `compileSdk` / `targetSdk` do novo projeto;
- versão do AGP;
- versão do Kotlin;
- Gradle Wrapper;
- JDK exigido pelo projeto;
- plataforma e Build Tools necessários.

Se o stack for equivalente, esta configuração é um ótimo ponto de partida. Se o stack mudar, atualizar deliberadamente em branch e validar por PR.

## Repositórios públicos e privados

Este repositório está público e é usado como laboratório. Em projetos privados, o mesmo workflow funciona, mas convém controlar os triggers para evitar execuções desnecessárias e consumo de recursos/minutos do plano.

Nunca mover segredos para o YAML. Quando um projeto realmente precisar deles, usar GitHub Secrets/Environments e conceder apenas as permissões necessárias.

## Fontes e referências

- Workflow deste projeto: `.github/workflows/android-ci.yml`
- GitHub Actions: https://docs.github.com/actions
- GitHub-hosted runners: https://docs.github.com/actions/using-github-hosted-runners/about-github-hosted-runners
- Artifacts: https://docs.github.com/actions/using-workflows/storing-workflow-data-as-artifacts
- Gradle Actions: https://github.com/gradle/actions
- Android setup action: https://github.com/android-actions/setup-android
- Android CLI: https://developer.android.com/tools/agents/android-cli
- Android SDK command-line tools: https://developer.android.com/tools
