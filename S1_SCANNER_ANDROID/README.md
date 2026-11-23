# S1 SCANNER

Aplicativo Android de auditoria local, com ações iniciadas pelo usuário.

## O que faz
- Exibe informações básicas do dispositivo.
- Lista aplicativos visíveis ao Package Manager e procura nomes que correspondam a indicadores configurados.
- Permite escolher uma pasta pelo seletor oficial do Android e analisar nomes de arquivos/pastas.
- Exporta relatório `.txt`.

## O que não faz
- Não acessa dados privados de outros aplicativos.
- Não lê mensagens, senhas ou histórico privado do navegador.
- Não contorna permissões do Android.
- Não recupera arquivos apagados.
- Um nome correspondente não comprova instalação ativa, uso ou infração.

## Gerar o APK pelo GitHub
1. Crie um repositório no GitHub.
2. Extraia o ZIP deste projeto.
3. Envie o conteúdo da pasta `S1_SCANNER_ANDROID` para a raiz do repositório (incluindo a pasta oculta `.github`).
4. Abra **Actions** e selecione **Build S1 SCANNER APK**.
5. Clique em **Run workflow** ou aguarde o build após enviar os arquivos.
6. Quando terminar com sucesso, abra a execução e baixe o artefato `S1-SCANNER-debug-apk`.

O APK fica dentro do artefato do GitHub Actions, não diretamente na lista de arquivos do repositório.

## Observação sobre permissões
A busca de aplicativos usa `QUERY_ALL_PACKAGES`, adequada para builds privados/sideload com autorização. Publicar na Google Play exige cumprir a política de visibilidade de pacotes do Google Play.
