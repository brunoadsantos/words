[![tests and build](https://github.com/brunoadsantos/words/actions/workflows/push-workflow.yml/badge.svg)](https://github.com/brunoadsantos/words/actions)
[![Netlify Status](https://api.netlify.com/api/v1/badges/b5f15f6c-654f-4cda-ad6a-60c65663976c/deploy-status)](https://app.netlify.com/sites/bento-capitu/deploys)

## Rodar testes

```
npm run test
```

## Criar release

```
npm run release
```

## Live dev watch

```
npm run watch
```

## Live

https://bento-capitu.netlify.app/

## Gerar lista de palavras em Português

```
sudo apt install aspell-pt-br

aspell -l pt_BR dump master | aspell -l pt_BR expand > /tmp/aspell_PT_BR.txt
```

## Obra de Machado de Assis em domínio público

https://machado.mec.gov.br/obra-completa-lista/itemlist/category/23-romance
