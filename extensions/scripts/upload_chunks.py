import os
import subprocess

def split_and_upload(sql_file, chunk_size=500):
    if not os.path.exists(sql_file):
        print(f"Erro: Arquivo {sql_file} não encontrado.")
        return

    print(f"Lendo {sql_file}...")
    with open(sql_file, 'r', encoding='utf-8') as f:
        content = f.read()

    commands = []
    current_command = ""
    
    content = content.replace(';INSERT INTO', ';\nINSERT INTO')
    
    lines = content.split('\n')
    
    inserts = []
    schema = []

    for line in lines:
        line = line.strip()
        if not line: continue
        
        if line.startswith('BEGIN TRANSACTION') or line.startswith('COMMIT'):
            continue
            
        if line.startswith('CREATE TABLE') or line.startswith('INSERT INTO "verses" VALUES(1,'):
            if 'CREATE TABLE' in line:
                parts = line.split(';INSERT')
                schema.append(parts[0] + ';')
                if len(parts) > 1:
                    inserts.append('INSERT' + parts[1])
            else:
                inserts.append(line)
        elif line.startswith('INSERT INTO'):
            inserts.append(line)
        else:
            if 'CREATE TABLE' in schema[-1] if schema else False:
                schema[-1] += " " + line

    print(f"Total de comandos INSERT identificados: {len(inserts)}")

    if not inserts:
        print("Erro: Nenhum insert encontrado. Verifique o formato do dump.")
        return

    os.makedirs('../data/sql/sql_chunks', exist_ok=True)

    # Chunk 0: Schema
    chunk0_path = '../data/sql/sql_chunks/chunk_schema.sql'
    with open(chunk0_path, 'w', encoding='utf-8') as f:
        f.write("DROP TABLE IF EXISTS verses;\n")
        f.write("\n".join(schema))
    
    print("Enviando schema...")
    upload_chunk(chunk0_path)

    # Chunks de dados
    total_chunks = (len(inserts) // chunk_size) + 1
    
    for i in range(total_chunks):
        start = i * chunk_size
        end = start + chunk_size
        chunk_cmds = inserts[start:end]
        
        if not chunk_cmds: continue

        chunk_path = f'../data/sql/sql_chunks/chunk_{i+1:03d}.sql'
        with open(chunk_path, 'w', encoding='utf-8') as f:
            f.write("\n".join(chunk_cmds))
        
        print(f"Enviando chunk {i+1}/{total_chunks} ({len(chunk_cmds)} inserts)...")
        if not upload_chunk(chunk_path):
            print("Falha crítica no upload. Parando.")
            # break # Comentado para tentar continuar em caso de erro esporádico

def upload_chunk(filepath):
    
    cmd = [
        "npx", "wrangler", "d1", "execute", "bible-db", 
        "--remote", f"--file={filepath}", "-y"
    ]
    
    try:
        # Timeout aumentado para evitar falhas em chunks grandes
        result = subprocess.run(
            cmd, 
            cwd="../acf-extension", 
            capture_output=True, 
            text=True,
            timeout=60 
        )
        
        if result.returncode == 0:
            print("  Sucesso!")
            return True
        else:
            print(f"  Erro no wrangler: {result.stderr}")
            # Se for erro de 'table already exists', ignorar no schema
            if "already exists" in result.stderr:
                return True
            return False
            
    except subprocess.TimeoutExpired:
        print("  Timeout! O chunk demorou muito.")
        return False
    except Exception as e:
        print(f"  Erro ao executar comando: {e}")
        return False

if __name__ == "__main__":
    split_and_upload("../data/sql/bible_dump.sql", chunk_size=400) # 400 inserts por vez é seguro
