import sqlite3
import os

def dump_sqlite_to_sql(db_file, output_sql_file):
    if not os.path.exists(db_file):
        print(f"Erro: Banco de dados '{db_file}' não encontrado.")
        return

    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()

    try:
        with open(output_sql_file, 'w', encoding='utf-8') as f_out:
            # Dump the schema
            schema_dump = "".join(conn.iterdump())
            f_out.write(schema_dump)
            
            # For specific table, we can generate INSERTs more cleanly
            # Or use sqlite3 .dump command which includes schema and data
            
            # The .dump command usually generates CREATE TABLE and INSERTs
            # For simplicity, we can just use the built-in iterdump
            
        print(f"Dump SQL gerado com sucesso em '{output_sql_file}'.")

    except Exception as e:
        print(f"Erro ao gerar o dump SQL: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    db_file = "../data/db/bible.sqlite"
    output_sql_file = "../data/sql/bible_dump.sql"
    dump_sqlite_to_sql(db_file, output_sql_file)
