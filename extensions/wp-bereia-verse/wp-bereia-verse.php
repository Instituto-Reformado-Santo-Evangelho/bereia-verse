<?php
/**
 * Plugin Name: Bereia Versículos | IRSE
 * Description: Exibe versículos da Bíblia em um popup ao selecionar referências bíblicas no texto. Usa Cloudflare D1.
 * Version: 1.3.0
 * Plugin URI: https://tech.santoevangelho.com.br
 * Author: Organização IRSE
 * License: GPL2
 */

if (!defined('ABSPATH')) {
    exit;
}

// --- 1. CONFIGURAÇÕES DO ADMIN ---

function acf_bible_register_settings() {
    register_setting('acf_bible_options_group', 'acf_bible_marker_style');
    register_setting('acf_bible_options_group', 'acf_bible_hover_mode');
}
add_action('admin_init', 'acf_bible_register_settings');

function acf_bible_register_options_page() {
    add_options_page(
        'Configurações Bereia Versículos', 
        'Bereia Versículos', 
        'manage_options', 
        'acf-bible-settings', 
        'acf_bible_render_options_page'
    );
}
add_action('admin_menu', 'acf_bible_register_options_page');

function acf_bible_render_options_page() {
    ?>
    <div class="wrap">
        <h1>Configurações do Bereia Versículos | IRSE</h1>
        <form method="post" action="options.php">
            <?php settings_fields('acf_bible_options_group'); ?>
            <table class="form-table">
                <tr valign="top">
                    <th scope="row">Estilo de Marcação</th>
                    <td>
                        <?php $style = get_option('acf_bible_marker_style', 'underline'); ?>
                        <select name="acf_bible_marker_style">
                            <option value="none" <?php selected($style, 'none'); ?>>Apenas Seleção Manual (Sem ícones)</option>
                            <option value="arrow" <?php selected($style, 'arrow'); ?>>Ícone de Seta (➤)</option>
                            <option value="underline" <?php selected($style, 'underline'); ?>>Sublinhado Pontilhado</option>
                            <option value="underline_solid" <?php selected($style, 'underline_solid'); ?>>Sublinhado Sólido</option>
                        </select>
                        <p class="description">Escolha como as referências bíblicas serão destacadas no texto.</p>
                    </td>
                </tr>
                <tr valign="top">
                    <th scope="row">Ativar Popup ao Passar o Mouse</th>
                    <td>
                        <?php $hover = get_option('acf_bible_hover_mode', '1'); ?>
                        <label>
                            <input type="hidden" name="acf_bible_hover_mode" value="0">
                            <input type="checkbox" name="acf_bible_hover_mode" value="1" <?php checked($hover, '1'); ?>>
                            Exibir versículo automaticamente ao passar o mouse sobre referências marcadas
                        </label>
                        <p class="description">Se desativado, será necessário clicar na referência para exibir o versículo.</p>
                    </td>
                </tr>
            </table>
            <?php submit_button(); ?>
        </form>
    </div>
    <?php
}

// --- 2. CARREGAMENTO DOS SCRIPTS ---

function acf_bible_enqueue_scripts() {
    // Restringir ao Blog (Posts, Arquivos, Home do Blog)
    if ( ! ( is_home() || is_singular('post') || is_archive() ) ) {
        return;
    }

    // Carrega a opção do banco de dados (Padrão: arrow)
    // Se existir a option antiga 'acf_bible_show_arrows' = 0, devemos respeitar (migração simples em tempo de exec)
    // Mas para simplificar, vamos assumir o novo padrão.
    $marker_style = get_option('acf_bible_marker_style', 'underline');
    $hover_mode = get_option('acf_bible_hover_mode', '1');

    // CSS
    wp_enqueue_style(
        'acf-bible-style', 
        plugin_dir_url(__FILE__) . 'assets/css/style.css', 
        [], 
        '1.3.0'
    );

    // JS
    wp_enqueue_script(
        'acf-bible-script', 
        plugin_dir_url(__FILE__) . 'assets/js/script.js', 
        [], 
        '1.3.0', 
        true 
    );

    // Passar variáveis para o JS
    wp_localize_script('acf-bible-script', 'acfSettings', [
        'mappingUrl' => plugin_dir_url(__FILE__) . 'assets/bible_mapping.json',
        'logoUrl'    => plugin_dir_url(__FILE__) . 'assets/img/logo.png',
        'arrowUrl'   => plugin_dir_url(__FILE__) . 'assets/img/arrow.svg',
        'markerStyle'=> $marker_style, // 'none', 'arrow', 'underline'
        'hoverMode'  => $hover_mode    // '1' or '0'
    ]);
}

add_action('wp_enqueue_scripts', 'acf_bible_enqueue_scripts');
