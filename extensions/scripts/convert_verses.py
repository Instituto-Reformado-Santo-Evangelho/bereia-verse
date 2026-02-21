import re
import os

def clean_tags(text):
    # Fix known typos
    text = text.replace('<FRr>', '<Fr>')
    
    # Titles
    # <TS> or <TS1> -> <h1>
    text = re.sub(r'<TS1?>(.*?)<Ts>', r'<h1>\1</h1>', text)
    # <TS2> -> <h2>
    text = re.sub(r'<TS2>(.*?)<Ts>', r'<h2>\1</h2>', text)
    # <TS3> -> <h3>
    text = re.sub(r'<TS3>(.*?)<Ts>', r'<h3>\1</h3>', text)
    
    # Formatting
    text = re.sub(r'<FI>(.*?)<Fi>', r'<i>\1</i>', text)
    text = re.sub(r'<FU>(.*?)<Fu>', r'<u>\1</u>', text)
    text = re.sub(r'<FR>(.*?)<Fr>', r'<span class="words-of-jesus">\1</span>', text)
    text = re.sub(r'<FO>(.*?)<Fo>', r'<span class="ot-quote">\1</span>', text)
    
    # Font tags (Legacy/Specific to this file)
    text = re.sub(r'<font size=-1>(.*?)</font>', r'<span class="small-caps">\1</span>', text)
    text = re.sub(r'<font color=red>(.*?)</font>', r'<span class="red-text">\1</span>', text)
    
    # Notes (Generic handler for RF if they exist)
    # <RF>text<Rf>
    text = re.sub(r'<RF>(.*?)<Rf>', r'<sup class="footnote" title="\1">*</sup>', text)
    # <RF q=X>text<Rf>
    text = re.sub(r'<RF q=(.*?)>(.*?)<Rf>', r'<sup class="footnote" data-symbol="\1" title="\2">\1</sup>', text)

    return text

input_file = "../data/raw/ACF2011 - Almeida Corrigida e Fiel.ont"
output_file = "../data/raw/ACF2011_converted.txt"

if not os.path.exists(input_file):
    print(f"Error: Input file '{input_file}' not found.")
    exit(1)

try:
    with open(input_file, 'r', encoding='utf-8') as f_in, \
         open(output_file, 'w', encoding='utf-8') as f_out:
        line_count = 0
        for line in f_in:
            # Strip newline, process, add newline
            processed = clean_tags(line.rstrip('\n'))
            f_out.write(processed + '\n')
            line_count += 1
            
    print(f"Conversion complete. Processed {line_count} lines. Output saved to {output_file}")

except Exception as e:
    print(f"An error occurred: {e}")
