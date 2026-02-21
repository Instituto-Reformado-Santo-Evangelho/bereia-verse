import shutil
import os
import re
import time

def get_current_version(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    match = re.search(r'Version:\s*([0-9a-zA-Z\.\-]+)', content)
    if match:
        return match.group(1)
    return '0.0.0'

def update_version_in_file(file_path, new_version):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Update Header Version
    content = re.sub(r'(Version:\s*)([0-9a-zA-Z\.\-]+)', r'\g<1>' + new_version, content)
    
    # Update Enqueue Versions (avoiding cache)
    # Looking for '1.2.2' or similar inside wp_enqueue_style/script
    # We replace the version string in the enqueue calls. 
    # Assumes the version is passed as a string literal matching the header version or similar.
    # To be safe, we just replace the exact previous version string if possible, or use a broader regex for enqueue.
    # The previous code used explicit version strings. 
    # Let's replace any 'x.x.x' or 'x.x.x-test' inside the enqueue functions if it looks like a version.
    # However, simply replacing the found current_version is safer.
    
    # We need to know the old version to replace it safely in the code body
    old_version_match = re.search(r'Version:\s*([0-9a-zA-Z\.\-]+)', content) # Find it again just to be sure we match the header
    if old_version_match:
        old_ver = old_version_match.group(1) # This is the NEW one now because we replaced it in line 18? No, line 18 returned a string, didn't write to file yet.
        # Wait, line 18 did NOT write to file. It operated on 'content' string.
        # But wait, line 18 used regex on 'content' which still had the OLD version in the capture group?
        # No, line 18 replaces it. 
        
        # Let's restart the logic for clarity.
        pass

    # Re-read to be clean
    with open(file_path, 'r', encoding='utf-8') as f:
        original_content = f.read()
        
    old_version = get_current_version(file_path)
    
    new_content = original_content.replace(f"Version: {old_version}", f"Version: {new_version}")
    new_content = new_content.replace(f"'{old_version}'", f"'{new_version}'")
    new_content = new_content.replace(f'"{old_version}"', f'"{new_version}"')
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

# Setup Paths
script_dir = os.path.dirname(__file__)
project_root = os.path.abspath(os.path.join(script_dir, '..'))
source_dir_name = 'wp-bereia-verse'
source_path = os.path.join(project_root, source_dir_name)
dist_dir = os.path.join(project_root, 'dist')
os.makedirs(dist_dir, exist_ok=True)

# 1. Create Standard Production Zip
current_ver = get_current_version(os.path.join(source_path, 'wp-bereia-verse.php'))
output_prod_base = os.path.join(dist_dir, f'wp-bereia-verse-v{current_ver}')
shutil.make_archive(output_prod_base, 'zip', root_dir=project_root, base_dir=source_dir_name)
print(f"✅ Production Zip Created: {output_prod_base}.zip")

# 2. Create Test Zip with Random Version
current_ver = get_current_version(os.path.join(source_path, 'wp-bereia-verse.php'))
timestamp = int(time.time())
test_version = f"{current_ver}.{timestamp}"

# Create Temp Directory for Test Build
temp_build_dir = os.path.join(dist_dir, 'temp_wp_build')
temp_plugin_dir = os.path.join(temp_build_dir, source_dir_name)

if os.path.exists(temp_build_dir):
    shutil.rmtree(temp_build_dir)
os.makedirs(temp_plugin_dir)

# Copy files
# We can't use shutil.copytree directly because destination exists? No, we created parent.
# Actually, copytree expects dest to NOT exist.
shutil.rmtree(temp_plugin_dir) # Remove the one we just made with makedirs
shutil.copytree(source_path, temp_plugin_dir)

# Update Version
plugin_file = os.path.join(temp_plugin_dir, 'wp-bereia-verse.php')
update_version_in_file(plugin_file, test_version)

# Zip Test Build
test_zip_name = f"wp-bereia-verse-test-{timestamp}"
output_test_base = os.path.join(dist_dir, test_zip_name)
shutil.make_archive(output_test_base, 'zip', root_dir=temp_build_dir, base_dir=source_dir_name)
print(f"🧪 Test Zip Created: {output_test_base}.zip (Version: {test_version})")

# Cleanup
shutil.rmtree(temp_build_dir)

# --- 3. Deployment to Web ---
repo_root = os.path.abspath(os.path.join(project_root, '..'))
web_downloads_dir = os.path.join(repo_root, 'web', 'downloads')
web_content_path = os.path.join(repo_root, 'web', 'content', 'apps', 'plugin-wordpress-bereia-versiculos.md')

# Ensure web/downloads exists
os.makedirs(web_downloads_dir, exist_ok=True)

# Copy the PRODUCTION Zip
prod_zip_filename = "wp-bereia-verse.zip"
shutil.copy2(output_prod_base + '.zip', os.path.join(web_downloads_dir, prod_zip_filename))
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
    print(f"🌐 Updated {web_content_path} with WP version {current_ver}.")
    