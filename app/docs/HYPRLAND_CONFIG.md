# Configuração para Hyprland (Linux)

O Bereia Verse utiliza uma janela flutuante transparente que pode ser forçada a "tilar" (tiling) por gerenciadores de janela como o Hyprland, ignorando a lógica de posicionamento do aplicativo.

Para garantir que o popup flutue corretamente, adicione o bloco abaixo ao seu `hyprland.conf`. Esta configuração usa a sintaxe v2 de regras nomeadas, compatível com as versões mais recentes do Hyprland.

```ini
windowrule {
    name = bereia_popup
    match:class = ^(br-com-irse-verse-MainKt)$

    # Propriedades da Janela
    float = 1
    border_size = 0
    
    # Efeitos Visuais (Transparência Limpa)
    no_shadow = 1
    no_blur = 1
    
    # Comportamento
    stay_focused = 1
    pin = 1
}

# Fallback para versão nativa (se compilado como pacote .deb/.rpm)
windowrule {
    name = bereia_native
    match:class = ^(bereiaverse)$
    
    float = 1
    border_size = 0
    no_shadow = 1
    no_blur = 1
}
```

## Solução de Problemas

Se as regras não forem aplicadas:

1. Verifique se a classe da janela corresponde. Abra o app e rode:
   ```bash
   hyprctl clients
   ```
2. Procure o bloco da janela do Bereia Verse e confirme o valor de `class`.
3. Atualize o campo `match:class` na regra acima se for diferente.
