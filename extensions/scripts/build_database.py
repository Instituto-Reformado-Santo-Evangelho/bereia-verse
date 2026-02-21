import sqlite3
import os

def build_db():
    input_file = "../data/raw/ACF2011_converted.txt"
    db_file = "../data/db/bible.sqlite"
    
    if not os.path.exists(input_file):
        print(f"Erro: Arquivo {input_file} não encontrado.")
        return

    # Remover banco antigo se existir para garantir limpeza
    if os.path.exists(db_file):
        os.remove(db_file)
        
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    # Criar tabela simples
    # ID manual não é estritamente necessário se confiarmos no ROWID, 
    # mas ser explícito é melhor para portabilidade.
    cursor.execute('''
        CREATE TABLE verses (
            id INTEGER PRIMARY KEY,
            content TEXT NOT NULL
        )
    ''')
    
    print("Inserindo dados...")
    
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            # Usar transação para velocidade máxima
            batch = []
            line_id = 1
            
            for line in f:
                content = line.strip()
                batch.append((line_id, content))
                
                # Commit a cada 5000 registros para não consumir muita memória
                if len(batch) >= 5000:
                    cursor.executemany('INSERT INTO verses (id, content) VALUES (?, ?)', batch)
                    batch = []
                    print(f"Processados {line_id} versículos...")
                
                line_id += 1
            
            # Inserir o restante
            if batch:
                cursor.executemany('INSERT INTO verses (id, content) VALUES (?, ?)', batch)
        
        conn.commit()
        print(f"Sucesso! Banco de dados '{db_file}' criado com {line_id - 1} registros.")
        
        # Criar índice se necessário (Primary Key já é indexada no SQLite)
        
    except Exception as e:
        print(f"Erro ao inserir dados: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    build_db()
