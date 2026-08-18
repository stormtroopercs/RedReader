#!/usr/bin/env python3
"""Convert SAM lambda syntax to anonymous objects in Kotlin files."""

import re
import sys
from pathlib import Path

# Define the interface -> method mappings
INTERFACE_METHODS = {
    'OnSearchListener': 'onSearch',
    'Consumer': 'consume',
    'ActivityResultCallback': 'onActivityResult',
    'ByteArrayCallback': 'onByteArray',
    'GenericFactory': 'create',
    'FunctionOneArgWithReturn': 'call',
    'FunctionOneArgNoReturn': 'call',
    'Operator': 'call',
    'DownloadImageToSaveSuccessCallback': 'onDownloadImageToSaveSuccess',
    'OnSharedPreferenceChangeListener': 'onSharedPreferenceChanged',
    'ConfigProvider': 'getConfig',
    'RedditAccountChangeListener': 'onRedditAccountChange',
    'ToggledFullscreenCallback': 'onToggledFullscreen',
}

def fix_sam_lambda(content, file_path):
    """Convert SAM lambda syntax to anonymous objects."""
    
    # Common patterns to fix:
    patterns = [
        ('OnSearchListener', 'onSearch'),
        ('Consumer', 'consume'),
        ('ActivityResultCallback', 'onActivityResult'),
        ('GenericFactory', 'create'),
        ('FunctionOneArgWithReturn', 'call'),
        ('FunctionOneArgNoReturn', 'call'),
        ('Operator', 'call'),
        ('DownloadImageToSaveSuccessCallback', 'onDownloadImageToSaveSuccess'),
        ('OnSharedPreferenceChangeListener', 'onSharedPreferenceChanged'),
        ('ConfigProvider', 'getConfig'),
        ('ByteArrayCallback', 'onByteArray'),
        ('RedditAccountChangeListener', 'onRedditAccountChange'),
        ('ToggledFullscreenCallback', 'onToggledFullscreen'),
    ]
    
    modified = content
    
    for interface_name, method_name in patterns:
        # Pattern: InterfaceName<...> { params -> body }
        # Handle both with and without type parameters
        pattern = rf'{interface_name}\s*(?:<[^>]+>)?\s*\{{'
        
        while True:
            match = re.search(pattern, modified)
            if not match:
                break
            
            start = match.start()
            interface_full = match.group(0)
            
            # Find the matching closing brace
            brace_count = 1
            pos = match.end()
            while pos < len(modified) and brace_count > 0:
                if modified[pos] == '{':
                    brace_count += 1
                elif modified[pos] == '}':
                    brace_count -= 1
                pos += 1
            
            if brace_count != 0:
                break  # Malformed, skip
            
            end = pos
            lambda_body = modified[match.end():end-1]
            
            # Generate anonymous object
            anon_obj = f'object : {interface_name}() {{\n            override fun {method_name}({lambda_body.strip()})\n        }}'
            
            modified = modified[:start] + anon_obj + modified[end:]
    
    return modified

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: fix_sam.py <file.kt>")
        sys.exit(1)
    
    file_path = sys.argv[1]
    with open(file_path, 'r') as f:
        content = f.read()
    
    new_content = fix_sam_lambda(content, file_path)
    
    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        print(f"Fixed: {file_path}")
    else:
        print(f"No changes: {file_path}")
