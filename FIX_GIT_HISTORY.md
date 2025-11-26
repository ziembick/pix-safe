# Como resolver o erro de push com arquivos grandes

Os arquivos grandes da pasta `postgres-data/` já estão no histórico do Git. 
O `.gitignore` só previne novos arquivos, mas não remove os já commitados.

## Solução Rápida (aumentar buffer HTTP):

```bash
git config http.postBuffer 524288000
git config http.maxRequestBuffer 100M
git push origin feat-teste
```

## Solução Definitiva (limpar histórico):

Se a solução rápida não funcionar, você precisa remover os arquivos do histórico:

```bash
# Opção 1: Usar git filter-branch
git filter-branch --force --index-filter \
  "git rm -rf --cached --ignore-unmatch postgres-data" \
  --prune-empty --tag-name-filter cat -- --all

# Depois limpar referências
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# E fazer push forçado (CUIDADO!)
git push origin --force --all
```

**ATENÇÃO:** O push forçado reescreve o histórico. Certifique-se de que ninguém mais está trabalhando na branch antes de fazer isso.

