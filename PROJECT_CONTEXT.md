# Project Context

Este arquivo é o ponto de entrada para qualquer nova conversa ou pessoa que vá trabalhar neste repositório.

## Fonte de verdade

O GitHub é a fonte de verdade do projeto. Antes de propor mudanças, consulte o código e estes documentos no próprio repositório:

- `PROJECT_CONTEXT.md`
- `docs/ANDROID_CI.md`
- `docs/WORKFLOW.md`
- `docs/FONEGUARD.md`

Não é necessário exportar esses arquivos e reenviá-los manualmente para outro chat quando houver acesso ao repositório pelo conector do GitHub.

## Estado atual

Repositório: `agds23/MyApplicationGitHubTest`

Situação atual:

- aplicativo Android nativo em Kotlin;
- UI com Jetpack Compose e Material 3;
- Gradle Kotlin DSL + Version Catalog;
- `compileSdk = 37`;
- `targetSdk = 37`;
- `minSdk = 26`;
- Android Gradle Plugin `9.4.1`;
- Kotlin `2.2.10`;
- Compose BOM `2026.02.01`;
- Gradle Wrapper `9.6.0` com checksum de distribuição;
- projeto ainda é funcionalmente simples e exibe a tela inicial de teste;
- CI Android validado no GitHub Actions;
- o nome provisório do produto em discussão é **FoneGuard**.

O repositório é público neste momento e está sendo usado como laboratório. Portanto, não adicionar segredos, tokens, senhas, chaves privadas, credenciais, dados pessoais sensíveis ou arquivos de configuração local.

## Ambiente de desenvolvimento

O desenvolvimento local de referência usa Android Studio no Windows 11, com JDK 25 fornecido pelo próprio Android Studio. O projeto também é compilado em CI em uma VM Linux limpa.

A compilação não deve depender de estado exclusivo de uma máquina local. O objetivo é que qualquer mudança válida possa ser reconstruída pelo workflow versionado no repositório.

Quando for necessário executar Gradle pelo PowerShell e o Java global não estiver no `PATH`, pode-se apontar `JAVA_HOME` apenas na sessão atual para o JBR do Android Studio e executar o Wrapper do projeto.

## CI validado

O workflow oficial está em `.github/workflows/android-ci.yml`.

A execução **Android CI #14**, na `main`, após o merge do PR que endureceu a receita, foi a primeira referência final da configuração atual funcionando de ponta a ponta:

https://github.com/agds23/MyApplicationGitHubTest/actions/runs/35924117212

Ela validou em uma VM nova:

1. checkout do repositório;
2. JDK 25;
3. Android SDK tools;
4. Android CLI;
5. plataforma Android 17 / API 37;
6. Build Tools 37.0.0;
7. Gradle Wrapper e cache;
8. testes unitários;
9. Android Lint;
10. `assembleDebug`;
11. geração e upload do APK como artifact.

Os detalhes, versões fixadas e problemas já resolvidos estão em `docs/ANDROID_CI.md`.

## Forma de trabalho

Para mudanças funcionais ou estruturais, o padrão é:

`main` -> branch específica -> implementação -> PR -> CI verde -> merge -> sincronização local.

Evitar alterar a infraestrutura já validada sem uma necessidade concreta. Mudanças no CI devem ser testadas em branch/PR antes de substituir a receita funcional da `main`.

Mais detalhes em `docs/WORKFLOW.md`.

## Direção do produto

O próximo estágio não é mais infraestrutura. Estamos iniciando a definição funcional de um aplicativo provisoriamente chamado **FoneGuard**, com dois eixos possíveis:

- gerenciamento/bloqueio de chamadas recebidas;
- encaminhamento controlado de mensagens recebidas.

O ponto exato onde a discussão funcional deve continuar está registrado em `docs/FONEGUARD.md`.

## Como iniciar um novo chat

Em um novo chat dentro deste projeto, use um texto semelhante a este:

> Estamos desenvolvendo o repositório Android `agds23/MyApplicationGitHubTest`, provisoriamente chamado FoneGuard. O GitHub é a fonte de verdade. Antes de propor mudanças, leia pelo conector GitHub `PROJECT_CONTEXT.md`, `docs/ANDROID_CI.md`, `docs/WORKFLOW.md` e `docs/FONEGUARD.md`. A infraestrutura GitHub Actions já está funcional e validada; não quero reconstruí-la sem necessidade. Continue a partir do ponto registrado no `docs/FONEGUARD.md`. Neste momento quero definir, com calma, os tipos de regras de bloqueio de chamadas, suas prioridades e comportamento antes de implementar tudo.

Depois que o novo chat tiver lido esses arquivos, a conversa pode seguir diretamente para produto, arquitetura e implementação sem repetir a investigação de CI feita anteriormente.
