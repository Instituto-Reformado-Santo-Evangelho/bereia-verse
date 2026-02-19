<script setup>
useHead({
  title: 'Política de Privacidade - Bereia Versículos'
})
</script>

<template>
    <main class="doc-page">
        <!-- Hero / Header -->
        <div class="hero-section">
            <div class="container">
                <nav class="breadcrumb">
                    <NuxtLink to="/" class="nav-link">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
                        Voltar para Início
                    </NuxtLink>
                </nav>
                <h1 class="page-title">Política de Privacidade</h1>
                <p class="last-update">Última atualização: 1 de fevereiro de 2026</p>
            </div>
        </div>

        <div class="container content-wrapper">
            <article class="privacy-content">
                <div class="intro-block">
                    <p>O <strong>Bereia Versículos</strong>, desenvolvido pelo <strong>Instituto Reformado Santo Evangelho (IRSE)</strong>, valoriza a sua privacidade. Esta política descreve como lidamos com as informações ao utilizar nosso aplicativo em todas as plataformas (Windows, Linux e MacOs).</p>
                </div>

                <section>
                    <h2>1 – Coleta de dados pessoais</h2>
                    <p>O Bereia Versículos <strong>não coleta, não armazena e não transmite</strong> nenhum dado pessoal identificável para servidores próprios. Não solicitamos criação de conta, nome, e–mail ou qualquer outra informação pessoal para o funcionamento básico do aplicativo.</p>
                </section>

                <section>
                    <h2>2 – Integração com o Google Drive</h2>
                    <p>O aplicativo oferece uma funcionalidade opcional de sincronização de notas via Google Drive.</p>
                    <ul>
                        <li><strong>Finalidade:</strong> Armazenar e sincronizar as suas anotações entre diferentes dispositivos de sua propriedade.</li>
                        <li><strong>Escopo de acesso:</strong> O Bereia Versículos solicita acesso <em>apenas</em> ao seu próprio diretório de dados no Google Drive (App Data Folder) ou aos arquivos especificamente criados por ele. Não temos acesso aos seus outros arquivos pessoais, fotos ou documentos no seu Google Drive.</li>
                        <li><strong>Autenticação:</strong> O processo de login é realizado diretamente através dos serviços de autenticação do Google. O aplicativo nunca tem acesso à sua senha.</li>
                    </ul>
                </section>

                <section>
                    <h2>3 – Monitoramento da área de transferência (Clipboard)</h2>
                    <p>O Bereia Versículos monitora a área de transferência do seu sistema operacional para detectar referências bíblicas (exemplo, <em>João 3:16</em>).</p>
                    <ul>
                        <li><strong>Processamento local:</strong> Todo o processamento de texto capturado é feito de forma estritamente local no seu dispositivo.</li>
                        <li><strong>Privacidade:</strong> O conteúdo da sua área de transferência nunca é enviado para a internet ou para os nossos servidores. O app apenas “lê” o texto para verificar se ele corresponde a um padrão bíblico e, em seguida, descarta qualquer informação que não seja uma referência bíblica.</li>
                    </ul>
                </section>

                <section>
                    <h2>4 – Armazenamento local</h2>
                    <p>As configurações do aplicativo, histórico de visualização e notas (caso você não utilize a sincronização em nuvem) são armazenadas localmente no seu dispositivo. Você pode excluir esses dados a qualquer momento limpando o cache do aplicativo ou removendo as pastas de dados do sistema.</p>
                </section>

                <section>
                    <h2>5 – Compartilhamento com terceiros</h2>
                    <p>Nós não vendemos, não trocamos e não compartilhamos nenhuma informação com terceiros. A única interação externa ocorre entre o seu dispositivo e os servidores do Google, caso você opte por ativar a sincronização.</p>
                </section>

                <section>
                    <h2>6 – Alterações nesta Política</h2>
                    <p>Reservamo–nos o direito de atualizar esta política de privacidade periodicamente. Recomendamos que você revise esta página ocasionalmente para se manter informado sobre como estamos protegendo suas informações.</p>
                </section>

                <section>
                    <h2>7 – Contato</h2>
                    <p>Se você tiver alguma dúvida sobre esta política ou sobre o funcionamento do aplicativo, entre em contato conosco através do nosso site oficial: <a href="https://tech.santoevangelho.com.br" class="link-gold">tech.santoevangelho.com.br</a>.</p>
                </section>
            </article>
        </div>
    </main>
</template>

<style scoped>
.doc-page {
    background-color: var(--bg-color);
    padding: 0 !important;
    margin: 0 !important;
    max-width: 100% !important;
    width: 100%;
}

.hero-section {
    width: 100%;
    background: linear-gradient(to bottom, var(--surface-dark), var(--bg-color));
    padding: 6rem 0 4rem;
    margin-top: 0;
    position: relative;
    text-align: center;
}

.hero-section::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100px;
    background: linear-gradient(to top, var(--bg-color), transparent);
}

.container {
    max-width: 800px;
    margin: 0 auto;
    padding: 0 2rem;
    position: relative;
    z-index: 2;
}

.breadcrumb { 
    margin-bottom: 3rem;
    display: flex;
    justify-content: center;
}

.nav-link {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    color: var(--gold-primary);
    font-weight: 500;
    padding: 8px 16px;
    border-radius: 50px;
    background: rgba(255, 174, 0, 0.1);
    border: 1px solid rgba(255, 174, 0, 0.2);
    font-size: 0.85rem;
    text-transform: uppercase;
    letter-spacing: 1px;
    transition: all 0.3s;
}

.nav-link:hover {
    background: var(--gold-primary);
    color: #000;
}

.page-title {
    font-size: 3rem;
    color: #fff;
    margin-bottom: 1rem;
    text-shadow: 0 4px 20px rgba(0,0,0,0.8);
}

.last-update {
    color: var(--text-secondary);
    font-style: italic;
    opacity: 0.8;
}

.content-wrapper {
    margin-top: -3rem; /* Overlap hero */
    padding-bottom: 8rem;
    position: relative;
    z-index: 3;
}

.privacy-content {
    background: transparent;
    padding: 0;
    border: none;
    box-shadow: none;
}

.intro-block {
    margin-bottom: 4rem;
    font-size: 1.2rem;
    line-height: 1.8;
    color: var(--text-primary);
    border-left: 3px solid var(--gold-primary);
    padding-left: 2rem;
}

h2 {
    color: var(--gold-primary);
    font-size: 1.8rem;
    margin: 3rem 0 1.5rem;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid rgba(255,255,255,0.05);
}

p {
    color: var(--text-secondary);
    line-height: 1.8;
    margin-bottom: 1.5rem;
    font-size: 1.05rem;
}

ul {
    list-style: none;
    padding: 0;
    margin-bottom: 2rem;
}

li {
    margin-bottom: 1rem;
    padding-left: 1.5rem;
    position: relative;
    color: var(--text-secondary);
    line-height: 1.7;
}

li::before {
    content: '•';
    color: var(--gold-primary);
    position: absolute;
    left: 0;
    font-weight: bold;
}

strong, em {
    color: var(--text-primary);
    font-style: normal;
}

.link-gold {
    color: var(--gold-primary);
    text-decoration: underline;
    text-underline-offset: 4px;
}

.link-gold:hover {
    color: #fff;
}

.footer-note {
    margin-top: 6rem;
    padding-top: 3rem;
    border-top: 1px solid rgba(255,255,255,0.05);
    text-align: center;
}

.org-name {
    font-family: var(--font-heading);
    font-size: 1.4rem;
    color: #fff;
    margin-bottom: 0.5rem;
}

@media (max-width: 600px) {
    .hero-section {
        padding: 3rem 1rem 5rem;
    }
    
    .page-title {
        font-size: 2rem;
    }
    
    .privacy-content {
        padding: 2rem;
    }
    
    .content-wrapper {
        margin-top: 0;
        padding: 0 1rem 4rem;
    }
}
</style>
