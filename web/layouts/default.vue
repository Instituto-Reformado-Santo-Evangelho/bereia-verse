<template>
  <div>
    <header>
        <div class="logo-container">
            <img src="/assets/logo.png" alt="Logo IRSE" class="logo">
        </div>
        <h1>Instituto Reformado Santo Evangelho</h1>
        <p class="subtitle">Tecnologia a serviço do Reino</p>
    </header>

    <slot />

    <footer>
        <div class="bible-quote">
            <div class="quote-text">
                "Porque dele, e por ele, e para ele são todas as coisas; glória, pois, a ele eternamente. Amém."
                <span class="quote-ref">Romanos 11:36</span>
            </div>
        </div>
        <p>&copy; 2025 Instituto Reformado Santo Evangelho. Todos os direitos reservados.</p>
        <p class="footer-links">
            <a href="/bereia-versiculos/politica-de-privacidade">Política de Privacidade</a>
        </p>
        <a style="text-decoration: none;" href="https://santoevangelho.com.br"><img src="/assets/home.svg" alt="Ir para a página inicial"></a>
    </footer>
  </div>
</template>
