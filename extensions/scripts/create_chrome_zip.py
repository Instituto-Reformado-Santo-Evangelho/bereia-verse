import zipfile
import os
import json
import time
import shutil
import re

def update_manifest_version(manifest_path, new_version):
    with open(manifest_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    data['version'] = new_version
    # Ensure version is valid for Chrome (1-4 integers separated by dots)
    # Our test version is likely X.Y.Z.TIMESTAMP.
    # Chrome store only allows 4 parts max, each up to 65535. 
    # Timestamp is too large for the last part.
    # We will use a simplified test version for Chrome: X.Y.Z.BuildNum (modulo 65535) or just ignore strict validation if it's for local loading (unpacked/zip).
    # Chrome strictly validates version even for local loading? Yes, it needs to be valid format.
    # Let's try to fit it. Timestamp is ~1.7e9. 
    # We can use X.Y.Z + 1 (minor bump) or similar.
    # Or just use the timestamp as the name of the zip, but keep the internal version compliant or slightly modified.
    # Let's try to map timestamp to something smaller or just use a counter if we had state.
    # Since we don't have state, let's just use the seconds within the day/month? 
    # Better: just leave the version as is for Chrome validation, OR append a small number.
    # The user wants "zerar versões testes".
    # Let's create a version like X.Y.9999 to indicate test?
    # Or X.Y.Z.1234 (last 4 digits of timestamp).
    
    # Valid regex: ^(0|[1-9][0-9]{0,4})(\.(0|[1-9][0-9]{0,4})){0,3}$
    # Max value is 65535.
    
    parts = new_version.split('.')
    if len(parts) > 4:
        # Truncate to 4 parts
        parts = parts[:4]
    
    # Ensure last part is < 65535
    if len(parts) == 4 and int(parts[3]) > 65535:
        parts[3] = str(int(parts[3]) % 65535)
        
    data['version'] = ".".join(parts)

    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

def create_zip_from_dir(source_dir, output_zip_file):
    output_dir = os.path.dirname(output_zip_file)
    os.makedirs(output_dir, exist_ok=True)
    
    with zipfile.ZipFile(output_zip_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(source_dir):
            for file in files:
                file_path = os.path.join(root, file)
                zipf.write(file_path, os.path.relpath(file_path, source_dir))

if __name__ == "__main__":
    script_dir = os.path.dirname(__file__)
    project_root = os.path.abspath(os.path.join(script_dir, '..'))
    dist_dir = os.path.join(project_root, 'dist')
    os.makedirs(dist_dir, exist_ok=True)
    
    source_dir_name = 'ext-bereia-verse'
    source_path = os.path.join(project_root, source_dir_name)

    # 1. Standard Production Zip
    with open(os.path.join(source_path, 'manifest.json'), 'r') as f:
        manifest = json.load(f)
        current_ver = manifest.get('version', '1.0.0')

    prod_zip_path = os.path.join(dist_dir, f'ext-bereia-verse-v{current_ver}.zip')
    create_zip_from_dir(source_path, prod_zip_path)
    print(f"✅ Production Zip Created: {prod_zip_path}")

    # 2. Test Zip
    with open(os.path.join(source_path, 'manifest.json'), 'r') as f:
        manifest = json.load(f)
        current_ver = manifest.get('version', '1.0.0')

    timestamp = int(time.time())
    # Chrome version constraint: max 65535 per component.
    # We use (timestamp % 10000) as the build number to stay safe.
    build_num = timestamp % 10000
    test_version = f"{current_ver}.{build_num}"
    
    # Temp dir
    temp_build_dir = os.path.join(dist_dir, 'temp_chrome_build')
    if os.path.exists(temp_build_dir):
        shutil.rmtree(temp_build_dir)
    shutil.copytree(source_path, temp_build_dir)
    
    # Update Manifest
    update_manifest_version(os.path.join(temp_build_dir, 'manifest.json'), test_version)
    
    # Zip Test
    test_zip_name = f"ext-bereia-verse-test-{timestamp}.zip"
    test_zip_path = os.path.join(dist_dir, test_zip_name)
    create_zip_from_dir(temp_build_dir, test_zip_path)
    
    print(f"🧪 Test Zip Created: {test_zip_path} (Version: {test_version})")
    
    # Cleanup
    shutil.rmtree(temp_build_dir)

    # --- 3. Deployment to Web ---
    repo_root = os.path.abspath(os.path.join(project_root, '..'))
    web_downloads_dir = os.path.join(repo_root, 'web', 'downloads')
    web_content_path = os.path.join(repo_root, 'web', 'content', 'apps', 'extensao-bereia-versiculos.md')

    # Ensure web/downloads exists
    os.makedirs(web_downloads_dir, exist_ok=True)

    # Copy the PRODUCTION Zip
    prod_zip_filename = "ext-bereia-verse.zip"
    shutil.copy2(prod_zip_path, os.path.join(web_downloads_dir, prod_zip_filename))
    print(f"🚀 Deployed Production Zip to web/downloads/{prod_zip_filename}")

    # Update Markdown Content
    if os.path.exists(web_content_path):
        with open(web_content_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Update latestVersion
        content = re.sub(r"latestVersion: '[^']*'", f"latestVersion: '{current_ver}'", content)
        # Update downloadUrl if needed
        content = re.sub(r"downloadUrl: '[^']*'", f"downloadUrl: '/downloads/{prod_zip_filename}'", content)
        
        with open(web_content_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"🌐 Updated {web_content_path} with Chrome version {current_ver}.")
    