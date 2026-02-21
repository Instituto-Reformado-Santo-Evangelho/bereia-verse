import json
import os

def parse_mapping_file(filepath, current_global_offset):
    book_data = {}
    # Ordem dos livros é crucial. Vamos ler linha a linha.
    # Assumindo que o arquivo está ordenado corretamente.
    
    last_book = None
    
    with open(filepath, 'r', encoding='utf-8') as f:
        # Pular cabeçalho se houver (Livro Capitulo Versiculos)
        first_line = True
        
        for line in f:
            line = line.strip()
            if not line: continue
            
            parts = line.split('\t')
            if len(parts) < 3:
                # Tentar split por espaço se tab falhar, mas o arquivo parecia usar tabs
                # O read_file mostrou tabs. Vamos garantir.
                parts = line.split()
                if len(parts) < 3: 
                    continue

            # Detectar cabeçalho
            if first_line and (parts[0].lower() == 'livro' or parts[1].lower() == 'capítulo'):
                first_line = False
                continue
            first_line = False

            # Normalização
            # Alguns nomes podem ter espaços (1 Reis). 
            # O formato é: Nome do Livro [TAB/Space] Cap [TAB/Space] Versos
            # Se splitou por espaço e o nome tem espaço, precisamos recombinar.
            
            # Estratégia mais segura: pegar os dois ultimos como cap e verso. O resto é nome.
            verses_count = int(parts[-1])
            chapter_num = int(parts[-2])
            book_name = " ".join(parts[:-2])
            
            if book_name not in book_data:
                book_data[book_name] = {
                    "start": current_global_offset, # O ID do primeiro versículo deste livro
                    "chapters": []
                }
            
            # Adicionar contagem de versos.
            # Assume-se que os capítulos vem em ordem (1, 2, 3...)
            book_data[book_name]["chapters"].append(verses_count)
            
            # Atualizar offset global (soma todos os versos deste capítulo)
            current_global_offset += verses_count

    return book_data, current_global_offset

def main():
    files = ["../data/mappings/MAP-CAP-VERS-AT.md", "../data/mappings/MAP-CAP-VERs-NT.md"]
    full_mapping = {}
    global_offset = 1 # Começa no ID 1
    
    # Processar AT
    if os.path.exists(files[0]):
        print(f"Processando {files[0]}...")
        at_data, global_offset = parse_mapping_file(files[0], global_offset)
        full_mapping.update(at_data)
    else:
        print(f"Erro: {files[0]} não encontrado.")

    # Processar NT
    if os.path.exists(files[1]):
        print(f"Processando {files[1]}...")
        nt_data, global_offset = parse_mapping_file(files[1], global_offset)
        full_mapping.update(nt_data)
    else:
        print(f"Erro: {files[1]} não encontrado.")

    # Total esperado: 31102 + 1 (pois começou em 1 e terminou após o último)
    # O último offset será (31102 + 1). O total de versos é global_offset - 1.
    total_verses = global_offset - 1
    print(f"Total de versículos mapeados: {total_verses}")
    
    output_filename = "../data/mappings/bible_mapping.json"
    with open(output_filename, 'w', encoding='utf-8') as f:
        json.dump(full_mapping, f, ensure_ascii=False, indent=2)
    
    print(f"Mapeamento salvo em {output_filename}")

if __name__ == "__main__":
    main()
