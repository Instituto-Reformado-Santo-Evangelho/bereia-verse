import os
import subprocess
import re

def upload_existing_chunks(start_from_chunk=1):
    chunks_dir = '../data/sql/sql_chunks'
    
    # Listar arquivos que correspondem ao padrão chunk_XXX.sql
    files = [f for f in os.listdir(chunks_dir) if re.match(r'chunk_\d+\.sql', f)]
    files.sort() # Garantir ordem numérica
    
    total_files = len(files)
    print(f"Encontrados {total_files} chunks para processar.")

    for filename in files:
        # Extrair número do chunk
        try:
            chunk_num = int(filename.split('_')[1].split('.')[0])
        except ValueError:
            continue

        if chunk_num < start_from_chunk:
            print(f"Pulando {filename} (já processado)...")
            continue
            
        filepath = os.path.join(chunks_dir, filename)
        print(f"Enviando {filename}...")
        
        if not upload_chunk(filepath):
            print(f"Falha no {filename}. Tentando novamente uma vez...")
            if not upload_chunk(filepath):
                print("Falha persistente. Parando.")
                break

def upload_chunk(filepath):
    cmd = [
        "npx", "wrangler", "d1", "execute", "bible-db", 
        "--remote", f"--file={filepath}", "-y"
    ]
    
    try:
        result = subprocess.run(
            cmd, 
            cwd="../acf-extension", 
            capture_output=True, 
            text=True,
            timeout=60 # Timeout generoso
        )
        
        if result.returncode == 0:
            print("  Sucesso!")
            return True
        else:
            print(f"  Erro no wrangler: {result.stderr}")
            return False
            
    except subprocess.TimeoutExpired:
        print("  Timeout! O chunk demorou muito.")
        return False
    except Exception as e:
        print(f"  Erro ao executar comando: {e}")
        return False

if __name__ == "__main__":
    # Começa do chunk 2, pois o 1 já foi enviado manualmente
    upload_existing_chunks(start_from_chunk=2)
